package com.itm.profile_sdk.network

import kotlinx.serialization.Serializable

sealed class ApiException(message: String) : Exception(message) {
    /** 400 – validation or bad input. [errors] holds per-field messages. */
    class BadRequest(message: String, val errors: Map<String, List<String>>? = null) : ApiException(message)
    /** 401 – token missing or expired. */
    class Unauthorized(message: String) : ApiException(message)
    /** 403 – authenticated but not allowed. */
    class Forbidden(message: String) : ApiException(message)
    /** 404 – resource not found. */
    class NotFound(message: String) : ApiException(message)
    /** 5xx – something went wrong on the server. */
    class ServerError(message: String, val code: Int) : ApiException(message)
    /** Any other non-2xx not covered above. */
    class Unknown(message: String, val code: Int) : ApiException(message)
}

@Serializable
internal data class ApiErrorResponse(
    val success: Boolean = false,
    val code: Int? = null,
    val message: String? = null,
    val errors: Map<String, List<String>>? = null
)
