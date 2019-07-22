package io.exoji2e.erbil

import android.content.Context
import android.util.Log
import io.exoji2e.erbil.database.*
import kotlin.math.min

class DataContainer {
    val noH = 32
    val TAG = "DataContainer"
    private var mDb : ErbilDataBase? = null
    private val lock = java.lang.Object()
    private var lastTimeStamp : Int = 0
    //private var raw_data : ByteArray = byteArrayOf()
    private var readings : MutableList<ByteArray> = mutableListOf()
    constructor(context : Context) {
        mDb = ErbilDataBase.getInstance(context)
    }

    fun append(raw_data: ByteArray, readingTime: Long, sensorId : Long) {
        synchronized(lock) {
            readings.add(raw_data.copyOf())
            val timestamp = RawParser.timestamp(raw_data)
            if (timestamp == 0) {
                mDb?.sensorContactDao()?.insert(SensorContact(0, readingTime, sensorId, 0, 0))
                return
            }
            // Timestamp is 2 mod 15 every time a new reading to history is done.
            val minutesSinceLast = (timestamp + 12) % 15
            val start = readingTime - Time.MINUTE * (15 * (noH - 1) + minutesSinceLast)
            val now_history = RawParser.history(raw_data)
            val now_recent = RawParser.recent(raw_data)

            val history_prepared = prepare(now_history, sensorId, 15 * Time.MINUTE, start, minutesSinceLast != 14 && timestamp < Time.DURATION_MINUTES, true)
            val start_recent =
                    if (history_prepared.isEmpty()) {
                        val last = mDb?.glucoseEntryDao()?.getLast(sensorId, true)
                        if (last != null) {
                            min(last.utcTimeStamp, readingTime - 16 * Time.MINUTE)
                        } else {
                            readingTime - 16 * Time.MINUTE
                        }
                    } else {
                        min(readingTime - 16 * Time.MINUTE, history_prepared.last().utcTimeStamp)
                    }

            val recent_prepared = prepare(now_recent, sensorId, 1 * Time.MINUTE, start_recent, true, false)
            val added = extend(recent_prepared) + extend(history_prepared)
            mDb?.sensorContactDao()?.insert(SensorContact(0, sensorId, readingTime, timestamp, added))
            lastTimeStamp = timestamp
        }
    }
    fun get_sz_raw_data() : Int {
        return readings.size
    }
    fun get_raw_data(i: Int) : ByteArray{
        synchronized(lock){
            if(0<= i && i < readings.size)
                return readings[i]
            else
                return byteArrayOf()
        }
    }
    fun insert(v: List<GlucoseEntry>) {
        synchronized(lock){
            if(size() != 0) return
            for(g in v) {
                mDb?.glucoseEntryDao()?.insert(g)
            }
        }
        Log.d(TAG, String.format("inserted %d vales into database", v.size))
    }

    private fun extend(v: List<GlucoseReading>) : Int {
        synchronized(lock) {
            val toExtend = v.filter { g: GlucoseReading -> g.status != 0 && g.value > 10 }
                    .map { g: GlucoseReading -> GlucoseEntry(g, 0) }
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
                        dt : Long,
                        start : Long,
                        certain : Boolean,
                        isHistory: Boolean) : List<GlucoseReading> {
        val lastRecent = mDb?.glucoseEntryDao()?.getLast(sensorId, isHistory)

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
            GlucoseReading(chunk, start + i * dt, sensorId)
        }
    }
    fun nice(g : GlucoseEntry) : Boolean = g.status == 200 && (g.value < 5000 && g.value > 10)
    fun get(after: Long, before : Long) : List<GlucoseEntry> {
        return get(after, before, true)
    }
    fun get(after: Long, before : Long, nice : Boolean) : List<GlucoseEntry> {
        synchronized(lock) {
            val v = mDb?.glucoseEntryDao()?.getBetween(after, before).orEmpty().sortedBy{ g -> g.utcTimeStamp }
            if(!nice) return v
            else return v.filter{g -> nice(g)}
        }
    }

    private fun last() : GlucoseEntry? {
        return mDb?.glucoseEntryDao()?.getLast(false)
    }
    fun guess() : Pair<GlucoseReading, GlucoseReading>? {
        synchronized(lock){
            val last = last()
            if(last == null || !nice(last)) return null
            val last_as_reading = GlucoseReading(last.value, last.utcTimeStamp, last.sensorId, last.status, false, 0)
            val candidates = get(last.utcTimeStamp - Time.MINUTE*5, last.utcTimeStamp)
            val real = candidates.filter{g -> g.history == false && g.sensorId == last.sensorId}
            if(real.isEmpty()) return null
            val entry = real.first()
            val guess = last.value * 2 - entry.value
            val time = last.utcTimeStamp * 2 - entry.utcTimeStamp
            return Pair(last_as_reading, GlucoseReading(guess,
                    time,
                    last.sensorId, last.status, false, 0))
        }
    }
    fun lastTimeStamp() : Int {
        synchronized(lock){return lastTimeStamp}
    }

    fun size() : Int {
        synchronized(lock) {
            val sz = mDb?.glucoseEntryDao()?.getSize()
            return if(sz == null) 0 else sz
        }
    }

    fun insertManual(manual : ManualGlucoseEntry) {
        synchronized(lock) {
            mDb!!.manualEntryDao().insert(manual)
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
