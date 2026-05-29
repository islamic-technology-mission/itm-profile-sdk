package com.itm.profile_sdk.repository

import com.itm.profile_sdk.local.AppDatabase
import com.itm.profile_sdk.local.entity.UserProfileEntity
import com.itm.profile_sdk.local.mapper.toDomain
import com.itm.profile_sdk.local.mapper.toEntity
import com.itm.profile_sdk.models.NearbyUser
import com.itm.profile_sdk.models.ProfileViewsData
import com.itm.profile_sdk.models.ScreenTimeEntry
import com.itm.profile_sdk.models.ScreenTimeRequest
import com.itm.profile_sdk.models.Subscription
import com.itm.profile_sdk.models.UpdateProfileRequest
import com.itm.profile_sdk.models.UpsertProfileRequest
import com.itm.profile_sdk.models.UserProfile
import com.itm.profile_sdk.models.UserProfileData
import com.itm.profile_sdk.network.UserProfileApiService
import com.itm.profile_sdk.util.Result
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.transform
import kotlinx.coroutines.launch


interface UserProfileRepository {

    // ── Profile ───────────────────────────────────────────────────────────────

    /** Cache-first: emits DB instantly, background API refresh auto-updates the flow */
    fun observeUserProfile(userId: String): Flow<UserProfile>

    /** POST — first-login upsert (idempotent merge) */
    suspend fun upsertProfile(userId: String, request: UpsertProfileRequest): Result<UserProfile>

    /** PATCH — partial profile update (excludes ProfileViews & Subscription) */
    suspend fun updateProfile(userId: String, request: UpdateProfileRequest): Result<UserProfile>

    /** Force pull from API — useful for pull-to-refresh */
    suspend fun refreshProfile(userId: String): Result<Unit>

    // ── Subscription (always live) ────────────────────────────────────────────

    /** Always fetched live from API — never stored in DB */
    suspend fun getSubscription(userId: String): Result<Subscription>

    // ── Profile Views (always live) ───────────────────────────────────────────

    /** Owner-only paginated list of profile viewers — never stored in DB */
    suspend fun getProfileViews(
        userId: String,
        cursor: String? = null,
        limit: Int? = null
    ): Result<ProfileViewsData>

    // ── Screen Time ───────────────────────────────────────────────────────────

    /** Cache-first: emits DB instantly, background API refresh auto-updates the flow */
    fun observeScreenTime(userId: String): Flow<List<ScreenTimeEntry>>

    /** POST — append screen-time seconds for a date (server increments) */
    suspend fun postScreenTime(userId: String, request: ScreenTimeRequest): Result<Unit>

    /** Force refresh screen-time from API */
    suspend fun refreshScreenTime(userId: String): Result<Unit>

    // ── Nearby ────────────────────────────────────────────────────────────────

    /** Always live — lat/lng optional, falls back to saved location on server */
    suspend fun getNearbyUsers(lat: Double? = null, lng: Double? = null): Result<List<NearbyUser>>
}

class UserProfileRepositoryImpl internal constructor(
    private val apiService: UserProfileApiService,
    private val db: AppDatabase,
    private val externalScope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) : UserProfileRepository {

    private val profileDao    = db.userProfileDao()
    private val screenTimeDao = db.screenTimeDao()

    // ── Profile ───────────────────────────────────────────────────────────────

    override fun observeUserProfile(userId: String): Flow<UserProfile> {
        externalScope.launch { refreshProfile(userId) }
        return profileDao
            .observeProfile(userId)
            .mapNotNull { entity: UserProfileEntity? -> entity?.toDomain() }
    }

    override suspend fun upsertProfile(
        userId: String,
        request: UpsertProfileRequest
    ): Result<UserProfile> {
        return try {
            val response = apiService.upsertProfile(userId, request)
            if (response.status == "success") {
                val profile = response.data?.profile
                    ?: return Result.Error("Profile data missing in response")
                profileDao.insertProfile(profile.toEntity())
                Result.Success(profile)
            } else {
                Result.Error("API error: ${response.status}")
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Upsert failed", e)
        }
    }

    override suspend fun updateProfile(
        userId: String,
        request: UpdateProfileRequest
    ): Result<UserProfile> {
        return try {
            // 1. Optimistically apply to DB
            applyOptimisticUpdate(userId, request)

            // 2. Sync to API
            val response = apiService.updateProfile(userId, request)
            if (response.status == "success") {
                val profile = response.data?.profile
                    ?: return Result.Error("Profile data missing in response")
                // 3. Persist authoritative server response
                profileDao.insertProfile(profile.toEntity())
                Result.Success(profile)
            } else {
                Result.Error("API error: ${response.status}")
            }
        } catch (e: Exception) {
            // Rollback optimistic update
            refreshProfile(userId)
            Result.Error(e.message ?: "Update failed", e)
        }
    }

    override suspend fun refreshProfile(userId: String): Result<Unit> {
        return try {
            val response = apiService.getProfile(userId)
            if (response.status == "success") {
                response.data?.let { saveProfileToDb(userId, it) }
                Result.Success(Unit)
            } else {
                Result.Error("API error: ${response.status}")
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Refresh failed", e)
        }
    }

    // ── Subscription ──────────────────────────────────────────────────────────

    override suspend fun getSubscription(userId: String): Result<Subscription> {
        return try {
            val response = apiService.getProfile(userId)
            if (response.status == "success") {
                val subscription = response.data?.subscription
                    ?: return Result.Error("Subscription data unavailable")
                Result.Success(subscription)
            } else {
                Result.Error("API error: ${response.status}")
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Failed to fetch subscription", e)
        }
    }

    // ── Profile Views ─────────────────────────────────────────────────────────

    override suspend fun getProfileViews(
        userId: String,
        cursor: String?,
        limit: Int?
    ): Result<ProfileViewsData> {
        return try {
            val response = apiService.getProfileViews(userId, cursor, limit)
            if (response.status == "success") {
                val data = response.data
                    ?: return Result.Error("Profile views data unavailable")
                Result.Success(data)
            } else {
                Result.Error("API error: ${response.status}")
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Failed to fetch profile views", e)
        }
    }

    // ── Screen Time ───────────────────────────────────────────────────────────

    override fun observeScreenTime(userId: String): Flow<List<ScreenTimeEntry>> {
        externalScope.launch { refreshScreenTime(userId) }
        return screenTimeDao
            .observeScreenTime(userId)
            .mapNotNull { entities -> entities.map { it.toDomain() } }
    }

    override suspend fun postScreenTime(
        userId: String,
        request: ScreenTimeRequest
    ): Result<Unit> {
        return try {
            apiService.postScreenTime(userId, request)
            // Refresh local DB after posting so cache stays in sync
            refreshScreenTime(userId)
        } catch (e: Exception) {
            Result.Error(e.message ?: "Failed to post screen time", e)
        }
    }

    override suspend fun refreshScreenTime(userId: String): Result<Unit> {
        return try {
            val response = apiService.getScreenTime(userId)
            if (response.status == "success") {
                response.data?.let { entries ->
                    screenTimeDao.deleteScreenTime(userId)
                    screenTimeDao.insertScreenTime(entries.map { it.toEntity(userId) })
                }
                Result.Success(Unit)
            } else {
                Result.Error("API error: ${response.status}")
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Failed to refresh screen time", e)
        }
    }

    // ── Nearby ────────────────────────────────────────────────────────────────

    override suspend fun getNearbyUsers(
        lat: Double?,
        lng: Double?
    ): Result<List<NearbyUser>> {
        return try {
            val response = apiService.getNearbyUsers(lat, lng)
            if (response.status == "success") {
                Result.Success(response.data ?: emptyList())
            } else {
                Result.Error("API error: ${response.status}")
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Failed to fetch nearby users", e)
        }
    }

    // ── Private Helpers ───────────────────────────────────────────────────────

    private suspend fun saveProfileToDb(userId: String, data: UserProfileData) {
        data.profile?.let { profileDao.insertProfile(it.toEntity()) }
        data.screenTimeWeek?.let { entries ->
            screenTimeDao.deleteScreenTime(userId)
            screenTimeDao.insertScreenTime(entries.map { it.toEntity(userId) })
        }
        // Subscription → always live, not persisted
        // ProfileViews  → always live, not persisted
    }

    private suspend fun applyOptimisticUpdate(userId: String, request: UpdateProfileRequest) {
        profileDao.updateProfile(
            userId            = userId,
            name              = request.name,
            phone             = request.phone,
            gender            = request.gender,
            dob               = request.dob,
            visibility        = request.visibility,
            umrahOptIn        = request.umrahOptIn,
            locationLat       = request.location?.lat,
            locationLng       = request.location?.lng,
            locationGeohash   = request.location?.geohash,
            locationUpdatedAt = request.location?.updatedAt
        )
    }
}