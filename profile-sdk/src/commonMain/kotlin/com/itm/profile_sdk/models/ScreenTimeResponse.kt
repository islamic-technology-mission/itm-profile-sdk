package com.itm.profile_sdk.models

import kotlinx.serialization.Serializable

@Serializable
data class ScreenTimeResponse(
    val status: String? = null,
    val data: ScreenTimeData? = null
)