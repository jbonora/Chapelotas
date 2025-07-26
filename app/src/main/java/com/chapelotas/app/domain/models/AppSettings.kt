package com.chapelotas.app.domain.models

data class AppSettings(
    // Valores por defecto que se usarán la primera vez que se instale la app.
    // Luego, se leerán los valores que el usuario modifique desde la pantalla de Ajustes.

    val userName: String = "Juancito",
    val workStartTime: String = "09:00",
    val workEndTime: String = "18:00",
    val workHours24h: Boolean = true,
    val firstReminder: Int = 60,
    val secondReminder: Int = 30,
    val thirdReminder: Int = 10,
    val ongoingInterval: Int = 20,
    val missedInterval: Int = 1,
    val sarcasticMode: Boolean = false,
    val aiMode: Boolean = false
)