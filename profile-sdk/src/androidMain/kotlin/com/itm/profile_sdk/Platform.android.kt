package com.itm.profile_sdk

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

actual fun platform() = "Android"
internal actual fun getSystemTimeMillis(): Long = System.currentTimeMillis()
private val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

internal actual fun getCurrentDate(): String = sdf.format(Date())

internal actual fun isWithinDays(date: String?, days: Int): Boolean {
    if (date == null) return false
    return try {
        val parsed   = sdf.parse(date) ?: return false
        val cutoff   = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -days) }.time
        parsed.after(cutoff) || parsed == cutoff
    } catch (e: Exception) { false }
}