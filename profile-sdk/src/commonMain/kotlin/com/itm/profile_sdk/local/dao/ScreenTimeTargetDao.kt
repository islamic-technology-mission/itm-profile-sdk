package com.itm.profile_sdk.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.itm.profile_sdk.local.entity.ScreenTimeTargetEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ScreenTimeTargetDao {

    @Query("SELECT * FROM screen_time_target WHERE userId = :userId")
    fun observeTarget(userId: String): Flow<ScreenTimeTargetEntity?>

    @Query("SELECT * FROM screen_time_target WHERE userId = :userId")
    suspend fun getTarget(userId: String): ScreenTimeTargetEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTarget(target: ScreenTimeTargetEntity)
}