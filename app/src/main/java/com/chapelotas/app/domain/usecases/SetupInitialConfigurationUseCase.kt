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
            // 1. Verificar si es la primera vez que el usuario corre la app.
            val isFirstTime = preferencesRepository.isFirstTimeUser()
            if (!isFirstTime) {
                // Si no es la primera vez, simplemente devolvemos que no se necesita setup.
                // La MainActivity se encargará de verificar los permisos por su cuenta.
                return Result.success(SetupResult(
                    isFirstTimeSetup = false,
                    requiresPermissions = false
                ))
            }

            // 2. Si es la primera vez, intentamos acceder al calendario.
            // Esto no es para obtener datos, sino para ver si el sistema nos lanza un error de permisos.
            try {
                calendarRepository.getAvailableCalendars()
            } catch (e: SecurityException) {
                // Si falla porque no hay permisos, le informamos a la UI que los necesita.
                return Result.success(SetupResult(
                    isFirstTimeSetup = true,
                    requiresPermissions = true
                ))
            }

            // 3. Si llegamos aquí, significa que SÍ teníamos permisos desde el principio (raro, pero posible).
            // Iniciamos el servicio de notificaciones si no está corriendo.
            if (!notificationRepository.isNotificationServiceRunning()) {
                notificationRepository.startNotificationService()
            }

            // Devolvemos que el setup de primera vez se completó y no se requieren más permisos.
            Result.success(SetupResult(
                isFirstTimeSetup = true,
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
    val requiresPermissions: Boolean
)