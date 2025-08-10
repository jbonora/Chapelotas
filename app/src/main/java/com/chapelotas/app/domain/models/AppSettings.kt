package com.chapelotas.app.domain.models

// --- NOMBRES DE PERFILES SIMPLIFICADOS ---
object InsistenceProfile {
    const val NORMAL = "Normal" // Usa el sonido de alerta principal
    const val MEDIUM = "Medio"
    const val LOW = "Bajo"
}

data class AppSettings(
    val userName: String = "Juancito",
    val workStartTime: String = "08:00",
    val workEndTime: String = "23:00",
    val workHours24h: Boolean = true,
    val lunchStartTime: String = "13:00",
    val lunchEndTime: String = "14:00",
    val dinnerStartTime: String = "20:00",
    val dinnerEndTime: String = "21:00",

    val firstReminder: Int = 60,
    val secondReminder: Int = 30,
    val thirdReminder: Int = 10,

    val lowUrgencyInterval: Int = 20,
    val highUrgencyInterval: Int = 1,

    val insistenceSoundProfile: String = InsistenceProfile.LOW,

    val sarcasticMode: Boolean = true,
    // --- LÍNEA AÑADIDA ---
    val personalityProfile: String = "sarcastic", // Define el perfil de personalidad a usar

    val autoCreateAlarm: Boolean = true,
    val alarmOffsetMinutes: Int = 90, // 1:30 hs por defecto

    val snoozeMinutes: Int = 10,

    val travelTimeNearbyMinutes: Int = 15, // Tiempo de viaje para "Cerca"
    // --- VALOR ACTUALIZADO ---
    val travelTimeFarMinutes: Int = 40      // Tiempo de viaje para "Lejos"
)