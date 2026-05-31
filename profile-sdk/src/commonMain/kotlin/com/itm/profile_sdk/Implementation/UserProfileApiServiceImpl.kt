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
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

internal class UserProfileApiServiceImpl(
    private val client: HttpClient
) : UserProfileApiService {

    private fun url(path: String) = "${ApiConstants.BASE_URL}$path"

    override suspend fun upsertProfile(
        token: String, userId: String, request: UpsertProfileRequest
    ): UserProfileResponse {
        return client.post(url(ApiConstants.Endpoints.userProfile(userId))) {
            bearerAuth(token)
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
    }

    override suspend fun updateProfile(
        token: String, userId: String, request: UpdateProfileRequest
    ): UserProfileResponse {
        return client.patch(url(ApiConstants.Endpoints.userProfile(userId))) {
            bearerAuth(token)
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
    }

    override suspend fun getProfile(
        token: String, userId: String
    ): UserProfileResponse {
        return client.get(url(ApiConstants.Endpoints.userProfile(userId))) {
            bearerAuth(token)
            contentType(ContentType.Application.Json)
        }.body()
    }

    override suspend fun getProfileViews(
        token: String, userId: String, cursor: String?, limit: Int?
    ): ProfileViewsResponse {
        return client.get(url(ApiConstants.Endpoints.profileViews(userId))) {
            bearerAuth(token)
            contentType(ContentType.Application.Json)
            cursor?.let { parameter("cursor", it) }
            limit?.let  { parameter("limit", it) }
        }.body()
    }

    override suspend fun postScreenTime(
        token: String, userId: String, request: ScreenTimeRequest
    ): ScreenTimeResponse {
        return client.post(url(ApiConstants.Endpoints.screenTime(userId))) {
            bearerAuth(token)
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
    }

    override suspend fun getScreenTime(
        token: String, userId: String, days: Int?
    ): ScreenTimeResponse {
        return client.get(url(ApiConstants.Endpoints.screenTime(userId))) {
            bearerAuth(token)
            contentType(ContentType.Application.Json)
            days?.let { parameter("days", it) }
        }.body()
    }

    override suspend fun getNearbyUsers(
        token: String, lat: Double?, lng: Double?
    ): NearbyUsersResponse {
        return client.get(url(ApiConstants.Endpoints.NEARBY)) {
            bearerAuth(token)
            contentType(ContentType.Application.Json)
            lat?.let { parameter("lat", it) }
            lng?.let { parameter("lng", it) }
        }.body()
    }
}