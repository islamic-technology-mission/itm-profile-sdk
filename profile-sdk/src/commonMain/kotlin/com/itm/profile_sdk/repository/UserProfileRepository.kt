package com.itm.profile_sdk.repository

import com.itm.profile_sdk.local.AppDatabase
import com.itm.profile_sdk.local.entity.UserProfileEntity
import com.itm.profile_sdk.local.mapper.toDomain
import com.itm.profile_sdk.local.mapper.toEntity
import com.itm.profile_sdk.models.Subscription
import com.itm.profile_sdk.models.UserProfile
import com.itm.profile_sdk.models.UserProfileData
import com.itm.profile_sdk.network.UserProfileApiService
import com.itm.profile_sdk.util.Result
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch


interface UserProfileRepository {
    /**
     * Emits cached profile immediately from DB.
     * Simultaneously fires an API request in background to refresh DB.
     * ProfileViews and Subscription are NOT included — fetch them separately.
     */
    fun observeUserProfile(userId: String): Flow<UserProfile>

    /**
     * Subscription is always fetched live from API — never stored in DB.
     */
    suspend fun getSubscription(userId: String): Result<Subscription>

    /**
     * Update allowed profile fields both locally and on the API.
     * ProfileViews and Subscription are excluded.
     */
    suspend fun updateProfile(userId: String, updatedProfile: UserProfile): Result<Unit>

    /**
     * Force refresh from API — useful for pull-to-refresh.
     */
    suspend fun refreshProfile(userId: String): Result<Unit>
}

class UserProfileRepositoryImpl(
    private val apiService: UserProfileApiService,
    private val db: AppDatabase,
    private val externalScope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) : UserProfileRepository {

    private val profileDao = db.userProfileDao()
    private val screenTimeDao = db.screenTimeDao()

    // ─── Observe Profile (Cache-First) ───────────────────────────────────────

    override fun observeUserProfile(userId: String): Flow<UserProfile> {
        // Kick off background API refresh immediately
        externalScope.launch { refreshProfile(userId) }

        // Return DB Flow — emits cached data instantly, re-emits after API update
        return profileDao
            .observeProfile(userId)
            .mapNotNull { entity: UserProfileEntity -> entity.toDomain() }
    }

    // ─── Subscription (Always Live) ──────────────────────────────────────────

    override suspend fun getSubscription(userId: String): Result<Subscription> {
        return try {
            val response = apiService.getUserProfile(userId)
            if (response.status == "success") {
                val subscription = response.data?.subscription
                    ?: return Result.Error("Subscription data unavailable")
                Result.Success(subscription)
            } else {
                Result.Error("API returned non-success status: ${response.status}")
            }
        } catch (e: Exception) {
            Result.Error(message = e.message ?: "Unknown error", cause = e)
        }
    }

    // ─── Force Refresh ───────────────────────────────────────────────────────

    override suspend fun refreshProfile(userId: String): Result<Unit> {
        return try {
            val response = apiService.getUserProfile(userId)
            if (response.status == "success") {
                response.data?.let { saveToDb(userId, it) }
                Result.Success(Unit)
            } else {
                Result.Error("API returned non-success status: ${response.status}")
            }
        } catch (e: Exception) {
            Result.Error(message = e.message ?: "Unknown error", cause = e)
        }
    }

    // ─── Update Profile ───────────────────────────────────────────────────────

    override suspend fun updateProfile(userId: String, updatedProfile: UserProfile): Result<Unit> {
        return try {
            // 1. Optimistically update local DB first
            updateLocalProfile(userId, updatedProfile)

            // 2. Sync to API
            apiService.updateUserProfile(userId, updatedProfile)

            Result.Success(Unit)
        } catch (e: Exception) {
            // 3. On failure — rollback by re-fetching from API
            refreshProfile(userId)
            Result.Error(message = e.message ?: "Update failed", cause = e)
        }
    }

    // ─── Private Helpers ─────────────────────────────────────────────────────

    private suspend fun saveToDb(userId: String, data: UserProfileData) {
        // Save profile
        data.profile?.let { profileDao.insertProfile(it.toEntity()) }

        // Save screen time (replace weekly entries)
        data.screenTimeWeek?.let { entries ->
            screenTimeDao.deleteScreenTime(userId)
            screenTimeDao.insertScreenTime(entries.map { it.toEntity(userId) })
        }

        // Subscription → NOT saved (always live from API)
        // ProfileViews  → NOT saved (always live from API)
    }

    private suspend fun updateLocalProfile(userId: String, profile: UserProfile) {
        profileDao.updateProfile(
            userId = userId,
            name = profile.name,
            phone = profile.phone,
            gender = profile.gender,
            dob = profile.dob,
            visibility = profile.visibility,
            umrahOptIn = profile.umrahOptIn,
            locationLat = profile.location?.lat,
            locationLng = profile.location?.lng,
            locationGeohash = profile.location?.geohash,
            locationUpdatedAt = profile.location?.updatedAt
        )
    }
}