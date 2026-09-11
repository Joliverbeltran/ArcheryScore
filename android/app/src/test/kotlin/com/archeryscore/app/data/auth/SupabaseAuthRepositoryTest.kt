package com.archeryscore.app.data.auth

import com.archeryscore.app.data.auth.SupabaseAuthRepository
import com.archeryscore.app.data.auth.TestDataStore
import io.mockk.mockk
import io.github.jan.supabase.SupabaseClient
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

class SupabaseAuthRepositoryTest {

    @Test
    fun `currentUserId falls back to local prefs id when not signed in`() = runTest {
        val prefs = TestDataStore.prefs()
        val repo = SupabaseAuthRepository(
            supabase = mockk<SupabaseClient>(relaxed = true),
            prefs = prefs,
        )

        val localId = prefs.ensureLocalUserId()

        assertEquals(localId, repo.currentUserId.first())
        assertNotNull(localId)
    }
}