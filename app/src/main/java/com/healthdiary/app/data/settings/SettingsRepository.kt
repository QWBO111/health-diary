package com.healthdiary.app.data.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {

    private object Keys {
        val REMINDER_ENABLED = booleanPreferencesKey("reminder_enabled")
        val REMINDER_HOUR = intPreferencesKey("reminder_hour")
        val REMINDER_MINUTE = intPreferencesKey("reminder_minute")
        val HEIGHT_CM = intPreferencesKey("height_cm")
    }

    val reminderEnabled: Flow<Boolean> =
        context.settingsDataStore.data.map { it[Keys.REMINDER_ENABLED] ?: false }

    val reminderTime: Flow<Pair<Int, Int>> =
        context.settingsDataStore.data.map {
            (it[Keys.REMINDER_HOUR] ?: 21) to (it[Keys.REMINDER_MINUTE] ?: 0)
        }

    val heightCm: Flow<Int> =
        context.settingsDataStore.data.map { it[Keys.HEIGHT_CM] ?: 0 }

    suspend fun setReminderEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.REMINDER_ENABLED] = enabled }
    }

    suspend fun setReminderTime(hour: Int, minute: Int) {
        context.settingsDataStore.edit {
            it[Keys.REMINDER_HOUR] = hour
            it[Keys.REMINDER_MINUTE] = minute
        }
    }

    suspend fun setHeightCm(cm: Int) {
        context.settingsDataStore.edit { it[Keys.HEIGHT_CM] = cm }
    }
}
