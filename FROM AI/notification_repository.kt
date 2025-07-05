package com.chapelotas.app.domain.repositories

import com.chapelotas.app.domain.entities.ChapelotasNotification
import kotlinx.coroutines.flow.Flow

/**
 * Repositorio para gestionar las notificaciones de Chapelotas
 */
interface NotificationRepository {
    
    /**
     * Programa una notificación
     */
    suspend fun scheduleNotification(notification: ChapelotasNotification)
    
    /**
     * Programa múltiples notificaciones
     */
    suspend fun scheduleMultipleNotifications(notifications: List<ChapelotasNotification>)
    
    /**
     * Cancela una notificación programada
     */
    suspend fun cancelNotification(notificationId: String)
    
    /**
     * Cancela todas las notificaciones de un evento específico
     */
    suspend fun cancelNotificationsForEvent(eventId: Long)
    
    /**
     * Obtiene todas las notificaciones programadas pendientes
     */
    suspend fun getPendingNotifications(): List<ChapelotasNotification>
    
    /**
     * Obtiene las notificaciones de un evento específico
     */
    suspend fun getNotificationsForEvent(eventId: Long): List<ChapelotasNotification>
    
    /**
     * Marca una notificación como mostrada
     */
    suspend fun markAsShown(notificationId: String)
    
    /**
     * Limpia notificaciones antiguas (ya mostradas y con más de X días)
     */
    suspend fun cleanOldNotifications(daysToKeep: Int = 7)
    
    /**
     * Observa el estado de las notificaciones
     */
    fun observeNotifications(): Flow<List<ChapelotasNotification>>
    
    /**
     * Muestra una notificación inmediatamente (para alertas críticas)
     */
    suspend fun showImmediateNotification(notification: ChapelotasNotification)
    
    /**
     * Verifica si el servicio de notificaciones está activo
     */
    suspend fun isNotificationServiceRunning(): Boolean
    
    /**
     * Inicia el servicio de notificaciones persistente
     */
    suspend fun startNotificationService()
    
    /**
     * Detiene el servicio de notificaciones
     */
    suspend fun stopNotificationService()
}