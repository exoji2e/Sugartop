package io.exoji2e.erbil

import android.arch.persistence.room.Dao
import android.arch.persistence.room.Insert
import android.arch.persistence.room.OnConflictStrategy
import android.arch.persistence.room.Query

@Dao
interface GlucoseDao {
    @Query("SELECT * from GlucoseEntries")
    fun getAll(): List<GlucoseEntry>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(entry: GlucoseEntry)

    //Never use!
    @Query("DELETE from GlucoseEntries")
    fun deleteAll()
}