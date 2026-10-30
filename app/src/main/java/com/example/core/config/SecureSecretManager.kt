package com.example.core.config

import android.content.Context

/**
 * Interface isolating secret access and credential security.
 * Prevents hardcoding secrets in source files or decompiled assets.
 */
interface SecureSecretManager {
    fun isFirebaseAppCheckEnabled(): Boolean
}

class DefaultSecureSecretManager(
    private val context: Context
) : SecureSecretManager {

    override fun isFirebaseAppCheckEnabled(): Boolean {
        return true
    }
}
