package com.chapelotas.app.domain.entities

import java.time.LocalTime

/**
 * Preferencias del usuario para Chapelotas
 */
data class UserPreferences(
    val dailySummaryTime: LocalTime = LocalTime.of(7, 0),  // 7:00 AM
    val tomorrowSummaryTime: LocalTime = LocalTime.of(20, 0),  // 8:00 PM
    val isSarcasticModeEnabled: Boolean = false,
    val criticalAlertSound: String = "default_ringtone",
    val notificationSound: String = "default_notification",
    val minutesBeforeEventForReminder: Int = 15,
    val hasAcceptedPrivacyPolicy: Boolean = false,
    val isFirstTimeUser: Boolean = true
    // El campo "preferredCalendars" ha sido eliminado.
) {
    /**
     * Verifica si es hora del resumen diario
     */
    fun isTimeForDailySummary(): Boolean {
        val now = LocalTime.now()
        return now.hour == dailySummaryTime.hour &&
                now.minute >= dailySummaryTime.minute &&
                now.minute < dailySummaryTime.minute + 5
    }

    /**
     * Verifica si es hora del resumen de mañana
     */
    fun isTimeForTomorrowSummary(): Boolean {
        val now = LocalTime.now()
        return now.hour == tomorrowSummaryTime.hour &&
                now.minute >= tomorrowSummaryTime.minute &&
                now.minute < tomorrowSummaryTime.minute + 5
    }
}