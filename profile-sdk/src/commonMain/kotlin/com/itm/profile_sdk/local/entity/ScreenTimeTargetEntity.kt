package com.itm.profile_sdk.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "screen_time_target")
data class ScreenTimeTargetEntity(
    @PrimaryKey
    val userId: String,
    val dailyTargetMinutes: Int = 15  // default 15 mins
)