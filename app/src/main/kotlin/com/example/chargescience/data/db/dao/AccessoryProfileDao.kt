package com.example.chargescience.data.db.dao

import androidx.room.*
import com.example.chargescience.data.db.entity.AccessoryProfileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AccessoryProfileDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccessory(accessory: AccessoryProfileEntity): Long

    @Update
    suspend fun updateAccessory(accessory: AccessoryProfileEntity): Int

    @Query("SELECT * FROM accessory_profiles WHERE id = :id")
    suspend fun getAccessoryById(id: Long): AccessoryProfileEntity?

    @Query("SELECT * FROM accessory_profiles ORDER BY lastSeenTimestamp DESC")
    fun getAllAccessoriesFlow(): Flow<List<AccessoryProfileEntity>>

    @Query("SELECT * FROM accessory_profiles ORDER BY lastSeenTimestamp DESC")
    suspend fun getAllAccessories(): List<AccessoryProfileEntity>

    @Query("SELECT * FROM accessory_profiles WHERE type = :type ORDER BY lastSeenTimestamp DESC")
    fun getAccessoriesByTypeFlow(type: String): Flow<List<AccessoryProfileEntity>>

    @Query("DELETE FROM accessory_profiles WHERE id = :id")
    suspend fun deleteAccessory(id: Long): Int

    @Query("DELETE FROM accessory_profiles")
    suspend fun clearAllAccessories(): Int
}
