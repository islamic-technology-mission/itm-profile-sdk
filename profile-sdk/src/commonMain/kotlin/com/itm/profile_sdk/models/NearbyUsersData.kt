package com.itm.profile_sdk.models

import kotlinx.serialization.Serializable

@Serializable
data class NearbyUsersData(
    val uid: String? = null,
    val name: String? = null,
    val imageUrl: String? = null,
    val location: UserLocation? = null,
    val nearbyUsers: List<NearbyUser>? = null
)