package com.itm.profile_sdk.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.itm.profile_sdk.local.entity.ScreenTimeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ScreenTimeDao {

    @Query("SELECT * FROM screen_time WHERE userId = :userId ORDER BY date ASC")
    fun observeScreenTime(userId: String): Flow<List<ScreenTimeEntity>>

    @Query("SELECT * FROM screen_time WHERE userId = :userId ORDER BY date ASC")
    suspend fun getScreenTime(userId: String): List<ScreenTimeEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScreenTime(entries: List<ScreenTimeEntity>)

    @Query("DELETE FROM screen_time WHERE userId = :userId")
    suspend fun deleteScreenTime(userId: String)
}