package com.itm.profile_sdk.network

import com.itm.profile_sdk.models.NearbyUsersResponse
import com.itm.profile_sdk.models.ProfileViewsResponse
import com.itm.profile_sdk.models.ScreenTimeRequest
import com.itm.profile_sdk.models.ScreenTimeResponse
import com.itm.profile_sdk.models.UpdateProfileRequest
import com.itm.profile_sdk.models.UpsertProfileRequest
import com.itm.profile_sdk.models.UserProfileResponse

internal interface UserProfileApiService {
    suspend fun upsertProfile(
        token: String,
        userId: String,
        request: UpsertProfileRequest
    ): UserProfileResponse

    suspend fun updateProfile(
        token: String,
        userId: String,
        request: UpdateProfileRequest
    ): UserProfileResponse

    suspend fun getProfile(token: String, userId: String): UserProfileResponse
    suspend fun getProfileViews(
        token: String,
        userId: String,
        cursor: String? = null,
        limit: Int? = null
    ): ProfileViewsResponse

    suspend fun postScreenTime(
        token: String,
        userId: String,
        request: ScreenTimeRequest
    ): ScreenTimeResponse

    suspend fun getScreenTime(token: String, userId: String, days: Int? = null): ScreenTimeResponse
    suspend fun getNearbyUsers(
        token: String,
        lat: Double? = null,
        lng: Double? = null
    ): NearbyUsersResponse
}