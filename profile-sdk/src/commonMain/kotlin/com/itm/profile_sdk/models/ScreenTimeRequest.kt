package com.itm.profile_sdk.models

import kotlinx.serialization.Serializable

@Serializable
data class ScreenTimeRequest(
    val date: String? = null,
    val seconds: Int? = null
)