package io.exoji2e.erbil

import android.content.Context
import android.util.Log

class DataContainer {
    private val recent = mutableListOf<GlucoseEntry>()
    private val history = mutableListOf<GlucoseEntry>()
    val noH = 32
    val TAG = "DataContainer"
    private var mDb : ErbilDataBase? = null
    private var done : Boolean = false
    private var lastId : Int = -1
    private val lock = java.lang.Object()
    private var raw_data = ByteArray(360)
    constructor(context : Context) {
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
            mDb?.sensorContactDao()?.insert(SensorContact(0, readingTime, sensorId, 0, 0))
            return
        }
        // Timestamp is 2 mod 15 every time a new reading to history is done.
        val minutesSinceLast = (timestamp + 12)%15
        val start = readingTime - Time.MINUTE*(15*(noH - 1) + minutesSinceLast)
        val now_history = RawParser.history(raw_data)
        val now_recent = RawParser.recent(raw_data)

        val recent_prepared = prepare(now_recent, sensorId, recent, 1*Time.MINUTE, readingTime - 16*Time.MINUTE, true)
        val history_prepared = prepare(now_history, sensorId, history, 15*Time.MINUTE, start, minutesSinceLast != 14)
        val added = extend(recent_prepared, recent) + extend(history_prepared, history)
        mDb?.sensorContactDao()?.insert(SensorContact(0, readingTime, sensorId, timestamp, added))

        Log.d(TAG, String.format("recent_size %d", recent.size))
        Log.d(TAG, String.format("histroy_size %d", history.size))
    }

    fun insert(v: List<GlucoseEntry>) {
        waitForDone()
        synchronized(lock){
            if(history.size + recent.size != 0) return
            for(g in v) {
                if(g.history) history.add(g)
                else recent.add(g)
                mDb?.glucoseEntryDao()?.insert(g)
                lastId = Math.max(lastId, g.id)
            }
        }
        Log.d(TAG, String.format("inserted %d vales into database", v.size))
    }

    private fun extend(v: List<GlucoseReading>, into: MutableList<GlucoseEntry>) : Int {
        synchronized(lock) {
            val toExtend = v.filter { g: GlucoseReading -> g.status != 0 && g.value > 10 }
                    .mapIndexed { i: Int, g: GlucoseReading -> GlucoseEntry(g, lastId + 1 + i) }
            lastId += toExtend.size
            into.addAll(toExtend)
            for (r: GlucoseEntry in toExtend) {
                mDb?.glucoseEntryDao()?.insert(r)
            }
            Log.d(TAG, "Inserted into db!")
            return toExtend.size
        }
    }

    // Inspects last entry from the same sensor and filters out all that are already logged.
    private fun prepare(chunks : List<SensorChunk>,
                        sensorId : Long,
                        into: List<GlucoseEntry>,
                        dt : Long,
                        start : Long,
                        certain : Boolean) : List<GlucoseReading> {
        val lastRecent = last(sensorId, into)
        if(lastRecent != null) {
            var match = -1
            for (i in 0 until chunks.size) {
                if (lastRecent.eq(chunks[i])) {
                    match = i
                }
            }
            if(match > -1) {
                val range = IntRange(match + 1, chunks.size - 1)
                if(!certain)
                    return chunks.slice(range)
                            .mapIndexed { i: Int, chunk: SensorChunk ->
                            GlucoseReading(chunk,
                                    lastRecent.utcTimeStamp + (i + 1) * dt, sensorId)
                        }
                else {
                    return chunks.mapIndexed { i: Int, chunk: SensorChunk ->
                        GlucoseReading(chunk,
                                start + i * dt, sensorId)
                    }.slice(range)
                }
            }
        }
        return chunks.mapIndexed { i: Int, chunk: SensorChunk ->
            GlucoseReading(chunk, start + i * dt, sensorId) }
    }
    private fun get(after: Long, before : Long) : List<GlucoseEntry> {
        waitForDone()
        synchronized(lock) {
            return (history.filter{r -> r.utcTimeStamp < before && r.utcTimeStamp > after} +
                    recent.filter{r -> r.utcTimeStamp < before && r.utcTimeStamp > after})
                    .sortedBy { g -> g.utcTimeStamp }
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
    private fun last(sensorId: Long, v: List<GlucoseEntry>) : GlucoseEntry? {
        waitForDone()
        synchronized(lock) {
            val sz = v.size - 1
            val now = Time.now()
            for(i in 0 until v.size) {
                if(v[sz - i].sensorId == sensorId) return v[sz - i]
                if(now - Time.HOUR*24 > v[sz-i].utcTimeStamp) break
            }
            return null
        }
    }
    fun size() : Int {
        waitForDone()
        synchronized(lock) {
            return history.size + recent.size
        }
    }
    fun dump() : ByteArray {
        synchronized(lock) { return raw_data }
    }
    fun insertIntoDb(manual : ManualGlucoseEntry) {
        waitForDone()
        synchronized(lock) {
            mDb!!.manualEntryDao().insert(manual)
            Log.d(TAG, "inserted manual entry into db.")
            val v: List<ManualGlucoseEntry> = mDb!!.manualEntryDao().getAll()
            Log.d(TAG, String.format("table size: %d", v.size))
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