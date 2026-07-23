package com.itm.profile_sdk.core

import android.content.Context

private const val PREFS_NAME = "itm_profile_sdk_prefs"
private const val KEY_USER_ID = "itm_profile_sdk_user_id"

internal actual fun persistUserId(context: Any, userId: String?) {
    require(context is Context) {
        "On Android, pass Application context to ISDKClient.setup()"
    }
    val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    prefs.edit().apply {
        if (userId == null) remove(KEY_USER_ID) else putString(KEY_USER_ID, userId)
    }.apply()
}

internal actual fun readPersistedUserId(context: Any): String? {
    require(context is Context) {
        "On Android, pass Application context to ISDKClient.setup()"
    }
    val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    return prefs.getString(KEY_USER_ID, null)
}
