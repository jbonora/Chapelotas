package com.chapelotas.app.domain.repositories

import com.chapelotas.app.domain.entities.UserPreferences
import kotlinx.coroutines.flow.Flow

/**
 * Repositorio para gestionar las preferencias del usuario
 */
interface PreferencesRepository {

    /**
     * Obtiene las preferencias actuales del usuario
     */
    suspend fun getUserPreferences(): UserPreferences

    /**
     * Observa cambios en las preferencias (Flow para actualizaciones en tiempo real)
     */
    fun observeUserPreferences(): Flow<UserPreferences>

    /**
     * Actualiza todas las preferencias
     */
    suspend fun updateUserPreferences(preferences: UserPreferences)

    /**
     * Actualiza el horario del resumen diario
     */
    suspend fun updateDailySummaryTime(hour: Int, minute: Int)

    /**
     * Actualiza el horario del resumen de mañana
     */
    suspend fun updateTomorrowSummaryTime(hour: Int, minute: Int)

    /**
     * Activa/desactiva el modo sarcástico
     */
    suspend fun setSarcasticMode(enabled: Boolean)

    /**
     * Actualiza los calendarios preferidos
     */
    suspend fun updatePreferredCalendars(calendarIds: Set<Long>)

    /**
     * Marca que el usuario aceptó la política de privacidad
     */
    suspend fun acceptPrivacyPolicy()

    /**
     * Marca que el usuario ya no es primera vez
     */
    suspend fun markAsExperiencedUser()

    /**
     * Actualiza el sonido de alerta crítica
     */
    suspend fun updateCriticalAlertSound(soundUri: String)

    /**
     * Actualiza el sonido de notificación normal
     */
    suspend fun updateNotificationSound(soundUri: String)

    /**
     * Actualiza minutos antes del evento para recordatorio
     */
    suspend fun updateReminderMinutesBefore(minutes: Int)

    /**
     * Resetea todas las preferencias a valores por defecto
     */
    suspend fun resetToDefaults()

    /**
     * Verifica si es la primera vez que se ejecuta la app
     */
    suspend fun isFirstTimeUser(): Boolean
}