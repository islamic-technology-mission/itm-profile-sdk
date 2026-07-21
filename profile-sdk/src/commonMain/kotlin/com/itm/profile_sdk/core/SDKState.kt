package com.itm.profile_sdk.core

import com.itm.profile_sdk.auth.TokenManager
import com.itm.profile_sdk.repository.UserProfileRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO

internal object SDKState {
    var userId: String? = null
    var repository: UserProfileRepository? = null
    var tokenManager: TokenManager? = null
    var appContext: Any? = null
    val scope = CoroutineScope(Dispatchers.IO)  // ← add this

    fun requireUserId(): String =
        userId ?: error("No current user set. Call ISDKClient.initialize(userId) first, or use the overload that takes an explicit userId.")

    fun requireRepository(): UserProfileRepository =
        repository ?: error("ISDKClient not set up. Call ISDKClient.setup(...) once at app startup.")
}