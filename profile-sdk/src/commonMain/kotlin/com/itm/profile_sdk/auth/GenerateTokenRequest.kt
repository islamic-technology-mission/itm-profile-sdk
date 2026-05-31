package com.itm.profile_sdk.auth

import kotlinx.serialization.Serializable

@Serializable
internal data class GenerateTokenRequest(
    val uid: String
)