package com.chapelotas.app.data

import android.content.Context
import android.content.SharedPreferences
import com.chapelotas.app.domain.entities.UserPreferences
import com.chapelotas.app.domain.repositories.PreferencesRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalTime
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementación del repositorio de preferencias usando SharedPreferences
 */
@Singleton
class PreferencesRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : PreferencesRepository {

    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    private val _preferencesFlow = MutableStateFlow(loadPreferences())

    companion object {
        private const val PREFS_NAME = "chapelotas_preferences"

        // Keys
        private const val KEY_DAILY_SUMMARY_HOUR = "daily_summary_hour"
        private const val KEY_DAILY_SUMMARY_MINUTE = "daily_summary_minute"
        private const val KEY_TOMORROW_SUMMARY_HOUR = "tomorrow_summary_hour"
        private const val KEY_TOMORROW_SUMMARY_MINUTE = "tomorrow_summary_minute"
        private const val KEY_SARCASTIC_MODE = "sarcastic_mode"
        private const val KEY_CRITICAL_ALERT_SOUND = "critical_alert_sound"
        private const val KEY_NOTIFICATION_SOUND = "notification_sound"
        private const val KEY_REMINDER_MINUTES = "reminder_minutes_before"
        private const val KEY_PRIVACY_ACCEPTED = "privacy_policy_accepted"
        private const val KEY_FIRST_TIME_USER = "is_first_time_user"
        private const val KEY_PREFERRED_CALENDARS = "preferred_calendars"
    }

    override suspend fun getUserPreferences(): UserPreferences {
        return loadPreferences()
    }

    override fun observeUserPreferences(): Flow<UserPreferences> {
        return _preferencesFlow.asStateFlow()
    }

    override suspend fun updateUserPreferences(preferences: UserPreferences) {
        savePreferences(preferences)
        _preferencesFlow.value = preferences
    }

    override suspend fun updateDailySummaryTime(hour: Int, minute: Int) {
        val current = loadPreferences()
        val updated = current.copy(
            dailySummaryTime = LocalTime.of(hour, minute)
        )
        savePreferences(updated)
        _preferencesFlow.value = updated
    }

    override suspend fun updateTomorrowSummaryTime(hour: Int, minute: Int) {
        val current = loadPreferences()
        val updated = current.copy(
            tomorrowSummaryTime = LocalTime.of(hour, minute)
        )
        savePreferences(updated)
        _preferencesFlow.value = updated
    }

    override suspend fun setSarcasticMode(enabled: Boolean) {
        val current = loadPreferences()
        val updated = current.copy(isSarcasticModeEnabled = enabled)
        savePreferences(updated)
        _preferencesFlow.value = updated
    }

    override suspend fun updatePreferredCalendars(calendarIds: Set<Long>) {
        val current = loadPreferences()
        val updated = current.copy(preferredCalendars = calendarIds)
        savePreferences(updated)
        _preferencesFlow.value = updated
    }

    override suspend fun acceptPrivacyPolicy() {
        val current = loadPreferences()
        val updated = current.copy(hasAcceptedPrivacyPolicy = true)
        savePreferences(updated)
        _preferencesFlow.value = updated
    }

    override suspend fun markAsExperiencedUser() {
        val current = loadPreferences()
        val updated = current.copy(isFirstTimeUser = false)
        savePreferences(updated)
        _preferencesFlow.value = updated
    }

    override suspend fun updateCriticalAlertSound(soundUri: String) {
        val current = loadPreferences()
        val updated = current.copy(criticalAlertSound = soundUri)
        savePreferences(updated)
        _preferencesFlow.value = updated
    }

    override suspend fun updateNotificationSound(soundUri: String) {
        val current = loadPreferences()
        val updated = current.copy(notificationSound = soundUri)
        savePreferences(updated)
        _preferencesFlow.value = updated
    }

    override suspend fun updateReminderMinutesBefore(minutes: Int) {
        val current = loadPreferences()
        val updated = current.copy(minutesBeforeEventForReminder = minutes)
        savePreferences(updated)
        _preferencesFlow.value = updated
    }

    override suspend fun resetToDefaults() {
        val defaults = UserPreferences()
        savePreferences(defaults)
        _preferencesFlow.value = defaults
    }

    override suspend fun isFirstTimeUser(): Boolean {
        return prefs.getBoolean(KEY_FIRST_TIME_USER, true)
    }

    /**
     * Carga las preferencias desde SharedPreferences
     */
    private fun loadPreferences(): UserPreferences {
        return UserPreferences(
            dailySummaryTime = LocalTime.of(
                prefs.getInt(KEY_DAILY_SUMMARY_HOUR, 7),
                prefs.getInt(KEY_DAILY_SUMMARY_MINUTE, 0)
            ),
            tomorrowSummaryTime = LocalTime.of(
                prefs.getInt(KEY_TOMORROW_SUMMARY_HOUR, 20),
                prefs.getInt(KEY_TOMORROW_SUMMARY_MINUTE, 0)
            ),
            isSarcasticModeEnabled = prefs.getBoolean(KEY_SARCASTIC_MODE, false),
            criticalAlertSound = prefs.getString(KEY_CRITICAL_ALERT_SOUND, "default_ringtone") ?: "default_ringtone",
            notificationSound = prefs.getString(KEY_NOTIFICATION_SOUND, "default_notification") ?: "default_notification",
            minutesBeforeEventForReminder = prefs.getInt(KEY_REMINDER_MINUTES, 15),
            hasAcceptedPrivacyPolicy = prefs.getBoolean(KEY_PRIVACY_ACCEPTED, false),
            isFirstTimeUser = prefs.getBoolean(KEY_FIRST_TIME_USER, true),
            preferredCalendars = prefs.getStringSet(KEY_PREFERRED_CALENDARS, emptySet())
                ?.mapNotNull { it.toLongOrNull() }
                ?.toSet() ?: emptySet()
        )
    }

    /**
     * Guarda las preferencias en SharedPreferences
     */
    private fun savePreferences(preferences: UserPreferences) {
        prefs.edit().apply {
            putInt(KEY_DAILY_SUMMARY_HOUR, preferences.dailySummaryTime.hour)
            putInt(KEY_DAILY_SUMMARY_MINUTE, preferences.dailySummaryTime.minute)
            putInt(KEY_TOMORROW_SUMMARY_HOUR, preferences.tomorrowSummaryTime.hour)
            putInt(KEY_TOMORROW_SUMMARY_MINUTE, preferences.tomorrowSummaryTime.minute)
            putBoolean(KEY_SARCASTIC_MODE, preferences.isSarcasticModeEnabled)
            putString(KEY_CRITICAL_ALERT_SOUND, preferences.criticalAlertSound)
            putString(KEY_NOTIFICATION_SOUND, preferences.notificationSound)
            putInt(KEY_REMINDER_MINUTES, preferences.minutesBeforeEventForReminder)
            putBoolean(KEY_PRIVACY_ACCEPTED, preferences.hasAcceptedPrivacyPolicy)
            putBoolean(KEY_FIRST_TIME_USER, preferences.isFirstTimeUser)
            putStringSet(KEY_PREFERRED_CALENDARS, preferences.preferredCalendars.map { it.toString() }.toSet())
            apply()
        }
    }
}