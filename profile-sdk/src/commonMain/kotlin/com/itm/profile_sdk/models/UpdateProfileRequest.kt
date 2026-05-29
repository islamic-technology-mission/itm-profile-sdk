package com.itm.profile_sdk.models

import kotlinx.serialization.Serializable

@Serializable
data class UpdateProfileRequest(
    val name: String? = null,
    val phone: String? = null,
    val gender: String? = null,
    val dob: String? = null,
    val visibility: String? = null,
    val location: UserLocation? = null,
    val umrahOptIn: Boolean? = null
)