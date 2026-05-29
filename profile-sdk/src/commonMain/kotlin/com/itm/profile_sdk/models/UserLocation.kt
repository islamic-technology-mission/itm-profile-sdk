package com.itm.profile_sdk.models

import kotlinx.serialization.Serializable

@Serializable
data class UserLocation(
    val lat: Double? = null,
    val lng: Double? = null,
    val geohash: String? = null,
    val updatedAt: String? = null
)