package com.example.chargescience.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.chargescience.data.db.entity.BatteryCapacityEstimateEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BatteryCapacityEstimateDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEstimate(estimate: BatteryCapacityEstimateEntity): Long

    @Query("SELECT * FROM battery_capacity_estimates ORDER BY timestamp DESC")
    fun getAllEstimatesFlow(): Flow<List<BatteryCapacityEstimateEntity>>

    @Query("SELECT * FROM battery_capacity_estimates WHERE eligibilityStatus = 'VALID' ORDER BY timestamp DESC")
    fun getValidEstimatesFlow(): Flow<List<BatteryCapacityEstimateEntity>>

    @Query("SELECT * FROM battery_capacity_estimates WHERE eligibilityStatus = 'VALID' ORDER BY timestamp DESC")
    suspend fun getValidEstimates(): List<BatteryCapacityEstimateEntity>

    @Query("SELECT * FROM battery_capacity_estimates WHERE sessionId = :sessionId LIMIT 1")
    suspend fun getEstimateForSession(sessionId: Long): BatteryCapacityEstimateEntity?
}
