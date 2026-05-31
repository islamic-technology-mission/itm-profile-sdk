package com.itm.profile_sdk.auth

import kotlinx.serialization.Serializable

@Serializable
internal data class GenerateTokenResponse(
    val success: Boolean? = null,
    val code: Int? = null,
    val message: String? = null,
    val data: TokenData? = null
)