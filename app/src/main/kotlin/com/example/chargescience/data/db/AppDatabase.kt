package com.example.chargescience.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.chargescience.data.db.dao.AccessoryProfileDao
import com.example.chargescience.data.db.dao.BatteryCapacityEstimateDao
import com.example.chargescience.data.db.dao.ChargingSessionDao
import com.example.chargescience.data.db.dao.TelemetrySampleDao
import com.example.chargescience.data.db.dao.WirelessExperimentDao
import com.example.chargescience.data.db.entity.AccessoryProfileEntity
import com.example.chargescience.data.db.entity.BatteryCapacityEstimateEntity
import com.example.chargescience.data.db.entity.ChargingSessionEntity
import com.example.chargescience.data.db.entity.TelemetrySampleEntity
import com.example.chargescience.data.db.entity.WirelessExperimentEntity

@Database(
    entities = [
        ChargingSessionEntity::class,
        TelemetrySampleEntity::class,
        AccessoryProfileEntity::class,
        WirelessExperimentEntity::class,
        BatteryCapacityEstimateEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun chargingSessionDao(): ChargingSessionDao
    abstract fun telemetrySampleDao(): TelemetrySampleDao
    abstract fun accessoryProfileDao(): AccessoryProfileDao
    abstract fun wirelessExperimentDao(): WirelessExperimentDao
    abstract fun batteryCapacityEstimateDao(): BatteryCapacityEstimateDao

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

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS wireless_experiments (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        experimentType TEXT NOT NULL,
                        label TEXT NOT NULL,
                        startTimestamp INTEGER NOT NULL,
                        endTimestamp INTEGER,
                        durationSeconds INTEGER NOT NULL,
                        startSoc INTEGER NOT NULL,
                        endSoc INTEGER NOT NULL,
                        startTempCelsius REAL NOT NULL,
                        maxTempCelsius REAL NOT NULL,
                        endTempCelsius REAL NOT NULL,
                        startThermalStatus INTEGER NOT NULL,
                        maxThermalStatus INTEGER NOT NULL,
                        interruptionTimestamp INTEGER,
                        elapsedToInterruptionSeconds INTEGER,
                        didDisconnect INTEGER NOT NULL,
                        cutoffReason TEXT,
                        preInterruptionAvgPowerWatts REAL,
                        preInterruptionMaxTempCelsius REAL,
                        preInterruptionThermalStatus INTEGER,
                        preInterruptionDropPattern TEXT,
                        ringOuterDiameterMm REAL NOT NULL,
                        ringInnerDiameterMm REAL NOT NULL,
                        ringThicknessMm REAL NOT NULL,
                        ringOffsetMm REAL NOT NULL,
                        ringIsPermanentMagnet INTEGER NOT NULL,
                        ringBrandOrModel TEXT NOT NULL,
                        accessoryId INTEGER,
                        diagnosticOutcome TEXT,
                        confidenceScore REAL NOT NULL,
                        userNotes TEXT,
                        FOREIGN KEY(accessoryId) REFERENCES accessory_profiles(id) ON UPDATE NO ACTION ON DELETE SET_NULL
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_wireless_experiments_accessoryId ON wireless_experiments(accessoryId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_wireless_experiments_startTimestamp ON wireless_experiments(startTimestamp)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS battery_capacity_estimates (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        sessionId INTEGER NOT NULL,
                        timestamp INTEGER NOT NULL,
                        startSoc INTEGER NOT NULL,
                        endSoc INTEGER NOT NULL,
                        deltaSoc INTEGER NOT NULL,
                        deltaChargeCounterUah INTEGER NOT NULL,
                        calculatedCapacityMah REAL NOT NULL,
                        estimatedSohPercent REAL NOT NULL,
                        averageTempCelsius REAL NOT NULL,
                        maxTempCelsius REAL NOT NULL,
                        cycleCountAtSession INTEGER NOT NULL,
                        eligibilityStatus TEXT NOT NULL,
                        rejectionReason TEXT,
                        FOREIGN KEY(sessionId) REFERENCES charging_sessions(id) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_battery_capacity_estimates_sessionId ON battery_capacity_estimates(sessionId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_battery_capacity_estimates_timestamp ON battery_capacity_estimates(timestamp)")
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "charge_science.db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
