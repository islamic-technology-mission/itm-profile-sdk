package com.itm.profile_sdk.Implementation

import com.itm.profile_sdk.models.NearbyUsersResponse
import com.itm.profile_sdk.models.ProfileViewsResponse
import com.itm.profile_sdk.models.ScreenTimeRequest
import com.itm.profile_sdk.models.ScreenTimeResponse
import com.itm.profile_sdk.models.UpdateProfileRequest
import com.itm.profile_sdk.models.UpsertProfileRequest
import com.itm.profile_sdk.models.UserProfileResponse
import com.itm.profile_sdk.network.ApiConstants
import com.itm.profile_sdk.network.UserProfileApiService
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

class UserProfileApiServiceImpl(
    private val client: HttpClient
) : UserProfileApiService {
    // POST /profile — upsert
    override suspend fun upsertProfile(
        userId: String,
        request: UpsertProfileRequest
    ): UserProfileResponse {
        return client.post(ApiConstants.Endpoints.userProfile(userId)) {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
    }

    // PATCH /profile — partial update
    override suspend fun updateProfile(
        userId: String,
        request: UpdateProfileRequest
    ): UserProfileResponse {
        return client.patch(ApiConstants.Endpoints.userProfile(userId)) {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
    }

    // GET /profile
    override suspend fun getProfile(userId: String): UserProfileResponse {
        return client.get(ApiConstants.Endpoints.userProfile(userId)) {
            contentType(ContentType.Application.Json)
        }.body()
    }

    // GET /profile-views
    override suspend fun getProfileViews(
        userId: String,
        cursor: String?,
        limit: Int?
    ): ProfileViewsResponse {
        return client.get(ApiConstants.Endpoints.profileViews(userId)) {
            contentType(ContentType.Application.Json)
            cursor?.let { parameter("cursor", it) }
            limit?.let { parameter("limit", it) }
        }.body()
    }

    // POST /screen-time
    override suspend fun postScreenTime(
        userId: String,
        request: ScreenTimeRequest
    ): ScreenTimeResponse {
        return client.post(ApiConstants.Endpoints.screenTime(userId)) {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
    }

    // GET /screen-time
    override suspend fun getScreenTime(
        userId: String,
        days: Int?
    ): ScreenTimeResponse {
        return client.get(ApiConstants.Endpoints.screenTime(userId)) {
            contentType(ContentType.Application.Json)
            days?.let { parameter("days", it) }
        }.body()
    }

    // GET /nearby
    override suspend fun getNearbyUsers(
        lat: Double?,
        lng: Double?
    ): NearbyUsersResponse {
        return client.get(ApiConstants.BASE_URL + ApiConstants.Endpoints.NEARBY) {
            contentType(ContentType.Application.Json)
            lat?.let { parameter("lat", it) }
            lng?.let { parameter("lng", it) }
        }.body()
    }
}