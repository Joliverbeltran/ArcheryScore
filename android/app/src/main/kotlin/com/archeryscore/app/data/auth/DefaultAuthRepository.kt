package com.archeryscore.app.data.auth

import com.archeryscore.app.data.prefs.DataStorePreferencesRepository
import com.archeryscore.app.domain.repository.AuthRepository
import com.archeryscore.app.domain.repository.AuthFailureReason
import com.archeryscore.app.domain.repository.AuthResult
import kotlinx.coroutines.flow.Flow

class DefaultAuthRepository(
    private val prefs: DataStorePreferencesRepository,
) : AuthRepository {

    override val currentUserId: Flow<String?> = prefs.observeLocalUserId()

    override suspend fun requireUserId(): String = prefs.ensureLocalUserId()

    override suspend fun signUp(email: String, password: String): AuthResult {
        validateCredentials(email, password)?.let { return AuthResult.Failure(it) }
        return AuthResult.Success
    }

    override suspend fun signIn(email: String, password: String): AuthResult {
        validateCredentials(email, password)?.let { return AuthResult.Failure(it) }
        return AuthResult.Success
    }

    override suspend fun restore(): AuthResult = AuthResult.Success

    override suspend fun signOut(): AuthResult = AuthResult.Success

    private fun validateCredentials(email: String, password: String): AuthFailureReason? {
        if (!EMAIL_REGEX.matches(email)) return AuthFailureReason.INVALID_EMAIL
        if (password.length < MIN_PASSWORD) return AuthFailureReason.WEAK_PASSWORD
        return null
    }

    private companion object {
        val EMAIL_REGEX = Regex("[^@\\s]+@[^@\\s]+\\.[^@\\s]+")
        const val MIN_PASSWORD = 8
    }
}
