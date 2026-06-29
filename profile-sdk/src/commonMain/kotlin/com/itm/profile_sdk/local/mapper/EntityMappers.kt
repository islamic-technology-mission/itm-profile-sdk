package com.itm.profile_sdk.local.mapper

import com.itm.profile_sdk.local.entity.ScreenTimeEntity
import com.itm.profile_sdk.local.entity.UserProfileEntity
import com.itm.profile_sdk.models.ScreenTimeEntry
import com.itm.profile_sdk.models.UserLocation
import com.itm.profile_sdk.models.UserProfile

// ─── UserProfile ──────────────────────────────────────────────────────────────

internal fun UserProfile.toEntity(): UserProfileEntity = UserProfileEntity(
    id = id,
    name = name,
    email = email,
    phone = phone,
    imageUrl = imageUrl,
    gender = gender,
    dob = dob,
    platform = platform,
    visibility = visibility,
    umrahOptIn = umrahOptIn,
    migrated = migrated,
    whatsappVerified = whatsappVerified,
    createdAt = createdAt,
    nearbyUsers = nearbyUsers,
    locationLat = location?.lat,
    locationLng = location?.lng,
    locationGeohash = location?.geohash,
    locationUpdatedAt = location?.updatedAt,
    locationCity = location?.city,
    locationCountry = location?.country
)

internal fun UserProfileEntity.toDomain(): UserProfile = UserProfile(
    id = id,
    name = name,
    email = email,
    phone = phone,
    imageUrl = imageUrl,
    gender = gender,
    dob = dob,
    platform = platform,
    visibility = visibility,
    umrahOptIn = umrahOptIn,
    migrated = migrated,
    whatsappVerified = whatsappVerified,
    createdAt = createdAt,
    nearbyUsers = nearbyUsers,
    location = if (locationLat != null || locationLng != null) {
        UserLocation(
            lat = locationLat,
            lng = locationLng,
            geohash = locationGeohash,
            updatedAt = locationUpdatedAt,
            city = locationCity,
            country = locationCountry
        )
    } else null
)

// ─── ScreenTime ───────────────────────────────────────────────────────────────

internal fun ScreenTimeEntry.toEntity(userId: String): ScreenTimeEntity = ScreenTimeEntity(
    userId = userId,
    date = date ?: "",
    minutes = minutes
)

internal fun ScreenTimeEntity.toDomain(): ScreenTimeEntry = ScreenTimeEntry(
    date = date,
    minutes = minutes
)

// Note: Subscription and ProfileViews have no mappers — both are live from API only