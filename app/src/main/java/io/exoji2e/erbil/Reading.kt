package io.exoji2e.erbil

import java.util.*

data class SensorData(val value : Int, val statusCode : Byte, val b3 : Byte, val b4 : Byte, val b5 : Byte) {
    constructor(b : ByteArray) : this(RawParser.bin2int(b[1], b[0]), b[2], b[3], b[4], b[5])
}

data class Reading(val date: Date, val data: SensorData) {
    fun tommol() : Double = value() * 0.0062492 - 1.89978
    fun value() : Int = data.value
    fun eq(s : SensorData) : Boolean {
        return s.equals(data)
    }
}