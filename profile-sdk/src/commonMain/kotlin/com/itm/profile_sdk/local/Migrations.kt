package com.itm.profile_sdk.local

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

/**
 * v1 → v2: Added screen_time_target table.
 */
internal val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL(
            "CREATE TABLE IF NOT EXISTS `screen_time_target` " +
                "(`userId` TEXT NOT NULL, `dailyTargetMinutes` INTEGER NOT NULL DEFAULT 15, " +
                "PRIMARY KEY(`userId`))"
        )
    }
}

/**
 * v2 → v3: Added imageUrl column to user_profile.
 */
internal val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE `user_profile` ADD COLUMN `imageUrl` TEXT")
    }
}

/**
 * v3 → v4: Added city and country columns to user_profile.
 */
internal val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE `user_profile` ADD COLUMN `city` TEXT")
        connection.execSQL("ALTER TABLE `user_profile` ADD COLUMN `country` TEXT")
    }
}

/**
 * v4 → v5: Added protectedFieldsUnlockAt column to user_profile.
 */
internal val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE `user_profile` ADD COLUMN `protectedFieldsUnlockAt` TEXT")
    }
}
