package io.exoji2e.erbil

import android.arch.persistence.room.Database
import android.arch.persistence.room.Room
import android.arch.persistence.room.RoomDatabase
import android.content.Context

@Database(entities = arrayOf(GlucoseEntry::class, SensorContact::class, ManualGlucoseEntry::class), version = 1)
abstract class ErbilDataBase : RoomDatabase() {

    abstract fun glucoseEntryDao(): GlucoseDao
    abstract fun manualEntryDao(): ManualDao
    abstract fun sensorContactDao(): SensorContactDao

    companion object {
        val NAME = "Erbil.db"
        private var INSTANCE: ErbilDataBase? = null

        fun getInstance(context: Context): ErbilDataBase {
            synchronized(ErbilDataBase::class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                            context.getApplicationContext(),
                            ErbilDataBase::class.java,
                            NAME)
                            .build()
                }
                return INSTANCE!!
            }


        }

        fun destroyInstance() {
            INSTANCE = null
        }
    }
}