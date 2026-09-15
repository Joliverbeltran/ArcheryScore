package com.archeryscore.app.data.prefs

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.archeryscore.app.domain.model.Discipline
import com.archeryscore.app.domain.model.RoundType
import com.archeryscore.app.domain.model.UserPreferences
import com.archeryscore.app.domain.repository.PreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class DataStorePreferencesRepository(
    private val store: DataStore<Preferences>,
) : PreferencesRepository {

    private object Keys {
        val DEFAULT_ROUND_TYPE = stringPreferencesKey("default_round_type")
        val DEFAULT_END_COUNT = intPreferencesKey("default_end_count")
        val DEFAULT_ARROWS_PER_END = intPreferencesKey("default_arrows_per_end")
        val DEFAULT_DISTANCE_M = intPreferencesKey("default_distance_m")
        val DEFAULT_DISCIPLINE = stringPreferencesKey("default_discipline")
        val COUNT_X_RINGS = booleanPreferencesKey("count_x_rings_deeply")

        val LOCAL_USER_ID = stringPreferencesKey("local_user_id")
    }

    override fun observePreferences(userId: String): Flow<UserPreferences> =
        store.data.map { prefs ->
            UserPreferences(
                defaultRoundType = safeRoundType(prefs[Keys.DEFAULT_ROUND_TYPE]),
                defaultEndCount = prefs[Keys.DEFAULT_END_COUNT] ?: UserPreferences().defaultEndCount,
                defaultArrowsPerEnd = prefs[Keys.DEFAULT_ARROWS_PER_END] ?: UserPreferences().defaultArrowsPerEnd,
                defaultDistanceM = prefs[Keys.DEFAULT_DISTANCE_M] ?: UserPreferences().defaultDistanceM,
                defaultDiscipline = safeDiscipline(prefs[Keys.DEFAULT_DISCIPLINE]),
                countXRingsDeeply = prefs[Keys.COUNT_X_RINGS] ?: false,
            )
        }

    override suspend fun updatePreferences(userId: String, prefs: UserPreferences) {
        store.edit { p ->
            p[Keys.DEFAULT_ROUND_TYPE] = prefs.defaultRoundType.name
            p[Keys.DEFAULT_END_COUNT] = prefs.defaultEndCount
            p[Keys.DEFAULT_ARROWS_PER_END] = prefs.defaultArrowsPerEnd
            p[Keys.DEFAULT_DISTANCE_M] = prefs.defaultDistanceM
            p[Keys.DEFAULT_DISCIPLINE] = prefs.defaultDiscipline.name
            p[Keys.COUNT_X_RINGS] = prefs.countXRingsDeeply
        }
    }

    fun observeLocalUserId(): Flow<String?> =
        store.data.map { it[Keys.LOCAL_USER_ID] }

    suspend fun ensureLocalUserId(): String {
        var current: String? = null
        store.edit { p ->
            current = p[Keys.LOCAL_USER_ID]
            if (current == null) {
                val id = java.util.UUID.randomUUID().toString()
                p[Keys.LOCAL_USER_ID] = id
                current = id
            }
        }
        return current!!
    }

    private fun safeRoundType(name: String?): RoundType =
        name?.let { runCatching { RoundType.valueOf(it) }.getOrNull() }
            ?: UserPreferences().defaultRoundType

    private fun safeDiscipline(name: String?): Discipline =
        name?.let { runCatching { Discipline.valueOf(it) }.getOrNull() }
            ?: UserPreferences().defaultDiscipline
}