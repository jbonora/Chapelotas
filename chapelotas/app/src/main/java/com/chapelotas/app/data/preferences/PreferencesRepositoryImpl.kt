package com.chapelotas.app.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.chapelotas.app.domain.repositories.PreferencesRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "chapelotas_preferences")

@Singleton
class PreferencesRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : PreferencesRepository {

    private object PreferenceKeys {
        val FIRST_TIME_LAUNCH = booleanPreferencesKey("first_time_launch")
        val SARCASTIC_MODE = booleanPreferencesKey("sarcastic_mode")
        val IS_24H_MODE = booleanPreferencesKey("is_24h_mode")
        val USER_NAME = stringPreferencesKey("user_name")
        val MONKEY_AGENDA_MIGRATED = booleanPreferencesKey("monkey_agenda_migrated")
        val LAST_SYNC_TIME = longPreferencesKey("last_sync_time")
    }

    override val isFirstTimeLaunch: Flow<Boolean> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[PreferenceKeys.FIRST_TIME_LAUNCH] ?: true
        }

    override val sarcasticMode: Flow<Boolean> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[PreferenceKeys.SARCASTIC_MODE] ?: true
        }

    override val is24hMode: Flow<Boolean> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[PreferenceKeys.IS_24H_MODE] ?: true
        }

    override val userName: Flow<String?> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[PreferenceKeys.USER_NAME]
        }

    override suspend fun setFirstTimeLaunch(value: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferenceKeys.FIRST_TIME_LAUNCH] = value
        }
    }

    override suspend fun setSarcasticMode(value: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferenceKeys.SARCASTIC_MODE] = value
        }
    }

    override suspend fun setIs24hMode(value: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferenceKeys.IS_24H_MODE] = value
        }
    }

    override suspend fun setUserName(value: String?) {
        context.dataStore.edit { preferences ->
            if (value != null) {
                preferences[PreferenceKeys.USER_NAME] = value
            } else {
                preferences.remove(PreferenceKeys.USER_NAME)
            }
        }
    }

    override suspend fun isMonkeyAgendaMigrated(): Boolean {
        return context.dataStore.data
            .map { preferences ->
                preferences[PreferenceKeys.MONKEY_AGENDA_MIGRATED] ?: false
            }
            .catch { false }
            .collect { return@collect it }
    }

    override suspend fun setMonkeyAgendaMigrated(value: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferenceKeys.MONKEY_AGENDA_MIGRATED] = value
        }
    }

    override suspend fun getLastSyncTime(): Long {
        return context.dataStore.data
            .map { preferences ->
                preferences[PreferenceKeys.LAST_SYNC_TIME] ?: 0L
            }
            .catch { 0L }
            .collect { return@collect it }
    }

    override suspend fun setLastSyncTime(value: Long) {
        context.dataStore.edit { preferences ->
            preferences[PreferenceKeys.LAST_SYNC_TIME] = value
        }
    }

    override suspend fun clearAll() {
        context.dataStore.edit { preferences ->
            preferences.clear()
        }
    }
}