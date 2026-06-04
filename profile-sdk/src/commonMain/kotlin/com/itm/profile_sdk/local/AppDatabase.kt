package com.itm.profile_sdk.local

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import com.itm.profile_sdk.local.dao.ScreenTimeDao
import com.itm.profile_sdk.local.dao.UserProfileDao
import com.itm.profile_sdk.local.entity.ScreenTimeEntity
import com.itm.profile_sdk.local.entity.UserProfileEntity

@Database(
    entities = [
        UserProfileEntity::class,
        ScreenTimeEntity::class
    ],
    version = 1
)
@ConstructedBy(AppDatabaseConstructor::class)
abstract class AppDatabase : RoomDatabase() {
    internal abstract fun userProfileDao(): UserProfileDao
    internal abstract fun screenTimeDao(): ScreenTimeDao
}

@Suppress("NO_ACTUAL_FOR_EXPECT")
expect object AppDatabaseConstructor : RoomDatabaseConstructor<AppDatabase> {
    override fun initialize(): AppDatabase
}

// expect function — each platform implements buildDatabase() differently
internal expect fun buildDatabase(context: Any): AppDatabase

internal const val DB_NAME = "app_database.db"