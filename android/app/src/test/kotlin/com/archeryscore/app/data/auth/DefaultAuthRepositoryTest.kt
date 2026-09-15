package com.archeryscore.app.data.auth

import com.archeryscore.app.data.prefs.DataStorePreferencesRepository
import com.archeryscore.app.domain.repository.AuthFailureReason
import com.archeryscore.app.domain.repository.AuthResult
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Test

class DefaultAuthRepositoryTest {

    private fun buildRepo(): DefaultAuthRepository =
        DefaultAuthRepository(TestDataStore.prefs())

    @Test
    fun `sign up with invalid email fails`() = runTest {
        val repo = buildRepo()
        val result = repo.signUp("not-an-email", "password123")

        assertInstanceOf(AuthResult.Failure::class.java, result)
        assertEquals(
            AuthFailureReason.INVALID_EMAIL,
            (result as AuthResult.Failure).reason,
        )
    }

    @Test
    fun `sign up with weak password fails`() = runTest {
        val repo = buildRepo()
        val result = repo.signUp("user@example.com", "short")

        assertEquals(
            AuthFailureReason.WEAK_PASSWORD,
            (result as AuthResult.Failure).reason,
        )
    }

    @Test
    fun `sign up with valid credentials succeeds`() = runTest {
        val repo = buildRepo()
        assertEquals(AuthResult.Success, repo.signUp("user@example.com", "password123"))
    }

    @Test
    fun `sign in validates credentials then succeeds`() = runTest {
        val repo = buildRepo()
        assertEquals(AuthResult.Success, repo.signIn("user@example.com", "password123"))
        assertEquals(
            AuthFailureReason.INVALID_EMAIL,
            (repo.signIn("bad", "password123") as AuthResult.Failure).reason,
        )
    }

    @Test
    fun `restore and sign out succeed locally`() = runTest {
        val repo = buildRepo()
        assertEquals(AuthResult.Success, repo.restore())
        assertEquals(AuthResult.Success, repo.signOut())
    }

    @Test
    fun `require user id generates persistent id`() = runTest {
        val repo = buildRepo()
        val first = repo.requireUserId()
        val second = repo.requireUserId()
        assertEquals(first, second)
        assertEquals(first, repo.currentUserId.first())
    }
}
