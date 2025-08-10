package com.chapelotas.app.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.chapelotas.app.domain.models.AppSettings
import com.chapelotas.app.domain.repositories.PreferencesRepository
import com.google.gson.Gson
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

// Se utiliza DataStore en lugar de SharedPreferences para un manejo de datos asíncrono y más seguro.
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "chapelotas_settings")

@Singleton
class PreferencesRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : PreferencesRepository {

    private val gson = Gson()

    private object PreferenceKeys {
        val APP_SETTINGS = stringPreferencesKey("app_settings")
        // --- CLAVE AÑADIDA ---
        val FINAL_HUAWEI_ALARM_TIME = stringPreferencesKey("final_huawei_alarm_time")
    }

    // El flujo de datos se lee directamente desde DataStore, que maneja la carga y observación automáticamente.
    override fun observeAppSettings(): Flow<AppSettings> {
        return context.dataStore.data.map { preferences ->
            val json = preferences[PreferenceKeys.APP_SETTINGS]
            if (json != null) {
                try {
                    gson.fromJson(json, AppSettings::class.java)
                } catch (e: Exception) {
                    AppSettings() // Devuelve valores por defecto si hay un error de deserialización.
                }
            } else {
                AppSettings() // Devuelve valores por defecto si no hay nada guardado.
            }
        }
    }

    // Guardar los ajustes ahora es una operación de suspensión segura.
    override suspend fun saveAppSettings(settings: AppSettings) {
        context.dataStore.edit { preferences ->
            val json = gson.toJson(settings)
            preferences[PreferenceKeys.APP_SETTINGS] = json
        }
    }

    // --- IMPLEMENTACIÓN DE FUNCIONES AÑADIDAS ---
    override suspend fun setFinalHuaweiAlarmTime(dateTime: LocalDateTime) {
        context.dataStore.edit { preferences ->
            preferences[PreferenceKeys.FINAL_HUAWEI_ALARM_TIME] = dateTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        }
    }

    override fun getFinalHuaweiAlarmTime(): Flow<LocalDateTime?> {
        return context.dataStore.data.map { preferences ->
            preferences[PreferenceKeys.FINAL_HUAWEI_ALARM_TIME]?.let {
                try {
                    LocalDateTime.parse(it, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                } catch (e: Exception) {
                    null
                }
            }
        }
    }

    override suspend fun clearFinalHuaweiAlarmTime() {
        context.dataStore.edit { preferences ->
            preferences.remove(PreferenceKeys.FINAL_HUAWEI_ALARM_TIME)
        }
    }
    // --- FIN DE LA IMPLEMENTACIÓN ---

    // Las funciones antiguas se mantienen con SharedPreferences por simplicidad y compatibilidad,
    // pero idealmente también se migrarían a DataStore en el futuro.
    private val legacyPrefs = context.getSharedPreferences("chapelotas_prefs", Context.MODE_PRIVATE)

    override suspend fun isFirstTimeUser(): Boolean {
        return legacyPrefs.getBoolean("is_first_time_user", true)
    }

    override suspend fun isTodayInitialized(date: String): Boolean {
        return legacyPrefs.getBoolean("today_initialized_$date", false)
    }

    override suspend fun setTodayInitialized(date: String) {
        legacyPrefs.edit().putBoolean("today_initialized_$date", true).apply()
    }

    override suspend fun areAlarmsConfigured(): Boolean {
        return legacyPrefs.getBoolean("alarms_configured", false)
    }

    override suspend fun setAlarmsConfigured(configured: Boolean) {
        legacyPrefs.edit().putBoolean("alarms_configured", configured).apply()
    }

    override suspend fun getLastSuccessfulRun(): Long? {
        val timestamp = legacyPrefs.getLong("last_successful_run", -1)
        return if (timestamp == -1L) null else timestamp
    }

    override suspend fun setLastSuccessfulRun(timestamp: Long) {
        legacyPrefs.edit()
            .putLong("last_successful_run", timestamp)
            .putBoolean("is_first_time_user", false)
            .apply()
    }
}