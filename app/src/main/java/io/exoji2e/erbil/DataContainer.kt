package io.exoji2e.erbil

import android.content.Context
import android.util.Log

class DataContainer {
    private val hist = mutableListOf<GlucoseEntry>()
    private val recent = mutableListOf<GlucoseEntry>()
    private val history = mutableListOf<Reading>()
    val noH = 32
    val TAG = "DataContainer"
    private val mWorker : DbWorkerThread
    private var mDb : GlucoseDataBase? = null
    private var done : Boolean = false
    private var lastId : Int = -1
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
    fun append(raw_data: ByteArray, readingTime: Long, sensorId : Long) {
        synchronized(lock) {
            this.raw_data = raw_data
        }
        val timestamp = RawParser.timestamp(raw_data)
        if(timestamp == 0) {
            return
        }
        // Timestamp is 2 mod 15 every time a new reading to history is done.
        val minutesSinceLast = (timestamp + 12)%15
        val start = readingTime - Time.MINUTE*(15*(noH - 1) + minutesSinceLast)
        val sensor_history = RawParser.history(raw_data)
        val now_history = RawParser.historySensorChunk(raw_data)
        val now_recent = RawParser.recentSensorChunk(raw_data)
        val lastStored = last()
        var pushed = false
        if(lastStored != null) {
            var match = -1
            for (i in 0..sensor_history.size - 1) {
                if (lastStored.eq(sensor_history[i])) {
                    match = i
                }
            }
            if (match > -1) {
                val v = sensor_history.slice(IntRange(match + 1, sensor_history.size - 1))
                        .mapIndexed { i: Int, sensorData: SensorData ->
                            Reading(
                                    lastStored.utcTimeStamp + (i + 1) * 15 * Time.MINUTE, sensorData)
                        }
                if (v.isNotEmpty())
                    push(v)
                pushed = true
            }
        }
        if(!pushed){
            val v = sensor_history.mapIndexed { i: Int, sensorData: SensorData -> Reading(start + i * 15 * Time.MINUTE, sensorData) }
            push(v)
            pushed = true
        }

        val recent_push = topush(now_recent, sensorId, recent, 1, readingTime - 16*Time.MINUTE)
        extend(recent_push, recent)
        val history_push = topush(now_history, sensorId, hist, 15, start)
        extend(history_push, hist)
        Log.d(TAG, String.format("recent_size %d", recent.size))
        Log.d(TAG, String.format("histroy_size %d", hist.size))
    }

    private fun extend(v: List<GlucoseReading>, into: MutableList<GlucoseEntry>) {
        synchronized(lock) {
            val toExtend = v.filter {g: GlucoseReading -> g.status != 0 && g.value > 10}
                    .mapIndexed{i: Int, g: GlucoseReading -> GlucoseEntry(g, lastId + 1 + i)}
            lastId += toExtend.size
            into.addAll(toExtend)
            // TODO: DB query.
        }
    }

    private fun topush(chunks : List<SensorChunk>, sensorId : Long, into: List<GlucoseEntry>, dt : Int, start : Long) : List<GlucoseReading> {
        val lastRecent = last(sensorId, into)
        if(lastRecent != null) {
            var match = -1
            for (i in 0 until chunks.size) {
                if (lastRecent.eq(chunks[i])) {
                    match = i
                }
            }
            if(match > -1) {
                return chunks.slice(IntRange(match + 1, chunks.size - 1))
                        .mapIndexed { i: Int, chunk: SensorChunk ->
                            GlucoseReading(chunk,
                                    lastRecent.utcTimeStamp + (i + 1) * dt * Time.MINUTE, sensorId)
                        }
            }
        }
        return chunks.mapIndexed { i: Int, chunk: SensorChunk ->
            GlucoseReading(chunk, start + i * 15 * Time.MINUTE, sensorId) }
    }
    private fun get(after: Long, before : Long) : List<Reading> {
        waitForDone()
        synchronized(lock) {
            return history.filter{r -> r.utcTimeStamp < before && r.utcTimeStamp > after}
        }
    }
    fun get8h() : List<Reading> {
        val now = Time.now()
        return get(now - Time.HOUR*8L, now)
    }
    fun get24h() : List<Reading> {
        val now = Time.now()
        return get(now - Time.HOUR*24L, now)
    }
    fun last(sensorId: Long, v: List<GlucoseEntry>) : GlucoseEntry? {
        waitForDone()
        synchronized(lock) {
            val sz = v.size - 1
            for(i in 0 until v.size) {
                if(v[sz - i].sensorId == sensorId) return v[sz - i]
            }
            return null
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
        // 0-readings after sensor startup seem to have statuscode 0 and/or readingValue <= 10.
        val toAdd = new_history.filter{r -> RawParser.byte2uns(r.statusCode) != 0 && r.readingValue > 10}
        synchronized(lock) {
            ret = history.addAll(toAdd)
        }
        val task = Runnable {
            for(r: Reading in toAdd) {
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