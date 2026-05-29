package com.itm.profile_sdk.local.entity

import androidx.room.Entity

@Entity(
    tableName = "screen_time",
    primaryKeys = ["userId", "date"]
)
data class ScreenTimeEntity(
    val userId: String,
    val date: String,
    val minutes: Int? = null
)