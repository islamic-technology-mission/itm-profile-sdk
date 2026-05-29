package com.itm.profile_sdk.models

import kotlinx.serialization.Serializable

@Serializable
data class ProfileViewsData(
    val total: Int? = null,
    val items: List<ProfileViewer>? = null,
    val nextCursor: String? = null,
    val hasMore: Boolean? = null
)