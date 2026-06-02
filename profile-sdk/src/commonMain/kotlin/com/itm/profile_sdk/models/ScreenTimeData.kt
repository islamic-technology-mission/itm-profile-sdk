package com.itm.profile_sdk.models

import kotlinx.serialization.Serializable

@Serializable
data class ScreenTimeData(
    val screenTime: List<ScreenTimeEntry>? = null
)