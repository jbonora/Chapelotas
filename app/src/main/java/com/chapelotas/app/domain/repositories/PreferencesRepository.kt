package com.chapelotas.app.domain.repositories

import com.chapelotas.app.domain.models.AppSettings
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime

/**
 * Define el contrato para gestionar las preferencias y configuraciones de la aplicación.
 * Abstrae los detalles de cómo y dónde se guarda la configuración (ej. SharedPreferences).
 */
interface PreferencesRepository {

    /**
     * Observa los cambios en la configuración de la aplicación en tiempo real.
     * @return Un Flow que emite el objeto AppSettings cada vez que cambia.
     */
    fun observeAppSettings(): Flow<AppSettings>

    /**
     * Guarda un nuevo objeto de configuración.
     * @param settings El nuevo objeto AppSettings a persistir.
     */
    suspend fun saveAppSettings(settings: AppSettings)

    // --- Otras preferencias específicas ---

    suspend fun isFirstTimeUser(): Boolean
    suspend fun isTodayInitialized(date: String): Boolean
    suspend fun setTodayInitialized(date: String)
    suspend fun areAlarmsConfigured(): Boolean
    suspend fun setAlarmsConfigured(configured: Boolean)
    suspend fun getLastSuccessfulRun(): Long?
    suspend fun setLastSuccessfulRun(timestamp: Long)

    // --- FUNCIONES AÑADIDAS ---
    suspend fun setFinalHuaweiAlarmTime(dateTime: LocalDateTime)
    fun getFinalHuaweiAlarmTime(): Flow<LocalDateTime?>
    suspend fun clearFinalHuaweiAlarmTime()
    // --- FIN DE FUNCIONES AÑADIDAS ---
}