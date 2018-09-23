package io.exoji2e.erbil

import android.content.Context

class SensorData {
    val map = HashMap<Long, Pair<Double,Double>>()
    val default = Pair(-0.04605, 0.00567)
    fun get(sensorId : Long) : Pair<Double, Double> {
        if(sensorId in map) return map[sensorId]!!
        return default
    }
    fun sensor2mmol(value: Int, sensorId: Long): Double = sensor2mmol(value, get(sensorId))
    fun sensor2mmol(value: Int, p : Pair<Double, Double>) : Double = value*p.second + p.first

    fun recalibrate(sensorId: Long, context : Context) : Pair<Double, Double> {
        val db = ErbilDataBase.getInstance(context)
        val srs = db.sensorContactDao().getAll().filter{s -> s.sensorId == sensorId}.sortedBy { s -> s.utcTimeStamp }
        if(srs.isEmpty()) {
            return default
        }
        val xs = mutableListOf<Int>()
        val ys = mutableListOf<Double>()
        val start = srs.first().utcTimeStamp - Time.HOUR*8
        val end = srs.last().utcTimeStamp
        val entries = DataContainer.getInstance(context).get(start, end).filter { g -> g.sensorId == sensorId }
        val manualData = db.manualEntryDao().getAll().filter{m -> m.utcTimeStamp >= start && m.utcTimeStamp <= end}
        for (m in manualData) {
            val ts = m.utcTimeStamp + Time.MINUTE*5
            val e = entries.minBy{e -> Math.max(e.utcTimeStamp - ts, ts - e.utcTimeStamp)}
            if(e == null || Math.max(e.utcTimeStamp - ts, ts - e.utcTimeStamp) > Time.MINUTE*5) continue
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
        val b1 = SS_xy/SS_xx
        val b0 = m_y - b1*m_x
        return Pair(b0, b1)

    }
    fun save(sensorId : Long, p : Pair<Double, Double>) {
        map.put(sensorId, p)
    }

    companion object {
        var sData : SensorData? = null
        fun instance() : SensorData {
            synchronized(SensorData::class.java){
                if(sData == null){
                    sData = SensorData()
                    return sData!!
                } else {
                    return sData!!
                }
            }

        }
    }
}