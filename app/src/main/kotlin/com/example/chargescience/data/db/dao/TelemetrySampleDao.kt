package com.example.chargescience.data.db.dao

import androidx.room.*
import com.example.chargescience.data.db.entity.TelemetrySampleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TelemetrySampleDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSample(sample: TelemetrySampleEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSamples(samples: List<TelemetrySampleEntity>): List<Long>

    @Query("SELECT * FROM telemetry_samples WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    fun getSamplesForSessionFlow(sessionId: Long): Flow<List<TelemetrySampleEntity>>

    @Query("SELECT * FROM telemetry_samples WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    suspend fun getSamplesForSession(sessionId: Long): List<TelemetrySampleEntity>

    @Query("SELECT COUNT(*) FROM telemetry_samples WHERE sessionId = :sessionId")
    suspend fun getSampleCountForSession(sessionId: Long): Int

    @Query("SELECT * FROM telemetry_samples WHERE sessionId = :sessionId ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestSampleForSession(sessionId: Long): TelemetrySampleEntity?

    @Query("DELETE FROM telemetry_samples WHERE sessionId = :sessionId")
    suspend fun deleteSamplesForSession(sessionId: Long): Int
}
