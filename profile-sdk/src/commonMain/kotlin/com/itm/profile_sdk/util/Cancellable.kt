package com.itm.profile_sdk.util

/**
 * Returned from Flow observation functions.
 * Call cancel() to stop receiving updates and clean up the coroutine.
 */
class Cancellable(private val onCancel: () -> Unit) {
    fun cancel() = onCancel()
}