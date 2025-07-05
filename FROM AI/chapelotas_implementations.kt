// =====================================
// DATA LAYER - IMPLEMENTATIONS (Parte 2)
// =====================================

// --- PreferencesRepositoryImpl.kt ---
// Path: data/PreferencesRepositoryImpl.kt
package com.chapelotas.app.data

import android.content.Context
import android.content.SharedPreferences
import com.chapelotas.app.domain.entities.UserPreferences
import com.chapelotas.app.domain.repositories.PreferencesRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PreferencesRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : PreferencesRepository {
    
    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME, 
        Context.MODE_PRIVATE
    )
    
    private val _preferencesFlow = MutableStateFlow(loadPreferences())
    
    companion object {
        private const val PREFS_NAME = "chapelotas_preferences"
        private const val KEY_DAILY_SUMMARY_HOUR = "daily_summary_hour"
        private const val KEY_DAILY_SUMMARY_MINUTE = "daily_summary_minute"
        private const val KEY_TOMORROW_SUMMARY_HOUR = "tomorrow_summary_hour"
        private const val KEY_TOMORROW_SUMMARY_MINUTE = "tomorrow_summary_minute"
        private const val KEY_SARCASTIC_MODE = "sarcastic_mode"
        private const val KEY_CRITICAL_ALERT_SOUND = "critical_alert_sound"
        private const val KEY_NOTIFICATION_SOUND = "notification_sound"
        private const val KEY_REMINDER_MINUTES = "reminder_minutes_before"
        private const val KEY_PRIVACY_ACCEPTED = "privacy_policy_accepted"
        private const val KEY_FIRST_TIME_USER = "is_first_time_user"
        private const val KEY_PREFERRED_CALENDARS = "preferred_calendars"
    }
    
    override suspend fun getUserPreferences(): UserPreferences {
        return loadPreferences()
    }
    
    override fun observeUserPreferences(): Flow<UserPreferences> {
        return _preferencesFlow.asStateFlow()
    }
    
    override suspend fun updateUserPreferences(preferences: UserPreferences) {
        savePreferences(preferences)
        _preferencesFlow.value = preferences
    }
    
    override suspend fun updateDailySummaryTime(hour: Int, minute: Int) {
        val current = loadPreferences()
        val updated = current.copy(
            dailySummaryTime = LocalTime.of(hour, minute)
        )
        savePreferences(updated)
        _preferencesFlow.value = updated
    }
    
    override suspend fun updateTomorrowSummaryTime(hour: Int, minute: Int) {
        val current = loadPreferences()
        val updated = current.copy(
            tomorrowSummaryTime = LocalTime.of(hour, minute)
        )
        savePreferences(updated)
        _preferencesFlow.value = updated
    }
    
    override suspend fun setSarcasticMode(enabled: Boolean) {
        val current = loadPreferences()
        val updated = current.copy(isSarcasticModeEnabled = enabled)
        savePreferences(updated)
        _preferencesFlow.value = updated
    }
    
    override suspend fun updatePreferredCalendars(calendarIds: Set<Long>) {
        val current = loadPreferences()
        val updated = current.copy(preferredCalendars = calendarIds)
        savePreferences(updated)
        _preferencesFlow.value = updated
    }
    
    override suspend fun acceptPrivacyPolicy() {
        val current = loadPreferences()
        val updated = current.copy(hasAcceptedPrivacyPolicy = true)
        savePreferences(updated)
        _preferencesFlow.value = updated
    }
    
    override suspend fun markAsExperiencedUser() {
        val current = loadPreferences()
        val updated = current.copy(isFirstTimeUser = false)
        savePreferences(updated)
        _preferencesFlow.value = updated
    }
    
    override suspend fun updateCriticalAlertSound(soundUri: String) {
        val current = loadPreferences()
        val updated = current.copy(criticalAlertSound = soundUri)
        savePreferences(updated)
        _preferencesFlow.value = updated
    }
    
    override suspend fun updateNotificationSound(soundUri: String) {
        val current = loadPreferences()
        val updated = current.copy(notificationSound = soundUri)
        savePreferences(updated)
        _preferencesFlow.value = updated
    }
    
    override suspend fun updateReminderMinutesBefore(minutes: Int) {
        val current = loadPreferences()
        val updated = current.copy(minutesBeforeEventForReminder = minutes)
        savePreferences(updated)
        _preferencesFlow.value = updated
    }
    
    override suspend fun resetToDefaults() {
        val defaults = UserPreferences()
        savePreferences(defaults)
        _preferencesFlow.value = defaults
    }
    
    override suspend fun isFirstTimeUser(): Boolean {
        return prefs.getBoolean(KEY_FIRST_TIME_USER, true)
    }
    
    private fun loadPreferences(): UserPreferences {
        return UserPreferences(
            dailySummaryTime = LocalTime.of(
                prefs.getInt(KEY_DAILY_SUMMARY_HOUR, 7),
                prefs.getInt(KEY_DAILY_SUMMARY_MINUTE, 0)
            ),
            tomorrowSummaryTime = LocalTime.of(
                prefs.getInt(KEY_TOMORROW_SUMMARY_HOUR, 20),
                prefs.getInt(KEY_TOMORROW_SUMMARY_MINUTE, 0)
            ),
            isSarcasticModeEnabled = prefs.getBoolean(KEY_SARCASTIC_MODE, false),
            criticalAlertSound = prefs.getString(KEY_CRITICAL_ALERT_SOUND, "default_ringtone") ?: "default_ringtone",
            notificationSound = prefs.getString(KEY_NOTIFICATION_SOUND, "default_notification") ?: "default_notification",
            minutesBeforeEventForReminder = prefs.getInt(KEY_REMINDER_MINUTES, 15),
            hasAcceptedPrivacyPolicy = prefs.getBoolean(KEY_PRIVACY_ACCEPTED, false),
            isFirstTimeUser = prefs.getBoolean(KEY_FIRST_TIME_USER, true),
            preferredCalendars = prefs.getStringSet(KEY_PREFERRED_CALENDARS, emptySet())
                ?.mapNotNull { it.toLongOrNull() }
                ?.toSet() ?: emptySet()
        )
    }
    
    private fun savePreferences(preferences: UserPreferences) {
        prefs.edit().apply {
            putInt(KEY_DAILY_SUMMARY_HOUR, preferences.dailySummaryTime.hour)
            putInt(KEY_DAILY_SUMMARY_MINUTE, preferences.dailySummaryTime.minute)
            putInt(KEY_TOMORROW_SUMMARY_HOUR, preferences.tomorrowSummaryTime.hour)
            putInt(KEY_TOMORROW_SUMMARY_MINUTE, preferences.tomorrowSummaryTime.minute)
            putBoolean(KEY_SARCASTIC_MODE, preferences.isSarcasticModeEnabled)
            putString(KEY_CRITICAL_ALERT_SOUND, preferences.criticalAlertSound)
            putString(KEY_NOTIFICATION_SOUND, preferences.notificationSound)
            putInt(KEY_REMINDER_MINUTES, preferences.minutesBeforeEventForReminder)
            putBoolean(KEY_PRIVACY_ACCEPTED, preferences.hasAcceptedPrivacyPolicy)
            putBoolean(KEY_FIRST_TIME_USER, preferences.isFirstTimeUser)
            putStringSet(KEY_PREFERRED_CALENDARS, preferences.preferredCalendars.map { it.toString() }.toSet())
            apply()
        }
    }
}

// --- CalendarRepositoryImpl.kt (EXTRACTO CLAVE) ---
// Path: data/calendar/CalendarRepositoryImpl.kt
package com.chapelotas.app.data.calendar

import android.Manifest
import android.content.ContentResolver
import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.CalendarContract
import androidx.core.content.ContextCompat
import com.chapelotas.app.domain.entities.CalendarEvent
import com.chapelotas.app.domain.repositories.CalendarRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CalendarRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferencesRepository: com.chapelotas.app.domain.repositories.PreferencesRepository
) : CalendarRepository {
    
    private val contentResolver: ContentResolver = context.contentResolver
    private val criticalEventIds = mutableSetOf<Long>()
    
    // Implementación completa disponible en el proyecto
    // Incluye lectura de calendarios, eventos, observadores, etc.
}

// --- NotificationRepositoryImpl.kt (EXTRACTO CLAVE) ---
// Path: data/notifications/NotificationRepositoryImpl.kt
package com.chapelotas.app.data.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.*
import com.chapelotas.app.R
import com.chapelotas.app.domain.entities.ChapelotasNotification
import com.chapelotas.app.domain.entities.NotificationPriority
import com.chapelotas.app.domain.entities.NotificationType
import com.chapelotas.app.domain.repositories.NotificationRepository
import com.chapelotas.app.MainActivity
import com.google.gson.Gson
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.Duration
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : NotificationRepository {
    
    private val workManager = WorkManager.getInstance(context)
    private val notificationManager = NotificationManagerCompat.from(context)
    private val gson = Gson()
    private val _notificationsFlow = MutableStateFlow<List<ChapelotasNotification>>(emptyList())
    
    companion object {
        const val CHANNEL_GENERAL = "chapelotas_general"
        const val CHANNEL_CRITICAL = "chapelotas_critical"
        const val CHANNEL_SERVICE = "chapelotas_service"
        const val WORK_TAG_PREFIX = "chapelotas_notification_"
        const val NOTIFICATION_DATA_KEY = "notification_data"
        const val FOREGROUND_SERVICE_ID = 1337
        private var notificationIdCounter = 2000
    }
    
    // Implementación completa disponible en el proyecto
}

// --- NotificationWorker.kt ---
// Path: data/notifications/NotificationWorker.kt
package com.chapelotas.app.data.notifications

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.chapelotas.app.domain.entities.ChapelotasNotification
import com.chapelotas.app.domain.repositories.NotificationRepository
import com.google.gson.Gson
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class NotificationWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val notificationRepository: NotificationRepository
) : CoroutineWorker(appContext, workerParams) {
    
    override suspend fun doWork(): Result {
        return try {
            val notificationJson = inputData.getString(NotificationRepositoryImpl.NOTIFICATION_DATA_KEY)
                ?: return Result.failure()
            
            val notification = Gson().fromJson(notificationJson, ChapelotasNotification::class.java)
            notificationRepository.showImmediateNotification(notification)
            
            Result.success()
        } catch (e: Exception) {
            Result.failure()
        }
    }
}

// --- ChapelotasNotificationService.kt ---
// Path: data/notifications/ChapelotasNotificationService.kt
package com.chapelotas.app.data.notifications

import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.chapelotas.app.R
import com.chapelotas.app.MainActivity
import android.app.PendingIntent
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ChapelotasNotificationService : Service() {
    
    override fun onCreate() {
        super.onCreate()
        startForegroundService()
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }
    
    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
    
    private fun startForegroundService() {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            notificationIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val notification = NotificationCompat.Builder(this, NotificationRepositoryImpl.CHANNEL_SERVICE)
            .setContentTitle("Chapelotas está activo")
            .setContentText("Vigilando tu calendario para que no te olvides de nada 👀")
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
        
        startForeground(NotificationRepositoryImpl.FOREGROUND_SERVICE_ID, notification)
    }
}

// --- Theme.kt ---
// Path: ui/theme/Theme.kt
package com.chapelotas.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.foundation.isSystemInDarkTheme

private val LightColorScheme = lightColorScheme(
    primary = androidx.compose.ui.graphics.Color(0xFF1976D2),
    secondary = androidx.compose.ui.graphics.Color(0xFF388E3C),
    tertiary = androidx.compose.ui.graphics.Color(0xFFF57C00)
)

private val DarkColorScheme = darkColorScheme(
    primary = androidx.compose.ui.graphics.Color(0xFF90CAF9),
    secondary = androidx.compose.ui.graphics.Color(0xFF81C784),
    tertiary = androidx.compose.ui.graphics.Color(0xFFFFB74D)
)

@Composable
fun ChapelotasTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}

// =====================================
// CONFIGURACIÓN IMPORTANTE
// =====================================

// --- AndroidManifest.xml ---
/*
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    
    <uses-permission android:name="android.permission.READ_CALENDAR" />
    <uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
    <uses-permission android:name="android.permission.VIBRATE" />
    <uses-permission android:name="android.permission.WAKE_LOCK" />
    <uses-permission android:name="android.permission.USE_FULL_SCREEN_INTENT" />
    
    <application
        android:name=".ChapelotasApplication"
        android:allowBackup="true"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:theme="@style/Theme.Chapelotas">
        
        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:theme="@style/Theme.Chapelotas">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
        
        <service
            android:name=".data.notifications.ChapelotasNotificationService"
            android:enabled="true"
            android:exported="false"
            android:foregroundServiceType="dataSync" />
            
        <activity
            android:name=".presentation.ui.CriticalAlertActivity"
            android:excludeFromRecents="true"
            android:launchMode="singleTask"
            android:showOnLockScreen="true"
            android:theme="@style/Theme.Chapelotas.CriticalAlert" />
            
    </application>
</manifest>
*/

// --- themes.xml ---
/*
<resources xmlns:tools="http://schemas.android.com/tools">
    <style name="Base.Theme.Chapelotas" parent="Theme.Material3.DayNight.NoActionBar">
    </style>

    <style name="Theme.Chapelotas" parent="Base.Theme.Chapelotas" />
    
    <style name="Theme.Chapelotas.CriticalAlert" parent="Theme.Material3.Dark.NoActionBar">
        <item name="android:windowBackground">@android:color/black</item>
        <item name="android:windowNoTitle">true</item>
        <item name="android:windowFullscreen">true</item>
        <item name="android:windowContentOverlay">@null</item>
    </style>
</resources>
*/

// --- ic_notification.xml (drawable) ---
/*
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp"
    android:height="24dp"
    android:viewportWidth="24"
    android:viewportHeight="24">
    <path
        android:fillColor="#FFFFFF"
        android:pathData="M12,22c1.1,0 2,-0.9 2,-2h-4c0,1.1 0.89,2 2,2zM18,16v-5c0,-3.07 -1.64,-5.64 -4.5,-6.32L13.5,4c0,-0.83 -0.67,-1.5 -1.5,-1.5s-1.5,0.67 -1.5,1.5v0.68C7.63,5.36 6,7.92 6,11v5l-2,2v1h16v-1l-2,-2z"/>
</vector>
*/

// =====================================
// NOTAS IMPORTANTES
// =====================================
/*
1. MainActivity.kt completa está en el proyecto
2. CriticalAlertActivity.kt completa está en el proyecto
3. AIRepositoryImpl.kt tiene implementación simulada lista para reemplazar con API real
4. Todos los Use Cases restantes están implementados
5. Las versiones finales en libs.versions.toml y build.gradle.kts están funcionando
*/