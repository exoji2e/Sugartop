package io.exoji2e.erbil

import android.arch.persistence.room.ColumnInfo
import android.arch.persistence.room.Entity
import android.arch.persistence.room.PrimaryKey

data class SensorChunk(val value: Int, val status: Int, val history: Boolean, val rest: Int) {
    constructor(b : ByteArray, history : Boolean) :
            this(RawParser.bin2int(b[1], b[0]),
                    RawParser.byte2uns(b[2]),
                    history,
                    RawParser.bin2int(b[3], b[4], b[5]))
}
data class GlucoseReading(val value: Int, val utcTimeStamp: Long, val sensorId: Long, val status: Int, val history: Boolean, val rest: Int) {
    constructor(s: SensorChunk, utcTimeStamp: Long, sensorId: Long):
            this(s.value, utcTimeStamp, sensorId, s.status, s.history, s.rest)
}

@Entity(tableName = "GlucoseEntries")
data class GlucoseEntry(@PrimaryKey(autoGenerate = true) val id: Int,
                        @ColumnInfo(name = "value") val value: Int,
                        @ColumnInfo(name = "utcTimeStamp")val utcTimeStamp: Long,
                        @ColumnInfo(name = "sensorId") val sensorId: Long,
                        @ColumnInfo(name = "status") val status: Int,
                        @ColumnInfo(name = "history") val history: Boolean,
                        @ColumnInfo(name = "rest") val rest: Int) {
    constructor(s: GlucoseReading, id: Int):
            this(id, s.value, s.utcTimeStamp, s.sensorId, s.status, s.history, s.rest)
    fun tommol() : Double = RawParser.sensor2mmol(value)
    fun eq(s: SensorChunk) =
            value == s.value &&
            s.status == s.status &&
            history == s.history &&
            rest == s.rest
    override fun toString() =
            String.format("%d,%d,%d,%d,%d,%d,%d", id, value, utcTimeStamp, sensorId, status,
                    if(history) 1 else 0,
                    rest)
    companion object {
        fun headerString() = "id,value,utcTimeStamp,sensorId,status,history,rest"
        fun fromString(s : String) : GlucoseEntry? {
            try{
                val v = s.split(",").map{e -> e.toLong()}
                return GlucoseEntry(v[0].toInt(), v[1].toInt(), v[2], v[3], v[4].toInt(), v[5]==1L, v[6].toInt())
            } catch(e:Exception){}
            return null
        }
    }

}

@Entity(tableName = "ManualGlucoseEntries")
data class ManualGlucoseEntry(@PrimaryKey(autoGenerate = false) val utcTimeStamp: Long,
                       @ColumnInfo(name = "value") val value : Double)

@Entity(tableName = "SensorContacts")
data class SensorContact(@PrimaryKey(autoGenerate = true) val id: Int,
                         @ColumnInfo(name = "sensorId") val sensorId: Long,
                         @ColumnInfo(name = "utcTimeStamp") val utcTimeStamp: Long,
                         @ColumnInfo(name = "sensorTimeStamp") val sensorTimeStamp: Int,
                         @ColumnInfo(name = "noValuesRead") val noValuesRead: Int)