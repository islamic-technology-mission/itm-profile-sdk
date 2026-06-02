package com.itm.profile_sdk.models

import kotlinx.serialization.Serializable

@Serializable
data class NearbyUsersResponse(
    val status: String? = null,
    val data: NearbyUsersData? = null
)