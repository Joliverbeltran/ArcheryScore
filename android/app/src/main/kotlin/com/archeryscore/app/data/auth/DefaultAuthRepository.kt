package com.archeryscore.app.data.auth

import com.archeryscore.app.data.prefs.DataStorePreferencesRepository
import com.archeryscore.app.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow

class DefaultAuthRepository(
    private val prefs: DataStorePreferencesRepository,
) : AuthRepository {

    override val currentUserId: Flow<String?> = prefs.observeLocalUserId()

    override suspend fun requireUserId(): String = prefs.ensureLocalUserId()
}