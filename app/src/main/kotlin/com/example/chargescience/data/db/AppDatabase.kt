package com.example.chargescience.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.chargescience.data.db.dao.AccessoryProfileDao
import com.example.chargescience.data.db.dao.ChargingSessionDao
import com.example.chargescience.data.db.dao.TelemetrySampleDao
import com.example.chargescience.data.db.entity.AccessoryProfileEntity
import com.example.chargescience.data.db.entity.ChargingSessionEntity
import com.example.chargescience.data.db.entity.TelemetrySampleEntity

@Database(
    entities = [
        ChargingSessionEntity::class,
        TelemetrySampleEntity::class,
        AccessoryProfileEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun chargingSessionDao(): ChargingSessionDao
    abstract fun telemetrySampleDao(): TelemetrySampleDao
    abstract fun accessoryProfileDao(): AccessoryProfileDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE charging_sessions ADD COLUMN accessoryId INTEGER DEFAULT NULL")
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS accessory_profiles (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        type TEXT NOT NULL,
                        nickname TEXT NOT NULL,
                        manufacturer TEXT NOT NULL,
                        model TEXT NOT NULL,
                        claimedWattage REAL NOT NULL,
                        verifiedProtocols TEXT NOT NULL,
                        maxObservedPowerWatts REAL NOT NULL,
                        firstSeenTimestamp INTEGER NOT NULL,
                        lastSeenTimestamp INTEGER NOT NULL,
                        notes TEXT NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "charge_science.db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
