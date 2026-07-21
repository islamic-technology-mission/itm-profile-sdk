package com.itm.profile_sdk.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_profile")
internal data class UserProfileEntity(
    @PrimaryKey
    val id: String,
    val name: String? = null,
    val email: String? = null,
    val phone: String? = null,
    val imageUrl: String? = null,
    val gender: String? = null,
    val dob: String? = null,
    val platform: String? = null,
    val visibility: String? = null,
    val umrahOptIn: Boolean? = null,
    val migrated: Boolean? = null,
    val whatsappVerified: Boolean? = null,
    val createdAt: String? = null,
    val protectedFieldsUnlockAt: String? = null,
    val nearbyUsers: Int? = null,
    val locationLat: Double? = null,
    val locationLng: Double? = null,
    val locationGeohash: String? = null,
    val locationUpdatedAt: String? = null,
    val locationCity: String? = null,
    val locationCountry: String? = null
)