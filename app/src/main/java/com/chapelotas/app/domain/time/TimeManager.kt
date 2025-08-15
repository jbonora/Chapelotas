package com.chapelotas.app.domain.time

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manager que emite el tiempo actual cada segundo para actualización del UI
 */
@Singleton
class TimeManager @Inject constructor() {

    /**
     * Flow que emite el tiempo actual cada segundo
     */
    val currentTimeFlow: Flow<LocalDateTime> = flow {
        while (true) {
            emit(LocalDateTime.now())
            delay(1000) // Actualizar cada segundo
        }
    }

    /**
     * Flow que emite el tiempo actual cada minuto (para actualizaciones menos frecuentes)
     */
    val currentTimeMinuteFlow: Flow<LocalDateTime> = flow {
        while (true) {
            emit(LocalDateTime.now().withSecond(0).withNano(0))
            // Esperar hasta el próximo minuto
            val now = LocalDateTime.now()
            val nextMinute = now.plusMinutes(1).withSecond(0).withNano(0)
            val delayMillis = ChronoUnit.MILLIS.between(now, nextMinute)
            delay(delayMillis)
        }
    }

    /**
     * Calcula el tiempo restante desde ahora hasta el momento objetivo
     */
    fun getTimeUntil(targetTime: LocalDateTime): TimeRemaining {
        val now = LocalDateTime.now()

        if (targetTime.isBefore(now)) {
            val minutesAgo = ChronoUnit.MINUTES.between(targetTime, now)
            val hoursAgo = minutesAgo / 60
            val remainingMinutes = minutesAgo % 60

            return TimeRemaining(
                isInPast = true,
                hours = hoursAgo.toInt(),
                minutes = remainingMinutes.toInt(),
                totalMinutes = minutesAgo
            )
        }

        val minutesUntil = ChronoUnit.MINUTES.between(now, targetTime)
        val hoursUntil = minutesUntil / 60
        val remainingMinutes = minutesUntil % 60

        return TimeRemaining(
            isInPast = false,
            hours = hoursUntil.toInt(),
            minutes = remainingMinutes.toInt(),
            totalMinutes = minutesUntil
        )
    }

    /**
     * Formatea el tiempo restante para mostrar en UI
     */
    fun formatTimeRemaining(timeRemaining: TimeRemaining): String {
        return when {
            timeRemaining.isInPast -> {
                when {
                    timeRemaining.totalMinutes < 60 -> "hace ${timeRemaining.totalMinutes} min"
                    timeRemaining.hours < 24 -> "hace ${timeRemaining.hours}h ${timeRemaining.minutes}min"
                    else -> "hace ${timeRemaining.hours / 24} días"
                }
            }
            timeRemaining.totalMinutes == 0L -> "ahora"
            timeRemaining.totalMinutes < 60 -> "en ${timeRemaining.totalMinutes} min"
            timeRemaining.hours < 24 -> "en ${timeRemaining.hours}h ${timeRemaining.minutes}min"
            else -> "en ${timeRemaining.hours / 24} días"
        }
    }
}

data class TimeRemaining(
    val isInPast: Boolean,
    val hours: Int,
    val minutes: Int,
    val totalMinutes: Long
)