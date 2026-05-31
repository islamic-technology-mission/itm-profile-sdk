package com.itm.profile_sdk

import platform.Foundation.NSDate
import platform.Foundation.timeIntervalSince1970

actual fun platform() = "iOS"
internal actual fun getSystemTimeMillis(): Long =
    (NSDate().timeIntervalSince1970 * 1000).toLong()