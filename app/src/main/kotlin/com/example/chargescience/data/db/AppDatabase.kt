package com.example.chargescience.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.chargescience.data.db.dao.ChargingSessionDao
import com.example.chargescience.data.db.dao.TelemetrySampleDao
import com.example.chargescience.data.db.entity.ChargingSessionEntity
import com.example.chargescience.data.db.entity.TelemetrySampleEntity

@Database(
    entities = [
        ChargingSessionEntity::class,
        TelemetrySampleEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun chargingSessionDao(): ChargingSessionDao
    abstract fun telemetrySampleDao(): TelemetrySampleDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "charge_science.db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
