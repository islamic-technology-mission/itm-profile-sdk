package com.itm.profile_sdk.models

import kotlinx.serialization.Serializable

@Serializable
data class ProfileViewer(
    val viewerUid: String? = null,
    val name: String? = null,
    val imageUrl: String? = null,
    val lastViewedAt: String? = null,
    val viewCount: Int? = null
)