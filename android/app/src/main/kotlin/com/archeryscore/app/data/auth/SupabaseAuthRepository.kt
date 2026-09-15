package com.archeryscore.app.data.auth

import com.archeryscore.app.data.prefs.DataStorePreferencesRepository
import com.archeryscore.app.domain.repository.AuthFailureReason
import com.archeryscore.app.domain.repository.AuthRepository
import com.archeryscore.app.domain.repository.AuthResult
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import java.io.IOException

class SupabaseAuthRepository(
    private val supabase: SupabaseClient,
    private val prefs: DataStorePreferencesRepository,
) : AuthRepository {

    private val _currentUserId = MutableStateFlow<String?>(null)

    override val currentUserId: Flow<String?> = _currentUserId

    override suspend fun requireUserId(): String =
        supabase.auth.currentUserOrNull()?.id ?: prefs.ensureLocalUserId()

    override suspend fun signUp(email: String, password: String): AuthResult {
        validateEmail(email)?.let { return AuthResult.Failure(it) }
        return runAuth {
            supabase.auth.signUpWith(Email) {
                this.email = email
                this.password = password
            }
            publishCurrentUser()
            AuthResult.Success
        }
    }

    override suspend fun signIn(email: String, password: String): AuthResult {
        validateEmail(email)?.let { return AuthResult.Failure(it) }
        return runAuth {
            supabase.auth.signInWith(Email) {
                this.email = email
                this.password = password
            }
            publishCurrentUser()
            AuthResult.Success
        }
    }

    override suspend fun restore(): AuthResult =
        runAuth {
            supabase.auth.loadFromStorage(false)
            publishCurrentUser()
            AuthResult.Success
        }

    override suspend fun signOut(): AuthResult =
        runAuth {
            supabase.auth.signOut()
            _currentUserId.value = null
            AuthResult.Success
        }

    private suspend fun publishCurrentUser() {
        _currentUserId.value = supabase.auth.currentUserOrNull()?.id
    }

    private suspend fun runAuth(block: suspend () -> AuthResult): AuthResult =
        try {
            block()
        } catch (e: IOException) {
            AuthResult.Failure(AuthFailureReason.NETWORK)
        } catch (e: Exception) {
            mapAuthError(e)
        }

    private fun mapAuthError(e: Exception): AuthResult {
        val message = e.message.orEmpty().lowercase()
        return when {
            "email not confirmed" in message || "invalid login credentials" in message ->
                AuthResult.Failure(AuthFailureReason.INVALID_CREDENTIALS)
            "already registered" in message || "user already" in message ||
                "email_taken" in message -> AuthResult.Failure(AuthFailureReason.EMAIL_TAKEN)
            "password should be at least" in message || "weak password" in message ->
                AuthResult.Failure(AuthFailureReason.WEAK_PASSWORD)
            "socket" in message || "failed to connect" in message || "timeout" in message ->
                AuthResult.Failure(AuthFailureReason.NETWORK)
            else -> AuthResult.Failure(AuthFailureReason.UNKNOWN)
        }
    }

    private fun validateEmail(email: String): AuthFailureReason? =
        if (!EMAIL_REGEX.matches(email)) AuthFailureReason.INVALID_EMAIL else null

    private companion object {
        val EMAIL_REGEX = Regex("[^@\\s]+@[^@\\s]+\\.[^@\\s]+")
    }
}
