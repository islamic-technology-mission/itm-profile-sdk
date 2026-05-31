package com.itm.profile_sdk.auth

import kotlinx.serialization.Serializable

@Serializable
internal data class TokenData(
    val idToken: String? = null,
    val refreshToken: String? = null,
    val uid: String? = null,
    val email: String? = null,
    val displayName: String? = null,
    val expiresIn: String? = null
)