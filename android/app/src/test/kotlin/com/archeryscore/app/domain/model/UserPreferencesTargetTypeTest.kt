package com.archeryscore.app.domain.model

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class UserPreferencesTargetTypeTest {

    @Test
    fun `default target type is CM122`() {
        val prefs = UserPreferences()
        assertEquals(TargetType.CM122, prefs.defaultTargetType)
    }

    @Test
    fun `target type round trips through name`() = runTest {
        for (targetType in TargetType.entries) {
            val prefs = UserPreferences(defaultTargetType = targetType)
            assertEquals(targetType, UserPreferences(defaultTargetType = TargetType.valueOf(targetType.name)).defaultTargetType)
        }
    }
}
