package com.itm.profile_sdk.local

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabaseConstructor
import androidx.sqlite.driver.bundled.BundledSQLiteDriver

actual object AppDatabaseConstructor : RoomDatabaseConstructor<AppDatabase> {
    actual override fun initialize(): AppDatabase = throw NotImplementedError(
        "Use buildDatabase(context) to instantiate AppDatabase on Android"
    )
}

fun buildDatabase(context: Context): AppDatabase {
    return Room.databaseBuilder<AppDatabase>(
        context = context.applicationContext,
        name = context.getDatabasePath(DB_NAME).absolutePath,
    )
        .setDriver(BundledSQLiteDriver())
        .build()
}