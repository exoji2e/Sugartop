package io.exoji2e.erbil

import android.arch.persistence.room.Dao
import android.arch.persistence.room.Insert
import android.arch.persistence.room.OnConflictStrategy.REPLACE
import android.arch.persistence.room.Query

@Dao
interface GlucoseDataDao {

    @Query("SELECT * from glucoseData")
    fun getAll(): List<Reading>

    @Insert(onConflict = REPLACE)
    fun insert(reading: Reading)

    //Never use!
    @Query("DELETE from glucoseData")
    fun deleteAll()
}