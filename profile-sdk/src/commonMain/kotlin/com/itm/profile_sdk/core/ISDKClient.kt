package com.itm.profile_sdk.core

import com.itm.profile_sdk.Implementation.UserProfileApiServiceImpl
import com.itm.profile_sdk.auth.TokenManager
import com.itm.profile_sdk.local.buildDatabase
import com.itm.profile_sdk.models.NearbyUser
import com.itm.profile_sdk.models.ProfileViewsData
import com.itm.profile_sdk.models.ScreenTimeEntry
import com.itm.profile_sdk.models.ScreenTimeRequest
import com.itm.profile_sdk.models.Subscription
import com.itm.profile_sdk.models.UpdateProfileRequest
import com.itm.profile_sdk.models.UpsertProfileRequest
import com.itm.profile_sdk.models.UserProfile
import com.itm.profile_sdk.models.UserProfileData
import com.itm.profile_sdk.network.ApiConstants
import com.itm.profile_sdk.network.HttpClientFactory
import com.itm.profile_sdk.repository.UserProfileRepositoryImpl
import com.itm.profile_sdk.util.Cancellable
import com.itm.profile_sdk.util.Result
import kotlinx.coroutines.launch

object ISDKClient {

    /**
     * Must be called once before using any SDK function.
     *
     * @param userId      Authenticated user's UUID
     * @param sandboxMode true → sandbox API base URL, false → production API base URL. Defaults to true.
     * @param context     Android: Application context. iOS: omit or pass Unit.
     *
     * Android: ISDKClient.initialize(userId = "abc-123", sandboxMode = true, context = applicationContext)
     * iOS:     ISDKClient.initialize(userId: "abc-123", sandboxMode: true)
     */
    fun initialize(
        userId: String,
        sandboxMode: Boolean = true,
        context: Any = Unit
    ) {
        require(userId.isNotBlank()) { "userId must not be blank." }

        // Clear previous user's cached token before switching
        SDKState.tokenManager?.clear()

        val baseUrl = if (sandboxMode) ApiConstants.BASE_URL_SANDBOX else ApiConstants.BASE_URL_PRODUCTION

        val tokenManager = TokenManager()
        val db = buildDatabase(context)
        val httpClient = HttpClientFactory.create(baseUrl)
        val apiService = UserProfileApiServiceImpl(httpClient, baseUrl)

        SDKState.userId = userId
        SDKState.tokenManager = tokenManager
        SDKState.repository = UserProfileRepositoryImpl(apiService, db)
    }

    // ── Token ─────────────────────────────────────────────────────────────────

    /**
     * Generates a Bearer token using the X-Internal-Key.
     * Intended for sample apps — call once on startup.
     * Token is cached internally and auto-renewed using refreshToken when expired.
     * The returned idToken can be passed directly to any SDK function.
     *
     * @param internalKey  X-Internal-Key provided by the team
     * @param onResult     Returns idToken on success
     */
    fun generateToken(
        internalKey: String,
        onResult: (Result<String>) -> Unit
    ) {
        val userId = SDKState.requireUserId()
        SDKState.scope.launch {
            try {
                if (SDKState.tokenManager != null) {
                    val token = SDKState.tokenManager!!
                        .generateToken(uid = userId, internalKey = internalKey)
                    onResult(Result.Success(token))
                } else {
                    onResult(Result.Error("Token generation failed"))
                }
            } catch (e: Exception) {
                onResult(Result.Error(e.message ?: "Token generation failed", e))
            }
        }
    }
    // ── Profile ───────────────────────────────────────────────────────────────

    /**
     * Observe profile — emits cached data immediately, re-emits on API refresh.
     * Token is auto-renewed internally if expired.
     * @return Cancellable — call .cancel() to stop observing.
     */
    fun observeProfile(
        token: String,
        onEach: (UserProfile) -> Unit,
        onError: (Throwable) -> Unit = {}
    ): Cancellable {
        // 1. Collect DB flow — emits cached data instantly if available
        val collectJob = SDKState.scope.launch {
            try {
                SDKState.requireRepository()
                    .observeUserProfile(token, SDKState.requireUserId())
                    .collect { onEach(it) }
            } catch (e: Exception) {
                onError(e)
            }
        }

        // 2. Trigger refresh using the existing public function
        refreshProfile(token = token, onResult = { result ->
            if (result is Result.Error) onError(Throwable(result.message))
        })

        return Cancellable { collectJob.cancel() }
    }

    fun observeProfile(
        userId : String,
        token: String,
        onEach: (UserProfile) -> Unit,
        onError: (Throwable) -> Unit = {}
    ): Cancellable {
        // 1. Collect DB flow — emits cached data instantly if available
        val collectJob = SDKState.scope.launch {
            try {
                SDKState.requireRepository()
                    .observeUserProfile(token, userId)
                    .collect { onEach(it) }
            } catch (e: Exception) {
                onError(e)
            }
        }

        // 2. Trigger refresh using the existing public function
        refreshProfile(userId = userId, token = token, onResult = { result ->
            if (result is Result.Error) onError(Throwable(result.message))
        })

        return Cancellable { collectJob.cancel() }
    }

    /** First-login profile upsert — idempotent merge. */
    fun upsertProfile(
        token: String,
        request: UpsertProfileRequest,
        onResult: (Result<UserProfile>) -> Unit
    ) {
        SDKState.scope.launch {
            onResult(
                SDKState.requireRepository().upsertProfile(token, SDKState.requireUserId(), request)
            )
        }
    }

    /**
     * Partial profile update.
     * Subscription and ProfileViews cannot be updated through this SDK.
     */
    fun updateProfile(
        token: String,
        request: UpdateProfileRequest,
        onResult: (Result<UserProfile>) -> Unit
    ) {
        SDKState.scope.launch {
            onResult(
                SDKState.requireRepository().updateProfile(token, SDKState.requireUserId(), request)
            )
        }
    }


    fun updateProfile(
        userId : String,
        token: String,
        request: UpdateProfileRequest,
        onResult: (Result<UserProfile>) -> Unit
    ) {
        SDKState.scope.launch {
            onResult(
                SDKState.requireRepository().updateProfile(token, userId, request)
            )
        }
    }


    /**
     * Full profile snapshot (profile + subscription + screenTimeWeek + profileViews).
     * Subscription and profileViews are always live from API. The profile and screenTimeWeek
     * portions are also saved to the local DB, so a subsequent observeProfile() emission
     * will reflect this fresh data.
     */
    fun getProfileCompleteData(
        token: String,
        onResult: (Result<UserProfileData>) -> Unit
    ) {
        SDKState.scope.launch {
            onResult(SDKState.requireRepository().getProfileData(token, SDKState.requireUserId()))
        }
    }

    fun getProfileCompleteData(
        userId: String,
        token: String,
        onResult: (Result<UserProfileData>) -> Unit
    ) {
        SDKState.scope.launch {
            onResult(SDKState.requireRepository().getProfileData(token, userId))
        }
    }

    /** Force refresh profile from API — useful for pull-to-refresh. */
    private fun refreshProfile(
        token: String,
        onResult: (Result<Unit>) -> Unit
    ) {
        SDKState.scope.launch {
            onResult(SDKState.requireRepository().refreshProfile(token, SDKState.requireUserId()))
        }
    }

    private fun refreshProfile(
        userId : String,
        token: String,
        onResult: (Result<Unit>) -> Unit
    ) {
        SDKState.scope.launch {
            onResult(SDKState.requireRepository().refreshProfile(token, userId))
        }
    }

    // ── Subscription ──────────────────────────────────────────────────────────

    /** Always fetched live from API — never cached locally. */
    fun getSubscription(
        token: String,
        onResult: (Result<Subscription>) -> Unit
    ) {
        SDKState.scope.launch {
            onResult(SDKState.requireRepository().getSubscription(token, SDKState.requireUserId()))
        }
    }

    // ── Profile Views ─────────────────────────────────────────────────────────

    /** Owner-only paginated list of profile viewers. Always live from API. */
    fun getProfileViews(
        token: String,
        cursor: String? = null,
        limit: Int? = null,
        onResult: (Result<ProfileViewsData>) -> Unit
    ) {
        SDKState.scope.launch {
            onResult(
                SDKState.requireRepository()
                    .getProfileViews(token, SDKState.requireUserId(), cursor, limit)
            )
        }
    }

    // ── Screen Time ───────────────────────────────────────────────────────────

    /**
     * Observe screen-time — emits cached data immediately, re-emits on API refresh.
     * @return Cancellable — call .cancel() to stop observing.
     */
    fun observeScreenTime(
        token: String,
        days: Int,
        onEach: (List<ScreenTimeEntry>) -> Unit,
        onError: (Throwable) -> Unit = {},
        onComplete: () -> Unit = {}
    ): Cancellable {
        val job = SDKState.scope.launch {
            try {
                SDKState.requireRepository()
                    .observeScreenTime(token, SDKState.requireUserId(), days)
                    .collect { onEach(it.entries) }
                onComplete()
            } catch (e: Exception) {
                onError(e)
            }
        }
        return Cancellable { job.cancel() }
    }

    /** Append screen-time seconds for a specific date (server increments). */
    fun postScreenTime(
        token: String,
        request: ScreenTimeRequest,
        days : Int = 7,
        onResult: (Result<Unit>) -> Unit
    ) {
        SDKState.scope.launch {
            onResult(
                SDKState.requireRepository()
                    .postScreenTime(token, SDKState.requireUserId(), days,request)
            )
        }
    }

    // ── Nearby ────────────────────────────────────────────────────────────────

    /** Nearby public users — lat/lng optional, falls back to saved location. */
    fun getNearbyUsers(
        token: String,
        lat: Double? = null,
        lng: Double? = null,
        onResult: (Result<List<NearbyUser>>) -> Unit
    ) {
        SDKState.scope.launch {
            onResult(SDKState.requireRepository().getNearbyUsers(token, lat, lng))
        }
    }
}