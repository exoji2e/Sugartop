package io.exoji2e.erbil

import android.arch.persistence.room.Dao
import android.arch.persistence.room.Insert
import android.arch.persistence.room.OnConflictStrategy
import android.arch.persistence.room.Query

@Dao
interface SensorContactDao {
    @Query("SELECT * from SensorContacts")
    fun getAll(): List<SensorContact>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(contact: SensorContact)

    //Never use!
    @Query("DELETE from SensorContacts")
    fun deleteAll()
}