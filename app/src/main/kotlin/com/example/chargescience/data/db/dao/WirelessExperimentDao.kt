package com.example.chargescience.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.chargescience.data.db.entity.WirelessExperimentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WirelessExperimentDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExperiment(experiment: WirelessExperimentEntity): Long

    @Update
    suspend fun updateExperiment(experiment: WirelessExperimentEntity): Int

    @Query("SELECT * FROM wireless_experiments ORDER BY startTimestamp DESC")
    fun getAllExperimentsFlow(): Flow<List<WirelessExperimentEntity>>

    @Query("SELECT * FROM wireless_experiments ORDER BY startTimestamp DESC")
    suspend fun getAllExperiments(): List<WirelessExperimentEntity>

    @Query("SELECT * FROM wireless_experiments WHERE id = :id")
    suspend fun getExperimentById(id: Long): WirelessExperimentEntity?

    @Query("DELETE FROM wireless_experiments WHERE id = :id")
    suspend fun deleteExperiment(id: Long): Int
}
