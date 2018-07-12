package io.exoji2e.erbil

import android.provider.ContactsContract
import java.util.*

class DataContainer {
    private val history = mutableListOf<Reading>()
    //var trend: List<Reading>? = null
    val MINUTE = 60*1000L
    val noH = 32
    fun append(raw_data: ByteArray, readingTime: Long) : Boolean {
        val timestamp = RawParser.timestamp(raw_data)
        // Timestamp is 2 mod 15 every time a new reading to history is done.
        val minutesSinceLast = (timestamp + 12)%15
        val start = readingTime - MINUTE*(15*(noH - 1) + minutesSinceLast)
        val sensor_history = RawParser.history(raw_data)
        val lastStored = last()
        if(lastStored != null) {
            var match = -1
            for (i in 0..sensor_history.size - 1) {
                if (lastStored.eq(sensor_history[i])) {
                    match = i
                }
            }
            if (match > -1) {
                val v = sensor_history.slice(IntRange(match + 1, sensor_history.size - 1))
                        .mapIndexed({ i: Int, sensorData: SensorData -> Reading(
                                lastStored.utcTimeStamp + (i + 1) * 15 * MINUTE, sensorData) })
                if (v.isNotEmpty())
                    return push(v)
                else
                    return true
            }
        }
        val v = sensor_history.mapIndexed(
                {i: Int, sensorData: SensorData -> Reading(start + i*15*MINUTE, sensorData)})
        return push(v)
    }
    fun getAll() : List<Reading> {
        synchronized(DataContainer::class) {
            return history.toList()
        }
    }
    fun get8h() : List<Reading> {
        synchronized(DataContainer::class) {
            val sz = history.size
            return history.slice(IntRange(sz - noH, sz - 1)).toList()
        }
    }
    //TODO:Save to DB as well
    fun last() : Reading? {
        synchronized(DataContainer::class) {
            if(history.isNotEmpty()) return history.last()
            else return null
        }
    }
    fun size() : Int {
        synchronized(DataContainer::class) {
            return history.size
        }
    }
    fun push(new_history : List<Reading>) : Boolean {
        synchronized(DataContainer::class) {
            return history.addAll(new_history)
        }
    }
    companion object {
        //TODO: Load from DB
        private var INSTANCE : DataContainer = DataContainer()
        fun getInstance() : DataContainer = INSTANCE
    }

}