package com.itm.profile_sdk.local

import android.content.Context
import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver

actual fun buildDatabase(context: Any): AppDatabase {
    require(context is Context) {
        "On Android, pass Application context to ISDKClient.initialize()"
    }
    return Room.databaseBuilder<AppDatabase>(
        context = context.applicationContext,
        name = context.getDatabasePath(DB_NAME).absolutePath
    )
        .setDriver(BundledSQLiteDriver())
        .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
        .build()
}
