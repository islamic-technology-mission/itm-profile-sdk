package com.itm.profile_sdk.models

import kotlinx.serialization.Serializable

@Serializable
data class UserProfile(
    val id: String = "",
    val platform: String? = null,
    val email: String? = null,
    val migrated: Boolean? = false,
    val umrahOptIn: Boolean? = false,
    val phone: String? = null,
    val imageUrl: String? = null,
    val gender: String? = null,
    val dob: String? = null,
    val location: UserLocation? = null,
    val whatsappVerified: Boolean? = false,
    val createdAt: String? = null,
    val name: String? = null,
    val visibility: String? = null,
    val updatedAt: String? = null,
    val nearbyUsers: Int? = 0
) {
    fun isPublic(): Boolean  = visibility.equals("public", ignoreCase = true)
}