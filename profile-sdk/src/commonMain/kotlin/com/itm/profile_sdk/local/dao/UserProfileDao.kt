package com.itm.profile_sdk.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.itm.profile_sdk.local.entity.UserProfileEntity
import kotlinx.coroutines.flow.Flow

@Dao
internal interface UserProfileDao {

    @Query("SELECT * FROM user_profile WHERE id = :userId")
    fun observeProfile(userId: String): Flow<UserProfileEntity?>

    @Query("SELECT * FROM user_profile WHERE id = :userId")
    suspend fun getProfile(userId: String): UserProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: UserProfileEntity)

    @Query("""
        UPDATE user_profile SET
            name                = :name,
            phone               = :phone,
            gender              = :gender,
            dob                 = :dob,
            visibility          = :visibility,
            umrahOptIn          = :umrahOptIn,
            locationLat         = :locationLat,
            locationLng         = :locationLng,
            locationGeohash     = :locationGeohash,
            locationUpdatedAt   = :locationUpdatedAt
        WHERE id = :userId
    """)
    suspend fun updateProfile(
        userId: String,
        name: String?,
        phone: String?,
        gender: String?,
        dob: String?,
        visibility: String?,
        umrahOptIn: Boolean?,
        locationLat: Double?,
        locationLng: Double?,
        locationGeohash: String?,
        locationUpdatedAt: String?
    )

    @Query("DELETE FROM user_profile WHERE id = :userId")
    suspend fun deleteProfile(userId: String)
}