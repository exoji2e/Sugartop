package io.exoji2e.erbil

import android.arch.persistence.room.ColumnInfo
import android.arch.persistence.room.Entity
import android.arch.persistence.room.PrimaryKey
import com.jjoe64.graphview.series.DataPoint
import java.util.*

data class SensorData(val value : Int, val statusCode : Byte, val b3 : Byte, val b4 : Byte, val b5 : Byte) {
    constructor(b : ByteArray) : this(RawParser.bin2int(b[1], b[0]), b[2], b[3], b[4], b[5])
    fun tommol() : Double = RawParser.sensor2mmol(value)
}

@Entity(tableName = "glucoseData")
data class Reading(@PrimaryKey(autoGenerate = true) val utcTimeStamp: Long,
                   @ColumnInfo(name = "readingValue") val readingValue: Int,
                   @ColumnInfo(name = "statusCode") val statusCode: Byte,
                   @ColumnInfo(name = "b3") val b3: Byte,
                   @ColumnInfo(name = "b4") val b4: Byte,
                   @ColumnInfo(name = "b5") val b5: Byte) {

    constructor(time: Long, data:SensorData) :
            this(time, data.value, data.statusCode, data.b3, data.b4, data.b5)
    fun tommol() : Double = RawParser.sensor2mmol(readingValue)
    fun eq(s : SensorData) : Boolean {
        return s.equals(SensorData(readingValue, statusCode, b3, b4, b5))
    }
    fun toDataPoint() : DataPoint = DataPoint(Date(utcTimeStamp), tommol())
}