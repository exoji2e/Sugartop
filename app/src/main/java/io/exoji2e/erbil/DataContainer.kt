package io.exoji2e.erbil

import android.content.Context
import android.provider.ContactsContract
import android.util.Log
import android.widget.Toast
import java.util.*

class DataContainer {
    private val history = mutableListOf<Reading>()
    val MINUTE = 60*1000L
    val noH = 32
    val TAG = "DataContainer"
    private val mWorker : DbWorkerThread
    private var mDb : GlucoseDataBase? = null
    private var done : Boolean = false
    private val lock = java.lang.Object()
    private var raw_data = ByteArray(360)
    constructor(context : Context) {
        mWorker = DbWorkerThread("dbWorker")
        mWorker.start()
        val task = Runnable {
            mDb = GlucoseDataBase.getInstance(context)
            val glucoseData =
                    mDb?.glucoseDataDao()?.getAll()
            synchronized(lock){
                if (glucoseData == null || glucoseData.isEmpty()) {
                    Log.d(TAG, "No data in db")
                } else {
                    history.addAll(glucoseData)
                    Log.d(TAG, "db contained %d items".format(history.size))

                }
                done = true
                lock.notifyAll()
            }
        }
        mWorker.postTask(task)

    }
    private fun waitForDone() {
        synchronized(lock) {
            while(!done) {
                lock.wait()
            }
        }
    }
    fun append(raw_data: ByteArray, readingTime: Long) : Boolean {
        synchronized(lock) {
            this.raw_data = raw_data
        }
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
        waitForDone()
        synchronized(lock) {
            return history.toList()
        }
    }
    fun get8h() : List<Reading> {
        waitForDone()
        synchronized(lock) {
            val sz = history.size
            return history.slice(IntRange(sz - noH, sz - 1)).toList()
        }
    }
    fun last() : Reading? {
        waitForDone()
        synchronized(lock) {
            if(history.isNotEmpty()) return history.last()
            else return null
        }
    }
    fun size() : Int {
        waitForDone()
        synchronized(lock) {
            return history.size
        }
    }
    fun push(new_history : List<Reading>) : Boolean {
        waitForDone()
        var ret = false
        synchronized(lock) {
            ret = history.addAll(new_history)
        }
        val task = Runnable {
            for(r: Reading in new_history) {
                mDb?.glucoseDataDao()?.insert(r)
            }
            Log.d(TAG, "Inserted into db!")
        }
        mWorker.postTask(task)
        return ret
    }
    fun dump() : ByteArray {
        synchronized(lock) { return raw_data }
    }
    companion object {
        private var INSTANCE : DataContainer? = null
        fun getInstance(context : Context) : DataContainer {
            if (INSTANCE == null) {
                synchronized(DataContainer::class) {
                    if(INSTANCE == null)
                        INSTANCE = DataContainer(context)
                }
            }
            return this.INSTANCE!!
        }
    }

}