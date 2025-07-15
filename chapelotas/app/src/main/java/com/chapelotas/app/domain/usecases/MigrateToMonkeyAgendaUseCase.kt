package com.chapelotas.app.domain.usecases

import android.util.Log
import com.chapelotas.app.data.database.ChapelotasDatabase
import java.time.LocalDate
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Migra los eventos existentes al nuevo sistema MonkeyAgenda
 * Se ejecuta una sola vez al actualizar
 */
@Singleton
class MigrateToMonkeyAgendaUseCase @Inject constructor(
    private val database: ChapelotasDatabase,
    private val monkeyAgendaService: MonkeyAgendaService
) {
    companion object {
        private const val TAG = "MigrateToAgenda"
        private const val PREF_KEY = "monkey_agenda_migrated"
    }

    suspend operator fun invoke(): Boolean {
        try {
            Log.d(TAG, "Iniciando migración a MonkeyAgenda")

            // 1. Obtener todos los eventos de hoy
            val todayEvents = database.eventPlanDao().getEventsByDate(LocalDate.now())
            var migratedCount = 0

            todayEvents.forEach { event ->
                // Solo migrar eventos pendientes
                if (event.resolutionStatus.name != "COMPLETED") {
                    val now = LocalDateTime.now(ZoneId.systemDefault())

                    // Programar notificaciones según la distancia del evento
                    val notificationTimes = event.getNotificationMinutesList()

                    notificationTimes.forEach { minutesBefore ->
                        val notificationTime = event.startTime.minusMinutes(minutesBefore.toLong())

                        // Solo programar si es futuro
                        if (notificationTime.isAfter(now)) {
                            monkeyAgendaService.scheduleAction(
                                scheduledTime = notificationTime,
                                actionType = "NOTIFY_EVENT",
                                eventId = event.eventId
                            )
                            migratedCount++
                        }
                    }
                }
            }

            // 2. Programar resumen diario para mañana a las 7 AM
            val tomorrowSummaryTime = LocalDate.now()
                .plusDays(1)
                .atTime(7, 0)

            monkeyAgendaService.scheduleAction(
                scheduledTime = tomorrowSummaryTime,
                actionType = "DAILY_SUMMARY"
            )

            // 3. Programar limpieza semanal
            val weeklyCleanupTime = LocalDate.now()
                .plusDays(7)
                .atTime(3, 0) // 3 AM del próximo domingo

            monkeyAgendaService.scheduleAction(
                scheduledTime = weeklyCleanupTime,
                actionType = "CLEANUP"
            )

            Log.d(TAG, "Migración completada. $migratedCount notificaciones programadas")

            // 4. Limpiar las tablas viejas de notificaciones
            cleanupOldNotifications()

            return true

        } catch (e: Exception) {
            Log.e(TAG, "Error durante migración", e)
            return false
        }
    }

    private suspend fun cleanupOldNotifications() {
        try {
            // Marcar todas las notificaciones viejas como ejecutadas
            val oldNotifications = database.notificationDao().getPendingNotifications(LocalDateTime.now(ZoneId.systemDefault()))

            oldNotifications.forEach { notification ->
                database.notificationDao().markAsExecuted(notification.notificationId)
            }

            Log.d(TAG, "Limpiadas ${oldNotifications.size} notificaciones viejas")

        } catch (e: Exception) {
            Log.e(TAG, "Error limpiando notificaciones viejas", e)
        }
    }
}