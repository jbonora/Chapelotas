package com.chapelotas.app.domain.models

data class AppSettings(
    // Valores por defecto que se usarán la primera vez que se instale la app.

    val userName: String = "Juancito",
    val workStartTime: String = "09:00",
    val workEndTime: String = "18:00",
    val workHours24h: Boolean = true,
    val firstReminder: Int = 60,
    val secondReminder: Int = 30,
    val thirdReminder: Int = 10,

    // --- LÓGICA DE INTERVALOS REFINADA SEGÚN TU PROPUESTA ---

    /**
     * Intervalo de "no molestar tanto".
     * Se usa para tareas EN CURSO que ya han sido ACEPTADAS.
     */
    val lowUrgencyInterval: Int = 20,

    /**
     * Tu "Intervalo de Alta Insistencia".
     * Se usa para:
     * 1. Tareas EN CURSO que NO han sido aceptadas.
     * 2. Tareas PASADAS que NO han sido terminadas (demoradas).
     */
    val highUrgencyInterval: Int = 1,

    // `missedInterval` y `ongoingInterval` han sido reemplazados por los dos de arriba.
    // ----------------------------------------------------

    val sarcasticMode: Boolean = true,
    val aiMode: Boolean = false
)