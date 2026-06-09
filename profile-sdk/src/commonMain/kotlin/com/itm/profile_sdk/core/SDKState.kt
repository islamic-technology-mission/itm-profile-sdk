package com.itm.profile_sdk.core

import com.itm.profile_sdk.auth.TokenManager
import com.itm.profile_sdk.repository.UserProfileRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.SupervisorJob

internal object SDKState {
    var userId: String? = null
    var repository: UserProfileRepository? = null
    var tokenManager: TokenManager? = null
    // SupervisorJob: a failing child coroutine does not cancel sibling coroutines or the scope.
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun requireUserId(): String =
        userId ?: error("ISDKClient not initialized. Call ISDKClient.initialize() first.")

    fun requireRepository(): UserProfileRepository =
        repository ?: error("ISDKClient not initialized. Call ISDKClient.initialize() first.")
}