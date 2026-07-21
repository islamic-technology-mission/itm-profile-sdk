package com.itm.profile_sdk.network

object ApiConstants {
    const val BASE_URL_SANDBOX = "https://sandbox.theislam360.com/"
    const val BASE_URL_PRODUCTION = "https://api.theislam360.com/"

    internal object Endpoints {
        // Profile
        fun userProfile(userId: String) = "api/v1/users/$userId/profile"
        fun profileViews(userId: String) = "api/v1/users/$userId/profile-views"

        // Screen time
        fun screenTime(userId: String) = "api/v1/users/$userId/screen-time"

        // Nearby
        const val NEARBY = "api/v1/users/nearby"
    }
}