package com.itm.profile_sdk.models

import kotlinx.serialization.Serializable

@Serializable
data class UserProfileResponse(
    val status: String? = null,
    val data: UserProfileData? = null
)