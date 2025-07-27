package com.chapelotas.app.domain.repositories

import com.chapelotas.app.domain.entities.ChapelotasNotification
import java.time.LocalDateTime

/**
 * Define el contrato para todo lo relacionado con las notificaciones al usuario.
 */
interface NotificationRepository {
    /**
     * Muestra una notificación de forma inmediata.
     */
    suspend fun showImmediateNotification(notification: ChapelotasNotification)

    /**
     * Programa una alarma de alta precisión en el sistema para despertar el dispositivo
     * y entregar un recordatorio en un momento exacto.
     * @param taskId El ID de la tarea para la cual es el recordatorio.
     * @param triggerAt La fecha y hora exactas en que la alarma debe sonar.
     */
    fun scheduleExactReminder(taskId: String, triggerAt: LocalDateTime)

    /**
     * Cancela una alarma programada para un recordatorio.
     * @param taskId El ID de la tarea cuya alarma se debe cancelar.
     */
    fun cancelReminder(taskId: String)
}