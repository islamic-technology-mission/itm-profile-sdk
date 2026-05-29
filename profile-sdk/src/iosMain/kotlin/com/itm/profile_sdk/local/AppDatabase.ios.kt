package com.itm.profile_sdk.local
import androidx.room.Room
import androidx.room.RoomDatabaseConstructor
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask

actual object AppDatabaseConstructor : RoomDatabaseConstructor<AppDatabase> {
    actual override fun initialize(): AppDatabase = throw NotImplementedError(
        "Use buildDatabase() to instantiate AppDatabase on iOS"
    )
}

@OptIn(ExperimentalForeignApi::class)
fun buildDatabase(): AppDatabase {
    val dbPath = NSFileManager.defaultManager.URLForDirectory(
        directory = NSDocumentDirectory,
        inDomain = NSUserDomainMask,
        appropriateForURL = null,
        create = false,
        error = null
    )!!.path + "/$DB_NAME"

    return Room.databaseBuilder<AppDatabase>(name = dbPath)
        .setDriver(BundledSQLiteDriver())
        .build()
}
