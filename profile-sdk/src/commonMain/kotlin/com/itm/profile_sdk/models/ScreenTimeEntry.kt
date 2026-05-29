package com.itm.profile_sdk.models

import kotlinx.serialization.Serializable

@Serializable
data class ScreenTimeEntry(
    val date: String? = null,
    val minutes: Int? = null
)