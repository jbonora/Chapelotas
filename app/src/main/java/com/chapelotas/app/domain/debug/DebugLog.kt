package com.chapelotas.app.domain.debug

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.LinkedList

/**
 * Un logger simple, en memoria y como Singleton para debuggear la app desde la UI.
 * Actúa como la "caja negra" de la aplicación.
 */
object DebugLog {
    private const val MAX_LOGS = 100
    private val formatter = DateTimeFormatter.ofPattern("HH:mm:ss.SSS")

    // Usamos una LinkedList para poder quitar elementos viejos de forma eficiente.
    private val _logs = MutableStateFlow<List<String>>(emptyList())
    val logs: StateFlow<List<String>> = _logs.asStateFlow()

    private val logQueue = LinkedList<String>()

    /**
     * Añade una nueva entrada al log.
     * @param message El mensaje a registrar.
     */
    fun add(message: String) {
        val logEntry = "[${LocalTime.now().format(formatter)}] $message"
        println("DEBUG_LOG: $logEntry") // También lo imprimimos a la consola normal

        synchronized(this) {
            // Si la cola está llena, removemos el más antiguo.
            if (logQueue.size >= MAX_LOGS) {
                logQueue.removeFirst()
            }
            // Añadimos el nuevo log al final.
            logQueue.add(logEntry)

            // Actualizamos el StateFlow para que la UI reaccione.
            _logs.update { logQueue.reversed() } // Invertimos para mostrar el más nuevo arriba.
        }
    }
}