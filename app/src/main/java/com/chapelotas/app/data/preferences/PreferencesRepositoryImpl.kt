package com.chapelotas.app.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.chapelotas.app.domain.entities.UserPreferences
import com.chapelotas.app.domain.repositories.PreferencesRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException
import java.time.LocalTime
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "chapelotas_preferences")

@Singleton
class PreferencesRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : PreferencesRepository {

    private object PreferenceKeys {
        val DAILY_SUMMARY_HOUR = intPreferencesKey("daily_summary_hour")
        val DAILY_SUMMARY_MINUTE = intPreferencesKey("daily_summary_minute")
        val TOMORROW_SUMMARY_HOUR = intPreferencesKey("tomorrow_summary_hour")
        val TOMORROW_SUMMARY_MINUTE = intPreferencesKey("tomorrow_summary_minute")
        val SARCASTIC_MODE = booleanPreferencesKey("sarcastic_mode")
        val PRIVACY_POLICY_ACCEPTED = booleanPreferencesKey("privacy_policy_accepted")
        val IS_FIRST_TIME_USER = booleanPreferencesKey("is_first_time_user")
        val CRITICAL_ALERT_SOUND = stringPreferencesKey("critical_alert_sound")
        val NOTIFICATION_SOUND = stringPreferencesKey("notification_sound")
        val REMINDER_MINUTES_BEFORE = intPreferencesKey("reminder_minutes_before")
    }

    override suspend fun getUserPreferences(): UserPreferences {
        return context.dataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }
            .map { preferences ->
                UserPreferences(
                    dailySummaryTime = LocalTime.of(
                        preferences[PreferenceKeys.DAILY_SUMMARY_HOUR] ?: 7,
                        preferences[PreferenceKeys.DAILY_SUMMARY_MINUTE] ?: 0
                    ),
                    tomorrowSummaryTime = LocalTime.of(
                        preferences[PreferenceKeys.TOMORROW_SUMMARY_HOUR] ?: 20,
                        preferences[PreferenceKeys.TOMORROW_SUMMARY_MINUTE] ?: 0
                    ),
                    isSarcasticModeEnabled = preferences[PreferenceKeys.SARCASTIC_MODE] ?: false,
                    hasAcceptedPrivacyPolicy = preferences[PreferenceKeys.PRIVACY_POLICY_ACCEPTED] ?: false,
                    isFirstTimeUser = preferences[PreferenceKeys.IS_FIRST_TIME_USER] ?: true,
                    criticalAlertSound = preferences[PreferenceKeys.CRITICAL_ALERT_SOUND] ?: "default_ringtone",
                    notificationSound = preferences[PreferenceKeys.NOTIFICATION_SOUND] ?: "default_notification",
                    minutesBeforeEventForReminder = preferences[PreferenceKeys.REMINDER_MINUTES_BEFORE] ?: 15
                )
            }
            .first()
    }

    override fun observeUserPreferences(): Flow<UserPreferences> {
        return context.dataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }
            .map { preferences ->
                UserPreferences(
                    dailySummaryTime = LocalTime.of(
                        preferences[PreferenceKeys.DAILY_SUMMARY_HOUR] ?: 7,
                        preferences[PreferenceKeys.DAILY_SUMMARY_MINUTE] ?: 0
                    ),
                    tomorrowSummaryTime = LocalTime.of(
                        preferences[PreferenceKeys.TOMORROW_SUMMARY_HOUR] ?: 20,
                        preferences[PreferenceKeys.TOMORROW_SUMMARY_MINUTE] ?: 0
                    ),
                    isSarcasticModeEnabled = preferences[PreferenceKeys.SARCASTIC_MODE] ?: false,
                    hasAcceptedPrivacyPolicy = preferences[PreferenceKeys.PRIVACY_POLICY_ACCEPTED] ?: false,
                    isFirstTimeUser = preferences[PreferenceKeys.IS_FIRST_TIME_USER] ?: true,
                    criticalAlertSound = preferences[PreferenceKeys.CRITICAL_ALERT_SOUND] ?: "default_ringtone",
                    notificationSound = preferences[PreferenceKeys.NOTIFICATION_SOUND] ?: "default_notification",
                    minutesBeforeEventForReminder = preferences[PreferenceKeys.REMINDER_MINUTES_BEFORE] ?: 15
                )
            }
    }

    override suspend fun updateUserPreferences(preferences: UserPreferences) {
        context.dataStore.edit { prefs ->
            prefs[PreferenceKeys.DAILY_SUMMARY_HOUR] = preferences.dailySummaryTime.hour
            prefs[PreferenceKeys.DAILY_SUMMARY_MINUTE] = preferences.dailySummaryTime.minute
            prefs[PreferenceKeys.TOMORROW_SUMMARY_HOUR] = preferences.tomorrowSummaryTime.hour
            prefs[PreferenceKeys.TOMORROW_SUMMARY_MINUTE] = preferences.tomorrowSummaryTime.minute
            prefs[PreferenceKeys.SARCASTIC_MODE] = preferences.isSarcasticModeEnabled
            prefs[PreferenceKeys.PRIVACY_POLICY_ACCEPTED] = preferences.hasAcceptedPrivacyPolicy
            prefs[PreferenceKeys.IS_FIRST_TIME_USER] = preferences.isFirstTimeUser
            prefs[PreferenceKeys.CRITICAL_ALERT_SOUND] = preferences.criticalAlertSound
            prefs[PreferenceKeys.NOTIFICATION_SOUND] = preferences.notificationSound
            prefs[PreferenceKeys.REMINDER_MINUTES_BEFORE] = preferences.minutesBeforeEventForReminder
        }
    }

    override suspend fun updateDailySummaryTime(hour: Int, minute: Int) {
        context.dataStore.edit { preferences ->
            preferences[PreferenceKeys.DAILY_SUMMARY_HOUR] = hour
            preferences[PreferenceKeys.DAILY_SUMMARY_MINUTE] = minute
        }
    }

    override suspend fun updateTomorrowSummaryTime(hour: Int, minute: Int) {
        context.dataStore.edit { preferences ->
            preferences[PreferenceKeys.TOMORROW_SUMMARY_HOUR] = hour
            preferences[PreferenceKeys.TOMORROW_SUMMARY_MINUTE] = minute
        }
    }

    override suspend fun setSarcasticMode(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferenceKeys.SARCASTIC_MODE] = enabled
        }
    }

    override suspend fun acceptPrivacyPolicy() {
        context.dataStore.edit { preferences ->
            preferences[PreferenceKeys.PRIVACY_POLICY_ACCEPTED] = true
        }
    }

    override suspend fun markAsExperiencedUser() {
        context.dataStore.edit { preferences ->
            preferences[PreferenceKeys.IS_FIRST_TIME_USER] = false
        }
    }

    override suspend fun updateCriticalAlertSound(soundUri: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferenceKeys.CRITICAL_ALERT_SOUND] = soundUri
        }
    }

    override suspend fun updateNotificationSound(soundUri: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferenceKeys.NOTIFICATION_SOUND] = soundUri
        }
    }

    override suspend fun updateReminderMinutesBefore(minutes: Int) {
        context.dataStore.edit { preferences ->
            preferences[PreferenceKeys.REMINDER_MINUTES_BEFORE] = minutes
        }
    }

    override suspend fun resetToDefaults() {
        context.dataStore.edit { preferences ->
            preferences.clear()
            // Establecer valores por defecto
            preferences[PreferenceKeys.DAILY_SUMMARY_HOUR] = 7
            preferences[PreferenceKeys.DAILY_SUMMARY_MINUTE] = 0
            preferences[PreferenceKeys.TOMORROW_SUMMARY_HOUR] = 20
            preferences[PreferenceKeys.TOMORROW_SUMMARY_MINUTE] = 0
            preferences[PreferenceKeys.SARCASTIC_MODE] = false
            preferences[PreferenceKeys.REMINDER_MINUTES_BEFORE] = 15
            preferences[PreferenceKeys.IS_FIRST_TIME_USER] = false
            preferences[PreferenceKeys.PRIVACY_POLICY_ACCEPTED] = true
            preferences[PreferenceKeys.CRITICAL_ALERT_SOUND] = "default_ringtone"
            preferences[PreferenceKeys.NOTIFICATION_SOUND] = "default_notification"
        }
    }

    override suspend fun isFirstTimeUser(): Boolean {
        return context.dataStore.data
            .map { preferences ->
                preferences[PreferenceKeys.IS_FIRST_TIME_USER] ?: true
            }
            .catch { true }
            .first()
    }
}