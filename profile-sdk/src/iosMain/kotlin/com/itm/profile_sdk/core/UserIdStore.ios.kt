package com.itm.profile_sdk.core

import platform.Foundation.NSUserDefaults

private const val KEY_USER_ID = "itm_profile_sdk_user_id"

internal actual fun persistUserId(context: Any, userId: String?) {
    // context is unused on iOS — NSUserDefaults is process/app-wide
    val defaults = NSUserDefaults.standardUserDefaults
    if (userId == null) {
        defaults.removeObjectForKey(KEY_USER_ID)
    } else {
        defaults.setObject(userId, KEY_USER_ID)
    }
}

internal actual fun readPersistedUserId(context: Any): String? =
    NSUserDefaults.standardUserDefaults.stringForKey(KEY_USER_ID)
