package com.itm.profile_sdk.models

data class ScreenTimeProgress(
    val targetMinutes: Int,

    // Daily
    val todayMinutes: Int,
    val isDailyTargetMet: Boolean,
    val dailyProgressPercent: Float,    // 0.0 - 100.0

    // Weekly (last 7 days)
    val weeklyMinutes: Int,
    val weeklyTargetMinutes: Int,       // targetMinutes * 7
    val isWeeklyTargetMet: Boolean,
    val weeklyProgressPercent: Float,

    // Monthly (last 30 days)
    val monthlyMinutes: Int,
    val monthlyTargetMinutes: Int,      // targetMinutes * 30
    val isMonthlyTargetMet: Boolean,
    val monthlyProgressPercent: Float,

    // Raw entries
    val entries: List<ScreenTimeEntry>
)