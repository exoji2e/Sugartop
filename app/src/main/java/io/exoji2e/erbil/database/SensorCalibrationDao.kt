package io.exoji2e.erbil.database

import android.arch.persistence.room.Dao
import android.arch.persistence.room.Insert
import android.arch.persistence.room.OnConflictStrategy
import android.arch.persistence.room.Query
import io.exoji2e.erbil.SensorCalibration

@Dao
interface SensorCalibrationDao {
    @Query("SELECT * from SensorCalibrations")
    fun getAll(): List<SensorCalibration>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(entry: SensorCalibration)

    @Query("DELETE from SensorCalibrations")
    fun deleteAll()
}