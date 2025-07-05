package com.chapelotas.app.domain.usecases

import com.chapelotas.app.domain.repositories.CalendarRepository
import com.chapelotas.app.domain.repositories.NotificationRepository
import com.chapelotas.app.domain.repositories.PreferencesRepository
import javax.inject.Inject

/**
 * Caso de uso para la configuración inicial de la app
 * Se ejecuta la primera vez que el usuario abre Chapelotas
 */
class SetupInitialConfigurationUseCase @Inject constructor(
    private val calendarRepository: CalendarRepository,
    private val notificationRepository: NotificationRepository,
    private val preferencesRepository: PreferencesRepository
) {
    suspend operator fun invoke(): Result<SetupResult> {
        return try {
            // 1. Verificar si es primera vez
            val isFirstTime = preferencesRepository.isFirstTimeUser()
            if (!isFirstTime) {
                return Result.success(SetupResult(
                    isFirstTimeSetup = false,
                    availableCalendars = emptyMap(),
                    requiresPermissions = false
                ))
            }

            // 2. Obtener calendarios disponibles
            val calendars = try {
                calendarRepository.getAvailableCalendars()
            } catch (e: SecurityException) {
                // No tiene permisos de calendario
                return Result.success(SetupResult(
                    isFirstTimeSetup = true,
                    availableCalendars = emptyMap(),
                    requiresPermissions = true
                ))
            }

            // 3. Iniciar servicio de notificaciones
            if (!notificationRepository.isNotificationServiceRunning()) {
                notificationRepository.startNotificationService()
            }

            // 4. Si hay calendarios, seleccionar todos por defecto
            if (calendars.isNotEmpty()) {
                val preferences = preferencesRepository.getUserPreferences()
                preferencesRepository.updateUserPreferences(
                    preferences.copy(
                        preferredCalendars = calendars.keys.toSet()
                    )
                )
            }

            Result.success(SetupResult(
                isFirstTimeSetup = true,
                availableCalendars = calendars,
                requiresPermissions = false
            ))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

/**
 * Resultado de la configuración inicial
 */
data class SetupResult(
    val isFirstTimeSetup: Boolean,
    val availableCalendars: Map<Long, String>,
    val requiresPermissions: Boolean
)