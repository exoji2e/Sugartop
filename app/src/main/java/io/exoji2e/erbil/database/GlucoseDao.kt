package io.exoji2e.erbil.database

import android.arch.persistence.room.Dao
import android.arch.persistence.room.Insert
import android.arch.persistence.room.OnConflictStrategy
import android.arch.persistence.room.Query

@Dao
interface GlucoseDao {
    @Query("SELECT * FROM GlucoseEntries")
    fun getAll(): List<GlucoseEntry>

    @Query("SELECT * FROM GlucoseEntries WHERE :start_time < utcTimeStamp AND utcTimeStamp < :end_time")
    fun getBetween(start_time: Long, end_time : Long) : List<GlucoseEntry>

    @Query("SELECT * FROM GlucoseEntries WHERE history = :history AND sensorId = :sensor_id ORDER BY utcTimeStamp DESC LIMIT 1")
    fun getLast(sensor_id: Long, history : Boolean) : GlucoseEntry?

    @Query("SELECT * FROM GlucoseEntries WHERE history = :history ORDER BY utcTimeStamp DESC LIMIT 1")
    fun getLast(history : Boolean) : GlucoseEntry?

    @Query("SELECT COUNT(*) FROM GlucoseEntries")
    fun getSize() : Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(entry: GlucoseEntry)

    //Never use!
    @Query("DELETE from GlucoseEntries")
    fun deleteAll()
}