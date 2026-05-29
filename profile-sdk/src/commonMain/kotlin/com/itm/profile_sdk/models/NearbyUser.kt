package com.itm.profile_sdk.models

import kotlinx.serialization.Serializable

@Serializable
data class NearbyUser(
    val id: String? = null,
    val name: String? = null,
    val imageUrl: String? = null,
    val location: UserLocation? = null,
    val gender: String? = null,
    val visibility: String? = null
)