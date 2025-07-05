// =====================================
// PROYECTO CHAPELOTAS - CÓDIGO COMPLETO
// =====================================

// =====================================
// DOMAIN LAYER - ENTITIES
// =====================================

// --- CalendarEvent.kt ---
// Path: domain/entities/CalendarEvent.kt
package com.chapelotas.app.domain.entities

import java.time.LocalDateTime

data class CalendarEvent(
    val id: Long,
    val title: String,
    val description: String?,
    val startTime: LocalDateTime,
    val endTime: LocalDateTime,
    val location: String?,
    val isAllDay: Boolean = false,
    val calendarId: Long,
    val calendarName: String,
    val isCritical: Boolean = false,
    val hasBeenSummarized: Boolean = false
) {
    val durationInMinutes: Long
        get() = java.time.Duration.between(startTime, endTime).toMinutes()
    
    fun isToday(): Boolean {
        val today = LocalDateTime.now().toLocalDate()
        return startTime.toLocalDate() == today
    }
    
    fun isTomorrow(): Boolean {
        val tomorrow = LocalDateTime.now().toLocalDate().plusDays(1)
        return startTime.toLocalDate() == tomorrow
    }
    
    fun minutesUntilStart(): Long {
        return java.time.Duration.between(LocalDateTime.now(), startTime).toMinutes()
    }
    
    fun timeUntilStartDescription(): String {
        val minutes = minutesUntilStart()
        return when {
            minutes < -60 -> "Empezó hace ${-minutes / 60} horas"
            minutes < 0 -> "Empezó hace ${-minutes} minutos"
            minutes == 0L -> "Empieza AHORA"
            minutes < 60 -> "En $minutes minutos"
            minutes < 1440 -> "En ${minutes / 60} horas"
            else -> "En ${minutes / 1440} días"
        }
    }
}

// --- ChapelotasNotification.kt ---
// Path: domain/entities/ChapelotasNotification.kt
package com.chapelotas.app.domain.entities

import java.time.LocalDateTime

data class ChapelotasNotification(
    val id: String,
    val eventId: Long,
    val scheduledTime: LocalDateTime,
    val message: String,
    val priority: NotificationPriority,
    val type: NotificationType,
    val hasBeenShown: Boolean = false,
    val createdAt: LocalDateTime = LocalDateTime.now()
) {
    fun shouldShowNow(): Boolean {
        return !hasBeenShown && LocalDateTime.now().isAfter(scheduledTime)
    }
    
    fun minutesUntilShow(): Long {
        return java.time.Duration.between(LocalDateTime.now(), scheduledTime).toMinutes()
    }
}

enum class NotificationPriority {
    LOW, NORMAL, HIGH, CRITICAL
}

enum class NotificationType {
    DAILY_SUMMARY, TOMORROW_SUMMARY, EVENT_REMINDER,
    CRITICAL_ALERT, PREPARATION_TIP, SARCASTIC_NUDGE
}

// --- AIPlan.kt ---
// Path: domain/entities/AIPlan.kt
package com.chapelotas.app.domain.entities

import java.time.LocalDateTime

data class AIPlan(
    val id: String,
    val generatedAt: LocalDateTime,
    val eventsAnalyzed: List<Long>,
    val notifications: List<PlannedNotification>,
    val aiInsights: String?,
    val suggestedFocus: String?
) {
    fun getPendingNotifications(): List<PlannedNotification> {
        return notifications.filter { !it.hasBeenScheduled }
    }
    
    fun criticalAlertsCount(): Int {
        return notifications.count { it.priority == NotificationPriority.CRITICAL }
    }
}

data class PlannedNotification(
    val eventId: Long,
    val suggestedTime: LocalDateTime,
    val suggestedMessage: String,
    val priority: NotificationPriority,
    val type: NotificationType,
    val rationale: String?,
    var hasBeenScheduled: Boolean = false
) {
    fun toScheduledNotification(): ChapelotasNotification {
        return ChapelotasNotification(
            id = java.util.UUID.randomUUID().toString(),
            eventId = eventId,
            scheduledTime = suggestedTime,
            message = suggestedMessage,
            priority = priority,
            type = type
        )
    }
}

// --- UserPreferences.kt ---
// Path: domain/entities/UserPreferences.kt
package com.chapelotas.app.domain.entities

import java.time.LocalTime

data class UserPreferences(
    val dailySummaryTime: LocalTime = LocalTime.of(7, 0),
    val tomorrowSummaryTime: LocalTime = LocalTime.of(20, 0),
    val isSarcasticModeEnabled: Boolean = false,
    val criticalAlertSound: String = "default_ringtone",
    val notificationSound: String = "default_notification",
    val minutesBeforeEventForReminder: Int = 15,
    val hasAcceptedPrivacyPolicy: Boolean = false,
    val isFirstTimeUser: Boolean = true,
    val preferredCalendars: Set<Long> = emptySet()
) {
    fun isTimeForDailySummary(): Boolean {
        val now = LocalTime.now()
        return now.hour == dailySummaryTime.hour && 
               now.minute >= dailySummaryTime.minute &&
               now.minute < dailySummaryTime.minute + 5
    }
    
    fun isTimeForTomorrowSummary(): Boolean {
        val now = LocalTime.now()
        return now.hour == tomorrowSummaryTime.hour && 
               now.minute >= tomorrowSummaryTime.minute &&
               now.minute < tomorrowSummaryTime.minute + 5
    }
}

// =====================================
// DOMAIN LAYER - REPOSITORIES
// =====================================

// --- CalendarRepository.kt ---
// Path: domain/repositories/CalendarRepository.kt
package com.chapelotas.app.domain.repositories

import com.chapelotas.app.domain.entities.CalendarEvent
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

interface CalendarRepository {
    suspend fun getAvailableCalendars(): Map<Long, String>
    suspend fun getEventsForDate(date: LocalDate, calendarIds: Set<Long>? = null): List<CalendarEvent>
    suspend fun getEventsInRange(startDate: LocalDate, endDate: LocalDate, calendarIds: Set<Long>? = null): List<CalendarEvent>
    suspend fun getTodayEvents(calendarIds: Set<Long>? = null): List<CalendarEvent>
    suspend fun getTomorrowEvents(calendarIds: Set<Long>? = null): List<CalendarEvent>
    fun observeCalendarChanges(): Flow<Unit>
    suspend fun markEventAsCritical(eventId: Long, isCritical: Boolean)
    suspend fun getCriticalEventIds(): Set<Long>
}

// --- NotificationRepository.kt ---
// Path: domain/repositories/NotificationRepository.kt
package com.chapelotas.app.domain.repositories

import com.chapelotas.app.domain.entities.ChapelotasNotification
import kotlinx.coroutines.flow.Flow

interface NotificationRepository {
    suspend fun scheduleNotification(notification: ChapelotasNotification)
    suspend fun scheduleMultipleNotifications(notifications: List<ChapelotasNotification>)
    suspend fun cancelNotification(notificationId: String)
    suspend fun cancelNotificationsForEvent(eventId: Long)
    suspend fun getPendingNotifications(): List<ChapelotasNotification>
    suspend fun getNotificationsForEvent(eventId: Long): List<ChapelotasNotification>
    suspend fun markAsShown(notificationId: String)
    suspend fun cleanOldNotifications(daysToKeep: Int = 7)
    fun observeNotifications(): Flow<List<ChapelotasNotification>>
    suspend fun showImmediateNotification(notification: ChapelotasNotification)
    suspend fun isNotificationServiceRunning(): Boolean
    suspend fun startNotificationService()
    suspend fun stopNotificationService()
}

// --- AIRepository.kt ---
// Path: domain/repositories/AIRepository.kt
package com.chapelotas.app.domain.repositories

import com.chapelotas.app.domain.entities.AIPlan
import com.chapelotas.app.domain.entities.CalendarEvent

interface AIRepository {
    suspend fun generateCommunicationPlan(events: List<CalendarEvent>, userContext: String? = null): AIPlan
    suspend fun generateNotificationMessage(event: CalendarEvent, messageType: String, isSarcastic: Boolean = false, additionalContext: String? = null): String
    suspend fun generateDailySummary(todayEvents: List<CalendarEvent>, isSarcastic: Boolean = false): String
    suspend fun generateTomorrowSummary(tomorrowEvents: List<CalendarEvent>, todayContext: String? = null, isSarcastic: Boolean = false): String
    suspend fun suggestCriticalEvents(event: CalendarEvent, userHistory: List<CalendarEvent>? = null): Boolean
    suspend fun generatePreparationTips(event: CalendarEvent, weatherInfo: String? = null, trafficInfo: String? = null): String?
    suspend fun testConnection(): Boolean
    suspend fun getCurrentModel(): String
}

// --- PreferencesRepository.kt ---
// Path: domain/repositories/PreferencesRepository.kt
package com.chapelotas.app.domain.repositories

import com.chapelotas.app.domain.entities.UserPreferences
import kotlinx.coroutines.flow.Flow

interface PreferencesRepository {
    suspend fun getUserPreferences(): UserPreferences
    fun observeUserPreferences(): Flow<UserPreferences>
    suspend fun updateUserPreferences(preferences: UserPreferences)
    suspend fun updateDailySummaryTime(hour: Int, minute: Int)
    suspend fun updateTomorrowSummaryTime(hour: Int, minute: Int)
    suspend fun setSarcasticMode(enabled: Boolean)
    suspend fun updatePreferredCalendars(calendarIds: Set<Long>)
    suspend fun acceptPrivacyPolicy()
    suspend fun markAsExperiencedUser()
    suspend fun updateCriticalAlertSound(soundUri: String)
    suspend fun updateNotificationSound(soundUri: String)
    suspend fun updateReminderMinutesBefore(minutes: Int)
    suspend fun resetToDefaults()
    suspend fun isFirstTimeUser(): Boolean
}

// =====================================
// DOMAIN LAYER - USE CASES
// =====================================

// --- GetDailySummaryUseCase.kt ---
// Path: domain/usecases/GetDailySummaryUseCase.kt
package com.chapelotas.app.domain.usecases

import com.chapelotas.app.domain.entities.CalendarEvent
import com.chapelotas.app.domain.repositories.AIRepository
import com.chapelotas.app.domain.repositories.CalendarRepository
import com.chapelotas.app.domain.repositories.PreferencesRepository
import javax.inject.Inject

class GetDailySummaryUseCase @Inject constructor(
    private val calendarRepository: CalendarRepository,
    private val aiRepository: AIRepository,
    private val preferencesRepository: PreferencesRepository
) {
    suspend operator fun invoke(): Result<DailySummaryResult> {
        return try {
            val preferences = preferencesRepository.getUserPreferences()
            val todayEvents = calendarRepository.getTodayEvents(
                calendarIds = preferences.preferredCalendars.takeIf { it.isNotEmpty() }
            )
            val criticalEventIds = calendarRepository.getCriticalEventIds()
            val eventsWithCriticality = todayEvents.map { event ->
                event.copy(isCritical = event.id in criticalEventIds)
            }
            val summary = aiRepository.generateDailySummary(
                todayEvents = eventsWithCriticality,
                isSarcastic = preferences.isSarcasticModeEnabled
            )
            val nextEvent = findNextEvent(todayEvents)
            
            Result.success(
                DailySummaryResult(
                    summary = summary,
                    events = eventsWithCriticality,
                    hasEvents = todayEvents.isNotEmpty(),
                    nextEvent = nextEvent,
                    criticalEventsCount = eventsWithCriticality.count { it.isCritical }
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    private fun findNextEvent(events: List<CalendarEvent>): CalendarEvent? {
        val now = java.time.LocalDateTime.now()
        return events.filter { it.startTime.isAfter(now) }.minByOrNull { it.startTime }
    }
}

data class DailySummaryResult(
    val summary: String,
    val events: List<CalendarEvent>,
    val hasEvents: Boolean,
    val nextEvent: CalendarEvent?,
    val criticalEventsCount: Int
)

// =====================================
// PRESENTATION LAYER
// =====================================

// --- MainViewModel.kt ---
// Path: presentation/viewmodels/MainViewModel.kt
package com.chapelotas.app.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chapelotas.app.domain.entities.CalendarEvent
import com.chapelotas.app.domain.usecases.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val getDailySummaryUseCase: GetDailySummaryUseCase,
    private val getTomorrowSummaryUseCase: GetTomorrowSummaryUseCase,
    private val scheduleNotificationsUseCase: ScheduleNotificationsUseCase,
    private val markEventAsCriticalUseCase: MarkEventAsCriticalUseCase,
    private val setupInitialConfigurationUseCase: SetupInitialConfigurationUseCase
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()
    
    private val _todayEvents = MutableStateFlow<List<CalendarEvent>>(emptyList())
    val todayEvents: StateFlow<List<CalendarEvent>> = _todayEvents.asStateFlow()
    
    private val _dailySummary = MutableStateFlow("")
    val dailySummary: StateFlow<String> = _dailySummary.asStateFlow()
    
    private val _tomorrowSummary = MutableStateFlow("")
    val tomorrowSummary: StateFlow<String> = _tomorrowSummary.asStateFlow()
    
    init {
        checkInitialSetup()
    }
    
    private fun checkInitialSetup() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            setupInitialConfigurationUseCase().fold(
                onSuccess = { result ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isFirstTimeUser = result.isFirstTimeSetup,
                        requiresPermissions = result.requiresPermissions,
                        availableCalendars = result.availableCalendars
                    )
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = error.message
                    )
                }
            )
        }
    }
    
    fun loadDailySummary() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingSummary = true)
            
            getDailySummaryUseCase().fold(
                onSuccess = { result ->
                    _dailySummary.value = result.summary
                    _todayEvents.value = result.events
                    _uiState.value = _uiState.value.copy(
                        isLoadingSummary = false,
                        hasEventsToday = result.hasEvents,
                        criticalEventsCount = result.criticalEventsCount
                    )
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoadingSummary = false,
                        error = error.message
                    )
                }
            )
        }
    }
    
    fun loadTomorrowSummary() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingSummary = true)
            
            getTomorrowSummaryUseCase().fold(
                onSuccess = { result ->
                    _tomorrowSummary.value = result.summary
                    _uiState.value = _uiState.value.copy(
                        isLoadingSummary = false,
                        hasEventsTomorrow = result.hasEvents
                    )
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoadingSummary = false,
                        error = error.message
                    )
                }
            )
        }
    }
    
    fun scheduleNotifications(forTomorrow: Boolean = false) {
        viewModelScope.launch {
            scheduleNotificationsUseCase(forTomorrow).fold(
                onSuccess = { result ->
                    _uiState.value = _uiState.value.copy(
                        lastScheduledCount = result.notificationsScheduled
                    )
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        error = error.message
                    )
                }
            )
        }
    }
    
    fun toggleEventCritical(eventId: Long, isCritical: Boolean) {
        viewModelScope.launch {
            markEventAsCriticalUseCase(eventId, isCritical).fold(
                onSuccess = {
                    loadDailySummary()
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        error = error.message
                    )
                }
            )
        }
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
    
    fun onPermissionsGranted() {
        _uiState.value = _uiState.value.copy(requiresPermissions = false)
        checkInitialSetup()
    }
}

data class MainUiState(
    val isLoading: Boolean = false,
    val isLoadingSummary: Boolean = false,
    val isFirstTimeUser: Boolean = false,
    val requiresPermissions: Boolean = false,
    val hasEventsToday: Boolean = false,
    val hasEventsTomorrow: Boolean = false,
    val criticalEventsCount: Int = 0,
    val lastScheduledCount: Int = 0,
    val availableCalendars: Map<Long, String> = emptyMap(),
    val error: String? = null
)

// =====================================
// APPLICATION CLASS
// =====================================

// --- ChapelotasApplication.kt ---
// Path: com/chapelotas/app/ChapelotasApplication.kt
package com.chapelotas.app

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class ChapelotasApplication : Application() {
    override fun onCreate() {
        super.onCreate()
    }
}

// =====================================
// DATA MODULE (HILT)
// =====================================

// --- DataModule.kt ---
// Path: data/DataModule.kt
package com.chapelotas.app.data

import com.chapelotas.app.data.ai.AIRepositoryImpl
import com.chapelotas.app.data.calendar.CalendarRepositoryImpl
import com.chapelotas.app.data.notifications.NotificationRepositoryImpl
import com.chapelotas.app.domain.repositories.AIRepository
import com.chapelotas.app.domain.repositories.CalendarRepository
import com.chapelotas.app.domain.repositories.NotificationRepository
import com.chapelotas.app.domain.repositories.PreferencesRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {
    
    @Binds
    @Singleton
    abstract fun bindCalendarRepository(
        calendarRepositoryImpl: CalendarRepositoryImpl
    ): CalendarRepository
    
    @Binds
    @Singleton
    abstract fun bindNotificationRepository(
        notificationRepositoryImpl: NotificationRepositoryImpl
    ): NotificationRepository
    
    @Binds
    @Singleton
    abstract fun bindAIRepository(
        aiRepositoryImpl: AIRepositoryImpl
    ): AIRepository
    
    @Binds
    @Singleton
    abstract fun bindPreferencesRepository(
        preferencesRepositoryImpl: PreferencesRepositoryImpl
    ): PreferencesRepository
}