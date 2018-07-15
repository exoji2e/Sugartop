package io.exoji2e.erbil

import android.arch.persistence.room.Database
import android.arch.persistence.room.Room
import android.arch.persistence.room.RoomDatabase
import android.content.Context

@Database(entities = arrayOf(Reading::class), version = 1)
abstract class GlucoseDataBase : RoomDatabase() {

    abstract fun glucoseDataDao(): GlucoseDataDao

    companion object {
        private var INSTANCE: GlucoseDataBase? = null

        fun getInstance(context: Context): GlucoseDataBase? {
            if (INSTANCE == null) {
                synchronized(GlucoseDataBase::class) {
                    if (INSTANCE == null) {
                        INSTANCE = Room.databaseBuilder(
                                context.getApplicationContext(),
                                GlucoseDataBase::class.java,
                                "glucose.db")
                            .build()
                    }
                }
            }
            return INSTANCE
        }

        fun destroyInstance() {
            INSTANCE = null
        }
    }
}