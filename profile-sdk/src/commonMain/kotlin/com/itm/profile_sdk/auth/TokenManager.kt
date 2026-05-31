package com.itm.profile_sdk.auth

import com.itm.profile_sdk.getSystemTimeMillis

/**
 * Internal to SDK.
 * Manages token lifecycle:
 *  - Generates token via /internal/generate-token
 *  - Caches idToken + expiry
 *  - Auto-renews by calling /internal/generate-token again when expired
 */
internal class TokenManager(
    private val tokenService: InternalTokenService = InternalTokenService()
) {
    private var uid: String? = null
    private var internalKey: String? = null
    private var cachedIdToken: String? = null
    private var tokenFetchedAt: Long = 0L
    private var expiresInMillis: Long = 3600L * 1000L

    /**
     * Generates and caches a fresh token.
     * Sample apps call this once on startup.
     * @return idToken to be passed to SDK API functions
     */
    suspend fun generateToken(uid: String, internalKey: String): String {
        this.uid         = uid
        this.internalKey = internalKey
        return fetchAndCache(uid, internalKey)
    }

    /**
     * Returns a valid idToken.
     * - If token is still fresh → returns cached token
     * - If token expired → calls /internal/generate-token again automatically
     */
    suspend fun getValidToken(): String {
        if (!isTokenExpired()) return cachedIdToken
            ?: error("No token cached. Call generateToken() first.")

        val currentUid = uid
            ?: error("No uid available. Call generateToken() first.")
        val currentKey = internalKey
            ?: error("No internalKey available. Call generateToken() first.")

        return fetchAndCache(currentUid, currentKey)
    }

    fun hasToken(): Boolean = cachedIdToken != null

    // ── Private ───────────────────────────────────────────────────────────────

    private suspend fun fetchAndCache(uid: String, internalKey: String): String {
        val response = tokenService.generateToken(uid, internalKey)
        if (response.success == true && response.data?.idToken != null) {
            cachedIdToken    = response.data.idToken
            tokenFetchedAt   = getSystemTimeMillis()
            expiresInMillis  = (response.data.expiresIn?.toLongOrNull() ?: 3600L) * 1000L
            return cachedIdToken?:""
        }
        error("Token generation failed: ${response.message}")
    }

    private fun isTokenExpired(): Boolean {
        if (cachedIdToken == null) return true
        // Renew 60 seconds before actual expiry to avoid edge cases
        val buffer = 60_000L
        return getSystemTimeMillis() - tokenFetchedAt >= expiresInMillis - buffer
    }
}
