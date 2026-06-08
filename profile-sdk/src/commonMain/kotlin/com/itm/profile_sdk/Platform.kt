package com.itm.profile_sdk

expect fun platform(): String
internal expect fun getSystemTimeMillis(): Long
internal expect fun getCurrentDate(): String
internal expect fun isWithinDays(date: String?, days: Int): Boolean