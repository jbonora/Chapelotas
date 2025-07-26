package com.chapelotas.app.data.preferences

import android.content.Context
import com.chapelotas.app.domain.models.AppSettings
import com.chapelotas.app.domain.repositories.PreferencesRepository
import com.google.gson.Gson
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PreferencesRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : PreferencesRepository {

    private val prefs = context.getSharedPreferences("chapelotas_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    companion object {
        private const val KEY_APP_SETTINGS = "app_settings"
        private const val KEY_FIRST_TIME = "is_first_time_user"
        private const val KEY_TODAY_INITIALIZED_PREFIX = "today_initialized_"
        private const val KEY_ALARMS_CONFIGURED = "alarms_configured"
        private const val KEY_LAST_SUCCESSFUL_RUN = "last_successful_run"
    }

    // --- INICIO DE LA CORRECCIÓN ---

    // 1. Creamos un "estado" interno que carga la configuración inicial.
    private val _appSettings = MutableStateFlow(loadInitialSettings())

    private fun loadInitialSettings(): AppSettings {
        val json = prefs.getString(KEY_APP_SETTINGS, null)
        return if (json != null) {
            try {
                gson.fromJson(json, AppSettings::class.java)
            } catch (e: Exception) {
                AppSettings()
            }
        } else {
            AppSettings()
        }
    }

    // 2. Esta función cumple el contrato "observe": devuelve el estado como un Flow.
    override fun observeAppSettings(): Flow<AppSettings> {
        return _appSettings.asStateFlow()
    }

    // 3. Esta función cumple el contrato "save": actualiza el estado y guarda en disco.
    override suspend fun saveAppSettings(settings: AppSettings) {
        // Actualizamos el estado en memoria para que todos los observadores (como la UI) reaccionen al instante.
        _appSettings.value = settings
        // Guardamos en el disco de forma asíncrona.
        withContext(Dispatchers.IO) {
            val json = gson.toJson(settings)
            prefs.edit().putString(KEY_APP_SETTINGS, json).apply()
        }
    }

    // --- FIN DE LA CORRECCIÓN ---


    override suspend fun isFirstTimeUser(): Boolean {
        return prefs.getBoolean(KEY_FIRST_TIME, true)
    }

    override suspend fun isTodayInitialized(date: String): Boolean {
        return prefs.getBoolean("$KEY_TODAY_INITIALIZED_PREFIX$date", false)
    }

    override suspend fun setTodayInitialized(date: String) {
        prefs.edit().putBoolean("$KEY_TODAY_INITIALIZED_PREFIX$date", true).apply()
    }

    override suspend fun areAlarmsConfigured(): Boolean {
        return prefs.getBoolean(KEY_ALARMS_CONFIGURED, false)
    }

    override suspend fun setAlarmsConfigured(configured: Boolean) {
        prefs.edit().putBoolean(KEY_ALARMS_CONFIGURED, configured).apply()
    }

    override suspend fun getLastSuccessfulRun(): Long? {
        val timestamp = prefs.getLong(KEY_LAST_SUCCESSFUL_RUN, -1)
        return if (timestamp == -1L) null else timestamp
    }

    override suspend fun setLastSuccessfulRun(timestamp: Long) {
        prefs.edit()
            .putLong(KEY_LAST_SUCCESSFUL_RUN, timestamp)
            .putBoolean(KEY_FIRST_TIME, false)
            .apply()
    }
}