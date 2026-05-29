package com.itm.profile_sdk.models

import kotlinx.serialization.Serializable

@Serializable
data class ProfileViewsResponse(
    val status: String? = null,
    val data: ProfileViewsData? = null
)