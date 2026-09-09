package com.archeryscore.app.data.auth

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import com.archeryscore.app.data.prefs.DataStorePreferencesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import java.io.File

object TestDataStore {

    fun prefs(
        file: File = File(System.getProperty("java.io.tmpdir"), "test-$id.preferences_pb"),
    ): DataStorePreferencesRepository {
        val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        val store: DataStore<Preferences> = PreferenceDataStoreFactory.create(scope = scope) { file }
        return DataStorePreferencesRepository(store)
    }

    private val id: String
        get() = java.util.UUID.randomUUID().toString()
}
