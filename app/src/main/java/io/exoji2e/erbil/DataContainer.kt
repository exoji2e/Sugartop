package io.exoji2e.erbil

import java.util.*

class DataContainer() {
    val history = mutableListOf<Reading>()
    var trend: List<Reading>? = null
    val MINUTE = 60*1000L
    val noH = 32L
    fun append(raw_data: ByteArray, time: Long) : Boolean {
        val start = time - MINUTE*15*noH
        val sensor_history = RawParser.history(raw_data)
        if(history.size == 0) {
            val v = sensor_history.mapIndexed({i: Int, sensorData: SensorData -> Reading(start + i*15*MINUTE, sensorData)})
            return push(v)
        }
        val last = history[history.size - 1]
        var match = -1
        for (i in 0..sensor_history.size -1) {
            if(last.eq(sensor_history[i])){
                match = i
                break
            }
        }
        if(match == -1) {
            val v = sensor_history.mapIndexed({i: Int, sensorData: SensorData -> Reading(start + i*15*MINUTE, sensorData)})
            return push(v)
        } else {
            val v = sensor_history.slice(IntRange(match + 1, sensor_history.size)).
                    mapIndexed({i: Int, sensorData: SensorData -> Reading(last.utcTimeStamp + (i+1)*15*MINUTE, sensorData)})
            return push(v)
        }
    }
    // Save to DB first
    fun push(new_history : List<Reading>) : Boolean {
        return history.addAll(new_history)
    }
}