package com.chapelotas.app.domain.repositories

import com.chapelotas.app.domain.models.AppSettings
import kotlinx.coroutines.flow.Flow

/**
 * Repositorio para gestionar las preferencias
 * Define el contrato sin exponer detalles de implementación
 */
interface PreferencesRepository {

    suspend fun isFirstTimeUser(): Boolean
    suspend fun isTodayInitialized(date: String): Boolean
    suspend fun setTodayInitialized(date: String)
    suspend fun areAlarmsConfigured(): Boolean
    suspend fun setAlarmsConfigured(configured: Boolean)
    suspend fun getLastSuccessfulRun(): Long?
    suspend fun setLastSuccessfulRun(timestamp: Long)

    // --- INICIO DE LA CORRECCIÓN ---
    // Este es el contrato correcto:
    // 1. Un método para OBSERVAR los cambios en la configuración.
    fun observeAppSettings(): Flow<AppSettings>
    // 2. Un método para GUARDAR la nueva configuración.
    suspend fun saveAppSettings(settings: AppSettings)
    // --- FIN DE LA CORRECCIÓN ---
}