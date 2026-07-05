package com.itm.profile_sdk.repository

import com.itm.profile_sdk.getCurrentDate
import com.itm.profile_sdk.isWithinDays
import com.itm.profile_sdk.local.AppDatabase
import com.itm.profile_sdk.local.entity.UserProfileEntity
import com.itm.profile_sdk.local.mapper.toDomain
import com.itm.profile_sdk.local.mapper.toEntity
import com.itm.profile_sdk.models.NearbyUser
import com.itm.profile_sdk.models.ProfileViewsData
import com.itm.profile_sdk.models.ScreenTimeEntry
import com.itm.profile_sdk.models.ScreenTimeProgress
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
import kotlinx.coroutines.launch


internal interface UserProfileRepository {
    fun observeUserProfile(token: String, userId: String): Flow<UserProfile>
    suspend fun getProfileData(token: String, userId: String): Result<UserProfileData>
    suspend fun upsertProfile(
        token: String,
        userId: String,
        request: UpsertProfileRequest
    ): Result<UserProfile>

    suspend fun updateProfile(
        token: String,
        userId: String,
        request: UpdateProfileRequest
    ): Result<UserProfile>

    suspend fun refreshProfile(token: String, userId: String): Result<Unit>
    suspend fun getSubscription(token: String, userId: String): Result<Subscription>
    suspend fun getProfileViews(
        token: String,
        userId: String,
        cursor: String?,
        limit: Int?
    ): Result<ProfileViewsData>

    fun observeScreenTime(token: String, userId: String, days: Int): Flow<ScreenTimeProgress>
    suspend fun postScreenTime(
        token: String,
        userId: String,
        days: Int,
        request: ScreenTimeRequest
    ): Result<Unit>

    suspend fun getNearbyUsers(token: String, lat: Double?, lng: Double?): Result<List<NearbyUser>>
}

internal class UserProfileRepositoryImpl(
    private val apiService: UserProfileApiService,
    private val db: AppDatabase,
    private val externalScope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) : UserProfileRepository {

    private val profileDao = db.userProfileDao()
    private val screenTimeDao = db.screenTimeDao()
    private val screenTimeTargetDao = db.screenTimeTargetDao()

    // ── Profile ───────────────────────────────────────────────────────────────

    override fun observeUserProfile(token: String, userId: String): Flow<UserProfile> {
        externalScope.launch { refreshProfile(token, userId) }
        return profileDao
            .observeProfile(userId)
            .mapNotNull { entity: UserProfileEntity? -> entity?.toDomain() }
    }

    override suspend fun upsertProfile(
        token: String, userId: String, request: UpsertProfileRequest
    ): Result<UserProfile> {
        return try {
            val response = apiService.upsertProfile(token, userId, request)
            if (response.status == "success") {
                val profile = response.data?.profile ?: return Result.Error("Profile data missing")
                profileDao.insertProfile(profile.toEntity())
                Result.Success(profile)
            } else Result.Error("API error: ${response.status}")
        } catch (e: Exception) {
            Result.Error(e.message ?: "Upsert failed", e)
        }
    }

    override suspend fun updateProfile(
        token: String, userId: String, request: UpdateProfileRequest
    ): Result<UserProfile> {
        return try {
            applyOptimisticUpdate(userId, request)
            val response = apiService.updateProfile(token, userId, request)
            if (response.status == "success") {
                val profile = response.data?.profile ?: return Result.Error("Profile data missing")
                profileDao.insertProfile(profile.toEntity())
                Result.Success(profile)
            } else Result.Error("API error: ${response.status}")
        } catch (e: Exception) {
            refreshProfile(token, userId)
            Result.Error(e.message ?: "Update failed", e)
        }
    }

    override suspend fun refreshProfile(token: String, userId: String): Result<Unit> {
        return try {
            val response = apiService.getProfile(token, userId)
            if (response.status == "success") {
                response.data?.let { saveProfileToDb(userId, it) }
                Result.Success(Unit)
            } else Result.Error("API error: ${response.status}")
        } catch (e: Exception) {
            Result.Error(e.message ?: "Refresh failed", e)
        }
    }

    /**
     * Full profile snapshot (profile + subscription + screenTimeWeek + profileViews).
     * Subscription and profileViews are always live (never cached), but the profile and
     * screenTimeWeek portions are written to the local DB — so observeUserProfile picks up
     * the fresh data on its next emission.
     */
    override suspend fun getProfileData(token: String, userId: String): Result<UserProfileData> {
        return try {
            val response = apiService.getProfile(token, userId)
            if (response.status == "success") {
                val data = response.data ?: return Result.Error("Profile data unavailable")
                saveProfileToDb(userId, data)
                Result.Success(data)
            } else Result.Error("API error: ${response.status}")
        } catch (e: Exception) {
            Result.Error(e.message ?: "Failed to fetch profile data", e)
        }
    }

    // ── Subscription ──────────────────────────────────────────────────────────

    override suspend fun getSubscription(token: String, userId: String): Result<Subscription> {
        return try {
            val response = apiService.getProfile(token, userId)
            if (response.status == "success") {
                val subscription =
                    response.data?.subscription ?: return Result.Error("Subscription unavailable")
                Result.Success(subscription)
            } else Result.Error("API error: ${response.status}")
        } catch (e: Exception) {
            Result.Error(e.message ?: "Failed to fetch subscription", e)
        }
    }

    // ── Profile Views ─────────────────────────────────────────────────────────

    override suspend fun getProfileViews(
        token: String, userId: String, cursor: String?, limit: Int?
    ): Result<ProfileViewsData> {
        return try {
            val response = apiService.getProfileViews(token, userId, cursor, limit)
            if (response.status == "success") {
                val data = response.data ?: return Result.Error("Profile views data unavailable")
                Result.Success(data)
            } else Result.Error("API error: ${response.status}")
        } catch (e: Exception) {
            Result.Error(e.message ?: "Failed to fetch profile views", e)
        }
    }

    // ── Screen Time ───────────────────────────────────────────────────────────


    override fun observeScreenTime(
        token: String,
        userId: String,
        days: Int
    ): Flow<ScreenTimeProgress> {
        // Combine screen time entries + target into a single progress flow
        return screenTimeDao
            .observeScreenTime(userId)
            .mapNotNull { entities ->
                val entries = entities.map { it.toDomain() }
                val targetMinutes = screenTimeTargetDao.getTarget(userId)?.dailyTargetMinutes ?: 15
                calculateProgress(entries, targetMinutes)
            }
    }

    override suspend fun postScreenTime(
        token: String, userId: String, days: Int, request: ScreenTimeRequest
    ): Result<Unit> {
        return try {
            apiService.postScreenTime(token, userId, request)
            refreshScreenTime(token, userId, days)
        } catch (e: Exception) {
            Result.Error(e.message ?: "Failed to post screen time", e)
        }
    }

    private suspend fun refreshScreenTime(token: String, userId: String, days: Int): Result<Unit> {
        return try {
            val response = apiService.getScreenTime(token, userId, days)
            if (response.status == "success") {
                response.data?.screenTime?.let { entries ->
                    screenTimeDao.deleteScreenTime(userId)
                    screenTimeDao.insertScreenTime(entries.map { it.toEntity(userId) })
                }
                Result.Success(Unit)
            } else Result.Error("API error: ${response.status}")
        } catch (e: Exception) {
            Result.Error(e.message ?: "Failed to refresh screen time", e)
        }
    }

    // ── Nearby ────────────────────────────────────────────────────────────────

    override suspend fun getNearbyUsers(
        token: String, lat: Double?, lng: Double?
    ): Result<List<NearbyUser>> {
        return try {
            val response = apiService.getNearbyUsers(token, lat, lng)
            if (response.status == "success") {
                Result.Success(response.data?.nearbyUsers ?: emptyList())
            } else Result.Error("API error: ${response.status}")
        } catch (e: Exception) {
            Result.Error(e.message ?: "Failed to fetch nearby users", e)
        }
    }

    // ── Private Helpers ───────────────────────────────────────────────────────

    private fun calculateProgress(
        entries: List<ScreenTimeEntry>,
        targetMinutes: Int
    ): ScreenTimeProgress {
        val today = getCurrentDate()
        entries.mapNotNull { it.date }

        val todayMinutes = entries.filter { it.date == today }.sumOf { it.minutes ?: 0 }
        val weeklyMinutes = entries.filter { isWithinDays(it.date, 7) }.sumOf { it.minutes ?: 0 }
        val monthlyMinutes = entries.filter { isWithinDays(it.date, 30) }.sumOf { it.minutes ?: 0 }

        val weeklyTarget = targetMinutes * 7
        val monthlyTarget = targetMinutes * 30

        return ScreenTimeProgress(
            targetMinutes = targetMinutes,
            todayMinutes = todayMinutes,
            isDailyTargetMet = todayMinutes >= targetMinutes,
            dailyProgressPercent = (todayMinutes.toFloat() / targetMinutes * 100).coerceAtMost(100f),
            weeklyMinutes = weeklyMinutes,
            weeklyTargetMinutes = weeklyTarget,
            isWeeklyTargetMet = weeklyMinutes >= weeklyTarget,
            weeklyProgressPercent = (weeklyMinutes.toFloat() / weeklyTarget * 100).coerceAtMost(100f),
            monthlyMinutes = monthlyMinutes,
            monthlyTargetMinutes = monthlyTarget,
            isMonthlyTargetMet = monthlyMinutes >= monthlyTarget,
            monthlyProgressPercent = (monthlyMinutes.toFloat() / monthlyTarget * 100).coerceAtMost(
                100f
            ),
            entries = entries
        )
    }


    // ── Private Helpers ───────────────────────────────────────────────────────

    private suspend fun saveProfileToDb(userId: String, data: UserProfileData) {
        data.profile?.let { profileDao.insertProfile(it.toEntity()) }
        data.screenTimeWeek?.let { entries ->
            screenTimeDao.deleteScreenTime(userId)
            screenTimeDao.insertScreenTime(entries.map { it.toEntity(userId) })
        }
    }

    private suspend fun applyOptimisticUpdate(userId: String, request: UpdateProfileRequest) {
        profileDao.updateProfile(
            userId = userId,
            name = request.name,
            phone = request.phone,
            imageUrl = request.imageUrl,
            gender = request.gender,
            dob = request.dob,
            visibility = request.visibility,
            umrahOptIn = request.umrahOptIn,
            locationLat = request.location?.lat,
            locationLng = request.location?.lng,
            locationGeohash = request.location?.geohash,
            locationUpdatedAt = request.location?.updatedAt
        )
    }
}