package com.itm.profile_sdk.core

/**
 * Persists the current userId across process restarts, so [ISDKClient.setup] can
 * automatically restore it without the app having to call [ISDKClient.initialize]
 * again on every launch. Cleared by [ISDKClient.logout].
 */
internal expect fun persistUserId(context: Any, userId: String?)

internal expect fun readPersistedUserId(context: Any): String?
