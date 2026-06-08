package com.itm.profile_sdk

import platform.Foundation.NSDate
import platform.Foundation.NSDateFormatter
import platform.Foundation.NSLocale
import platform.Foundation.currentLocale
import platform.Foundation.dateWithTimeIntervalSinceNow
import platform.Foundation.timeIntervalSince1970

actual fun platform() = "iOS"
internal actual fun getSystemTimeMillis(): Long =
    (NSDate().timeIntervalSince1970 * 1000).toLong()

private val formatter = NSDateFormatter().apply {
    dateFormat = "yyyy-MM-dd"
    locale     = NSLocale.currentLocale
}

internal actual fun getCurrentDate(): String = formatter.stringFromDate(NSDate())

internal actual fun isWithinDays(date: String?, days: Int): Boolean {
    if (date == null) return false
    return try {
        val parsed  = formatter.dateFromString(date) ?: return false
        val cutoff  = NSDate.dateWithTimeIntervalSinceNow(-(days * 24 * 60 * 60).toDouble())
        parsed.timeIntervalSince1970 >= cutoff.timeIntervalSince1970
    } catch (e: Exception) { false }
}