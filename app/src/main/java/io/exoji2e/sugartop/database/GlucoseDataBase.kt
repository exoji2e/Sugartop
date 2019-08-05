package io.exoji2e.sugartop.database

import android.arch.persistence.room.Database
import android.arch.persistence.room.Room
import android.arch.persistence.room.RoomDatabase
import android.content.Context
import android.arch.persistence.db.SupportSQLiteDatabase
import android.arch.persistence.room.migration.Migration
import io.exoji2e.sugartop.*
import io.exoji2e.sugartop.settings.UserData


@Database(entities = arrayOf(GlucoseEntry::class, SensorContact::class, ManualGlucoseEntry::class, SensorCalibration::class), version = 2)
abstract class GlucoseDataBase : RoomDatabase() {
    abstract fun glucoseEntryDao(): GlucoseDao
    abstract fun manualEntryDao(): ManualDao
    abstract fun sensorContactDao(): SensorContactDao
    abstract fun sensorCalibrationDao(): SensorCalibrationDao
    companion object {
        val MIGRATION_1_2: Migration = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("CREATE TABLE IF NOT EXISTS `SensorCalibrations` (`sensorId` INTEGER NOT NULL, `first` REAL NOT NULL, `second` REAL NOT NULL, PRIMARY KEY(`sensorId`))")
            }
        }
        private var INSTANCE: GlucoseDataBase? = null

        fun getInstance(context: Context): GlucoseDataBase {
            synchronized(GlucoseDataBase::class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                            context.getApplicationContext(),
                            GlucoseDataBase::class.java,
                            UserData.get_db_path(context))
                            .addMigrations(MIGRATION_1_2)
                            .build()
                }
                return INSTANCE!!
            }


        }

        fun destroyInstance() {
            if (INSTANCE?.isOpen == true)
                INSTANCE?.close()
            INSTANCE = null
        }
    }
}