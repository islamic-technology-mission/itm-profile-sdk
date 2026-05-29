package com.itm.profile_sdk.models

import kotlinx.serialization.Serializable

@Serializable
data class UserProfile(
    val id: String? = null,
    val name: String? = null,
    val email: String? = null,
    val phone: String? = null,
    val gender: String? = null,
    val dob: String? = null,
    val platform: String? = null,
    val visibility: String? = null,
    val location: UserLocation? = null,
    val umrahOptIn: Boolean? = null,
    val migrated: Boolean? = null,
    val whatsappVerified: Boolean? = null,
    val createdAt: String? = null,
    val nearbyUsers: Int? = null
)