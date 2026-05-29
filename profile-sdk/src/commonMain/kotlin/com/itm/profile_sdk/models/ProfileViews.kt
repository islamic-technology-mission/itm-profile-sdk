package com.itm.profile_sdk.models

import kotlinx.serialization.Serializable

@Serializable
data class ProfileViews(
    val totalViews: Int? = null,
    val userImages: List<String>? = null
)