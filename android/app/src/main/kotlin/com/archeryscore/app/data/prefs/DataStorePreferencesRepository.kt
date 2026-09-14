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
    }

    override fun observePreferences(): Flow<UserPreferences> =
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

    override suspend fun updatePreferences(prefs: UserPreferences) {
        store.edit { p ->
            p[Keys.DEFAULT_ROUND_TYPE] = prefs.defaultRoundType.name
            p[Keys.DEFAULT_END_COUNT] = prefs.defaultEndCount
            p[Keys.DEFAULT_ARROWS_PER_END] = prefs.defaultArrowsPerEnd
            p[Keys.DEFAULT_DISTANCE_M] = prefs.defaultDistanceM
            p[Keys.DEFAULT_DISCIPLINE] = prefs.defaultDiscipline.name
            p[Keys.COUNT_X_RINGS] = prefs.countXRingsDeeply
        }
    }

    private fun safeRoundType(name: String?): RoundType =
        name?.let { runCatching { RoundType.valueOf(it) }.getOrNull() }
            ?: UserPreferences().defaultRoundType

    private fun safeDiscipline(name: String?): Discipline =
        name?.let { runCatching { Discipline.valueOf(it) }.getOrNull() }
            ?: UserPreferences().defaultDiscipline
}