package io.exoji2e.sugartop.database

import android.arch.persistence.room.Dao
import android.arch.persistence.room.Insert
import android.arch.persistence.room.OnConflictStrategy
import android.arch.persistence.room.Query
import android.arch.persistence.room.Delete


@Dao
interface ManualDao {
    @Query("SELECT * from ManualGlucoseEntries")
    fun getAll(): List<ManualGlucoseEntry>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(entry: ManualGlucoseEntry)

    @Delete
    fun deleteRecord(entry: ManualGlucoseEntry)

    //Never use!
    @Query("DELETE from ManualGlucoseEntries")
    fun deleteAll()
}