package com.itm.profile_sdk.models

import kotlinx.serialization.Serializable

@Serializable
data class UserProfileData(
    val profile: UserProfile? = null,
    val subscription: Subscription? = null,
    val screenTimeWeek: List<ScreenTimeEntry>? = null,
    val profileViews: ProfileViews? = null
)