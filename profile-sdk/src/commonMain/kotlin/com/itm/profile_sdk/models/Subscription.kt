package com.itm.profile_sdk.models

import kotlinx.serialization.Serializable

@Serializable
data class Subscription(
    val active: Boolean? = null,
    val expiresAt: String? = null,
    val sku: String? = null,
    val platform: String? = null
)