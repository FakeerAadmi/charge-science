package com.example.chargescience.data.db.dao

import androidx.room.*
import com.example.chargescience.data.db.entity.ChargingSessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ChargingSessionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: ChargingSessionEntity): Long

    @Update
    suspend fun updateSession(session: ChargingSessionEntity): Int

    @Query("SELECT * FROM charging_sessions WHERE id = :sessionId")
    suspend fun getSessionById(sessionId: Long): ChargingSessionEntity?

    @Query("SELECT * FROM charging_sessions WHERE isCompleted = 0 ORDER BY startTimestamp DESC LIMIT 1")
    suspend fun getActiveSession(): ChargingSessionEntity?

    @Query("SELECT * FROM charging_sessions WHERE isCompleted = 0")
    suspend fun getIncompleteSessions(): List<ChargingSessionEntity>

    @Query("SELECT * FROM charging_sessions ORDER BY startTimestamp DESC")
    fun getAllSessionsFlow(): Flow<List<ChargingSessionEntity>>

    @Query("SELECT * FROM charging_sessions ORDER BY startTimestamp DESC")
    suspend fun getAllSessions(): List<ChargingSessionEntity>

    @Query("UPDATE charging_sessions SET accessoryId = :accessoryId WHERE id = :sessionId")
    suspend fun linkAccessoryToSession(sessionId: Long, accessoryId: Long?): Int

    @Query("DELETE FROM charging_sessions WHERE id = :sessionId")
    suspend fun deleteSession(sessionId: Long): Int

    @Query("DELETE FROM charging_sessions")
    suspend fun clearAllSessions(): Int
}
