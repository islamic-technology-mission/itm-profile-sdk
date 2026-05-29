package com.itm.profile_sdk.network

import com.itm.profile_sdk.models.NearbyUsersResponse
import com.itm.profile_sdk.models.ProfileViewsResponse
import com.itm.profile_sdk.models.ScreenTimeRequest
import com.itm.profile_sdk.models.ScreenTimeResponse
import com.itm.profile_sdk.models.UpdateProfileRequest
import com.itm.profile_sdk.models.UpsertProfileRequest
import com.itm.profile_sdk.models.UserProfileResponse

interface UserProfileApiService {

    // POST /api/v1/users/{userId}/profile
    // First-login profile upsert (idempotent merge)
    suspend fun upsertProfile(userId: String, request: UpsertProfileRequest): UserProfileResponse

    // PATCH /api/v1/users/{userId}/profile
    // Partial profile update
    suspend fun updateProfile(userId: String, request: UpdateProfileRequest): UserProfileResponse

    // GET /api/v1/users/{userId}/profile
    // Own profile returns full payload; other user returns public profile + increments view count
    suspend fun getProfile(userId: String): UserProfileResponse

    // GET /api/v1/users/{userId}/profile-views
    // Owner-only paginated list of profile viewers
    suspend fun getProfileViews(
        userId: String,
        cursor: String? = null,
        limit: Int? = null
    ): ProfileViewsResponse

    // POST /api/v1/users/{userId}/screen-time
    // Append screen-time seconds for a date (server increments)
    suspend fun postScreenTime(userId: String, request: ScreenTimeRequest): ScreenTimeResponse

    // GET /api/v1/users/{userId}/screen-time
    // Get last N days of screen-time (default 7)
    suspend fun getScreenTime(userId: String, days: Int? = null): ScreenTimeResponse

    // GET /api/v1/users/nearby
    // Nearby public users — lat/lng optional, falls back to saved location
    suspend fun getNearbyUsers(lat: Double? = null, lng: Double? = null): NearbyUsersResponse
}