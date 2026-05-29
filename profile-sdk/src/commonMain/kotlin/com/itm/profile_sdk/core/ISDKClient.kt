package com.itm.profile_sdk.core

import com.itm.profile_sdk.Implementation.UserProfileApiServiceImpl
import com.itm.profile_sdk.local.buildDatabase
import com.itm.profile_sdk.models.NearbyUser
import com.itm.profile_sdk.models.ProfileViewsData
import com.itm.profile_sdk.models.ScreenTimeEntry
import com.itm.profile_sdk.models.ScreenTimeRequest
import com.itm.profile_sdk.models.Subscription
import com.itm.profile_sdk.models.UpdateProfileRequest
import com.itm.profile_sdk.models.UpsertProfileRequest
import com.itm.profile_sdk.models.UserProfile
import com.itm.profile_sdk.network.HttpClientFactory
import com.itm.profile_sdk.repository.UserProfileRepositoryImpl
import com.itm.profile_sdk.util.Cancellable
import com.itm.profile_sdk.util.Result
import kotlinx.coroutines.launch

object ISDKClient {

    /**
     * Must be called once before using any SDK function.
     *
     * @param userId  Authenticated user's UUID
     * @param context Android: pass Application context. iOS: pass Unit or omit.
     *
     * Android: ISDKClient.initialize(userId = "abc-123", context = applicationContext)
     * iOS:     ISDKClient.initialize(userId: "abc-123")
     */
    fun initialize(userId: String, context: Any = Unit) {
        require(userId.isNotBlank()) { "userId must not be blank." }

        val db = buildDatabase(context)
        val httpClient = HttpClientFactory.create()
        val apiService = UserProfileApiServiceImpl(httpClient)

        SDKState.userId = userId
        SDKState.repository = UserProfileRepositoryImpl(apiService, db)
    }

    // ── Profile ───────────────────────────────────────────────────────────────

    /**
     * Observe profile with callbacks — safe for both Android and iOS.
     * Emits cached profile immediately, then re-emits on every API refresh.
     *
     * @return Cancellable — call .cancel() to stop observing.
     *
     * iOS usage:
     *   val job = ISDKClient.observeProfile(
     *       onEach    = { profile -> },
     *       onError   = { error -> },
     *       onComplete = { }
     *   )
     *   job.cancel() // when done
     */
    fun observeProfile(
        onEach: (UserProfile) -> Unit,
        onError: (Throwable) -> Unit = {},
        onComplete: () -> Unit = {}
    ): Cancellable {
        val job = SDKState.scope.launch {
            try {
                SDKState.requireRepository()
                    .observeUserProfile(SDKState.requireUserId())
                    .collect { onEach(it) }
                onComplete()
            } catch (e: Exception) {
                onError(e)
            }
        }
        return Cancellable { job.cancel() }
    }

    /**
     * First-login profile upsert — idempotent merge.
     */
    fun upsertProfile(
        request: UpsertProfileRequest,
        onResult: (Result<UserProfile>) -> Unit
    ) {
        SDKState.scope.launch {
            onResult(
                SDKState.requireRepository()
                    .upsertProfile(SDKState.requireUserId(), request)
            )
        }
    }

    /**
     * Partial profile update.
     * Subscription and ProfileViews cannot be updated through this SDK.
     */
    fun updateProfile(
        request: UpdateProfileRequest,
        onResult: (Result<UserProfile>) -> Unit
    ) {
        SDKState.scope.launch {
            onResult(
                SDKState.requireRepository()
                    .updateProfile(SDKState.requireUserId(), request)
            )
        }
    }

    /**
     * Force refresh profile from API — useful for pull-to-refresh.
     */
    fun refreshProfile(onResult: (Result<Unit>) -> Unit) {
        SDKState.scope.launch {
            onResult(
                SDKState.requireRepository()
                    .refreshProfile(SDKState.requireUserId())
            )
        }
    }

    // ── Subscription ──────────────────────────────────────────────────────────

    /**
     * Always fetched live from API — never cached locally.
     */
    fun getSubscription(onResult: (Result<Subscription>) -> Unit) {
        SDKState.scope.launch {
            onResult(
                SDKState.requireRepository()
                    .getSubscription(SDKState.requireUserId())
            )
        }
    }

    // ── Profile Views ─────────────────────────────────────────────────────────

    /**
     * Owner-only paginated list of profile viewers.
     * Always fetched live from API — never cached locally.
     */
    fun getProfileViews(
        cursor: String? = null,
        limit: Int? = null,
        onResult: (Result<ProfileViewsData>) -> Unit
    ) {
        SDKState.scope.launch {
            onResult(
                SDKState.requireRepository()
                    .getProfileViews(SDKState.requireUserId(), cursor, limit)
            )
        }
    }

    // ── Screen Time ───────────────────────────────────────────────────────────

    /**
     * Observe screen-time with callbacks — safe for both Android and iOS.
     * Emits cached data immediately, then re-emits on every API refresh.
     *
     * @return Cancellable — call .cancel() to stop observing.
     */
    fun observeScreenTime(
        onEach: (List<ScreenTimeEntry>) -> Unit,
        onError: (Throwable) -> Unit = {},
        onComplete: () -> Unit = {}
    ): Cancellable {
        val job = SDKState.scope.launch {
            try {
                SDKState.requireRepository()
                    .observeScreenTime(SDKState.requireUserId())
                    .collect { onEach(it) }
                onComplete()
            } catch (e: Exception) {
                onError(e)
            }
        }
        return Cancellable { job.cancel() }
    }

    /**
     * Append screen-time seconds for a specific date (server increments).
     */
    fun postScreenTime(
        request: ScreenTimeRequest,
        onResult: (Result<Unit>) -> Unit
    ) {
        SDKState.scope.launch {
            onResult(
                SDKState.requireRepository()
                    .postScreenTime(SDKState.requireUserId(), request)
            )
        }
    }

    // ── Nearby ────────────────────────────────────────────────────────────────

    /**
     * Fetch nearby public users.
     * lat/lng are optional — server falls back to the user's saved location.
     */
    fun getNearbyUsers(
        lat: Double? = null,
        lng: Double? = null,
        onResult: (Result<List<NearbyUser>>) -> Unit
    ) {
        SDKState.scope.launch {
            onResult(
                SDKState.requireRepository()
                    .getNearbyUsers(lat, lng)
            )
        }
    }
}