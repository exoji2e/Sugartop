package io.exoji2e.erbil

import android.arch.persistence.room.ColumnInfo
import android.arch.persistence.room.Entity
import android.arch.persistence.room.PrimaryKey
import android.content.Context
import io.exoji2e.erbil.database.ErbilDataBase
import io.exoji2e.erbil.database.GlucoseEntry
import io.exoji2e.erbil.database.ManualGlucoseEntry
import io.exoji2e.erbil.database.SensorCalibrationDao

class SensorData {
    val map = HashMap<Long, Pair<Double,Double>>()
    var sensorCalibrationDao: SensorCalibrationDao
    constructor(context: Context) {
        sensorCalibrationDao = ErbilDataBase.getInstance(context).sensorCalibrationDao()
        for(s in sensorCalibrationDao.getAll()) {
            map[s.sensorId] = Pair(s.first, s.second)
        }
    }
    fun get(sensorId : Long) : Pair<Double, Double> {
        if(sensorId in map) return map[sensorId]!!
        return default
    }
    fun sensor2mmol(value: Int, sensorId: Long): Double = sensor2mmol(value, get(sensorId))
    fun sensor2mmol(value: Int, p : Pair<Double, Double>) : Double = value*p.second + p.first

    fun start_end_sensor(sensorId: Long, context : Context) : Pair<Long, Long> {
        val db = ErbilDataBase.getInstance(context)
        val srs = db.sensorContactDao().getAll().filter{s -> s.sensorId == sensorId}.sortedBy { s -> s.utcTimeStamp }
        if(srs.isEmpty()) {
            return Pair(-1, -1)
        }
        return Pair(srs.first().utcTimeStamp - Time.HOUR*8, srs.last().utcTimeStamp)
    }

    fun manual_entries(sensorId: Long, context : Context) : List<ManualGlucoseEntry> {
        val (start, end) = start_end_sensor(sensorId, context)
        val db = ErbilDataBase.getInstance(context)
        return db.manualEntryDao().getAll().filter{m -> m.utcTimeStamp >= start && m.utcTimeStamp <= end}
    }
    fun get_calibration_pts(sensorId: Long, context: Context): List<Pair<ManualGlucoseEntry, GlucoseEntry>> {
        return get_calibration_pts(sensorId, context, Time.MINUTE * 5);
    }
    fun get_calibration_pts(sensorId: Long, context: Context, dt : Long): List<Pair<ManualGlucoseEntry, GlucoseEntry>> {
        val (start, end) = start_end_sensor(sensorId, context)
        val db = ErbilDataBase.getInstance(context)
        val manualData =  db.manualEntryDao().getAll().filter{m -> m.utcTimeStamp >= start && m.utcTimeStamp <= end}
        val entries = DataContainer.getInstance(context).get(start, end).filter { g -> g.sensorId == sensorId && g.status == 200}
        val out = mutableListOf<Pair<ManualGlucoseEntry, GlucoseEntry>>()
        for (m in manualData) {
            val ts = m.utcTimeStamp + dt
            val e = entries.minBy{e -> Math.abs(e.utcTimeStamp - ts)}
            if(e == null || Math.abs(e.utcTimeStamp - ts) > Time.MINUTE*5) continue
            out.add(Pair(m, e))
        }
        return out
    }

    fun recalibrate(sensorId: Long, context : Context) : Pair<Double, Double> {
        val calib = get_calibration_pts(sensorId, context)
        return recalibrate(calib)
    }


    fun save(sensorId : Long, p : Pair<Double, Double>) {
        map.put(sensorId, p)
        sensorCalibrationDao.insert(SensorCalibration(sensorId, p.first, p.second))
    }
    companion object {
        var sData : SensorData? = null
        val default = Pair(-0.04605, 0.00567)
        fun sensor2mmol(value: Int, p : Pair<Double, Double>) : Double = value*p.second + p.first
        fun sensor2mmol(value: Int) : Double = sensor2mmol(value, default)
        fun recalibrate(calib : List<Pair<ManualGlucoseEntry, GlucoseEntry>>) : Pair<Double, Double> {
            val xs = mutableListOf<Int>()
            val ys = mutableListOf<Double>()
            for ((m, e) in calib) {
                xs.add(e.value)
                ys.add(m.value)
            }
            if(xs.size < 3) return default
            val n = xs.size
            val m_x = xs.sum().toDouble()/n
            val m_y = ys.sum()/n
            var SS_xy = -n*m_y*m_x
            var SS_xx = -n*m_x*m_x
            for (i in 0 until n){
                SS_xy += xs[i]*ys[i]
                SS_xx += xs[i]*xs[i]
            }
            if(Math.abs(SS_xx) == 0.0) return default
            val b1 = SS_xy/SS_xx
            val b0 = m_y - b1*m_x
            return Pair(b0, b1)
        }

        fun instance(context : Context) : SensorData {
            synchronized(SensorData::class.java){
                if(sData == null){
                    sData = SensorData(context)
                    return sData!!
                } else {
                    return sData!!
                }
            }
        }
    }
}
@Entity(tableName = "SensorCalibrations")
data class SensorCalibration(@PrimaryKey(autoGenerate = false) val sensorId: Long,
                             @ColumnInfo(name = "first") val first : Double,
                             @ColumnInfo(name = "second") val second : Double)