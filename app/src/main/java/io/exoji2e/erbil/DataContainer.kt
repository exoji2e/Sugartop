package io.exoji2e.erbil

import android.content.Context
import android.util.Log

class DataContainer {
    private val recent = mutableListOf<GlucoseEntry>()
    private val history = mutableListOf<GlucoseEntry>()
    val noH = 32
    val TAG = "DataContainer"
    private val mWorker : DbWorkerThread
    private var mDb : ErbilDataBase? = null
    private var done : Boolean = false
    private var lastId : Int = -1
    private val lock = java.lang.Object()
    private var raw_data = ByteArray(360)
    constructor(context : Context) {
        mWorker = DbWorkerThread("dbWorker")
        mWorker.start()
        val task = Runnable {
            mDb = ErbilDataBase.getInstance(context)
            val glucoseData =
                    mDb?.glucoseEntryDao()?.getAll()
            synchronized(lock){
                if (glucoseData == null || glucoseData.isEmpty()) {
                    Log.d(TAG, "No data in db")
                } else {
                    for(g: GlucoseEntry in glucoseData) {
                        if(g.history) history.add(g)
                        else recent.add(g)
                        lastId = Math.max(lastId, g.id)
                    }
                    history.sortBy { entry -> entry.utcTimeStamp }
                    recent.sortBy { entry -> entry.utcTimeStamp }
                    Log.d(TAG, "history contained %d items".format(history.size))
                    Log.d(TAG, "recent contained %d items".format(recent.size))
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
        val now_history = RawParser.history(raw_data)
        val now_recent = RawParser.recent(raw_data)

        val recent_prepared = prepare(now_recent, sensorId, recent, 1, readingTime - 16*Time.MINUTE)
        extend(recent_prepared, recent)
        val history_prepared = prepare(now_history, sensorId, history, 15, start)
        extend(history_prepared, history)
        Log.d(TAG, String.format("recent_size %d", recent.size))
        Log.d(TAG, String.format("histroy_size %d", history.size))
    }

    private fun extend(v: List<GlucoseReading>, into: MutableList<GlucoseEntry>) {
        synchronized(lock) {
            val toExtend = v.filter {g: GlucoseReading -> g.status != 0 && g.value > 10}
                    .mapIndexed{i: Int, g: GlucoseReading -> GlucoseEntry(g, lastId + 1 + i)}
            lastId += toExtend.size
            into.addAll(toExtend)
            val task = Runnable {
                for(r: GlucoseEntry in toExtend) {
                    mDb?.glucoseEntryDao()?.insert(r)
                }
                Log.d(TAG, "Inserted into db!")
            }
            mWorker.postTask(task)
        }
    }

    // Inspects last entry from the same sensor and filters out all that are already logged.
    private fun prepare(chunks : List<SensorChunk>, sensorId : Long, into: List<GlucoseEntry>, dt : Int, start : Long) : List<GlucoseReading> {
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
    private fun get(after: Long, before : Long) : List<GlucoseEntry> {
        waitForDone()
        synchronized(lock) {
            return history.filter{r -> r.utcTimeStamp < before && r.utcTimeStamp > after}
        }
    }
    fun get8h() : List<GlucoseEntry> {
        val now = Time.now()
        return get(now - Time.HOUR*8L, now)
    }
    fun get24h() : List<GlucoseEntry> {
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
    fun size() : Int {
        waitForDone()
        synchronized(lock) {
            return history.size
        }
    }
    fun dump() : ByteArray {
        synchronized(lock) { return raw_data }
    }
    fun insertIntoDb(manual : ManualGlucoseEntry) {
        waitForDone()
        synchronized(lock){
            val task = Runnable{
                mDb!!.manualEntryDao().insert(manual)
                Log.d(TAG, "inserted manual entry into db.")
                val v :List<ManualGlucoseEntry> = mDb!!.manualEntryDao().getAll()
                Log.d(TAG, String.format("table size: %d", v.size))
            }
            mWorker.postTask(task)
        }
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