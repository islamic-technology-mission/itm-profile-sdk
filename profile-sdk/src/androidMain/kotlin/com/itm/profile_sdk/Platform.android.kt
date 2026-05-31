package com.itm.profile_sdk

actual fun platform() = "Android"
internal actual fun getSystemTimeMillis(): Long = System.currentTimeMillis()
