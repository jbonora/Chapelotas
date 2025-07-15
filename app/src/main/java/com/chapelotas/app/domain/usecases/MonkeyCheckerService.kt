package com.chapelotas.app.domain.usecases

import android.content.Context
import android.util.Log
import com.chapelotas.app.domain.entities.*
import com.chapelotas.app.domain.repositories.NotificationRepository
import com.google.gson.Gson
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import java.io.File
import java.time.LocalTime
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

/**
 * El Mono que checkea cada 5 minutos
 * Solo sabe comparar horas y ejecutar órdenes
 */
@Singleton
class MonkeyCheckerService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val masterPlanController: MasterPlanController,
    private val notificationRepository: NotificationRepository,
    private val gson: Gson
) {
    companion object {
        private const val TAG = "MonkeyChecker"
        private const val MASTER_PLAN_FILE = "chapelotas_master_plan.json"
        private const val CHECK_INTERVAL_MS = 5 * 60 * 1000L // 5 minutos
    }

    private var checkJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    /**
     * Iniciar el mono con auto-recuperación
     */
    fun startMonkey() {
        Log.d(TAG, "🐵 Mono despertando...")

        checkJob?.cancel()
        checkJob = scope.launch {
            while (isActive) {
                try {
                    checkNotifications()
                    delay(CHECK_INTERVAL_MS)
                } catch (e: Exception) {
                    Log.e(TAG, "🐵 Error en el mono: ${e.message}")
                    // Si hay error, esperar 30 segundos y continuar
                    delay(30_000)
                }
            }
        }
    }

    /**
     * Detener el mono
     */
    fun stopMonkey() {
        Log.d(TAG, "🐵 Mono durmiendo...")
        checkJob?.cancel()
    }

    /**
     * Checkeo principal cada 5 minutos con manejo robusto de errores
     */
    private suspend fun checkNotifications() {
        try {
            val now = LocalTime.now()
            val nowStr = now.format(DateTimeFormatter.ofPattern("HH:mm"))
            Log.d(TAG, "🐵 Mono checkeando: $nowStr")

            // Cargar plan maestro
            val plan = loadMasterPlan()
            if (plan == null) {
                Log.d(TAG, "🐵 No hay plan maestro, generando uno vacío...")
                val emptyPlan = MasterPlan()
                emptyPlan.proximoCheckeo = now.plusMinutes(5).format(DateTimeFormatter.ofPattern("HH:mm"))
                saveMasterPlan(emptyPlan)
                return
            }

            // Buscar notificaciones pendientes
            val notificacionesPendientes = mutableListOf<EventoConNotificaciones>()
            val eventosCriticosFuturos = mutableListOf<EventoConNotificaciones>()

            plan.eventosHoy.forEach { evento ->
                try {
                    // Checkear notificaciones programadas
                    evento.notificaciones.forEach { notif ->
                        if (!notif.ejecutada && notif.horaExacta <= nowStr) {
                            notificacionesPendientes.add(evento)
                            notif.ejecutada = true
                        }
                    }

                    // Recolectar críticos futuros
                    if (evento.esCritico && evento.horaInicio > nowStr) {
                        eventosCriticosFuturos.add(evento)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "🐵 Error procesando evento ${evento.id}: ${e.message}")
                }
            }

            // Si hay algo que notificar
            if (notificacionesPendientes.isNotEmpty() || eventosCriticosFuturos.isNotEmpty()) {
                Log.d(TAG, "🐵 Hay ${notificacionesPendientes.size} notificaciones + ${eventosCriticosFuturos.size} críticos")

                try {
                    // Generar mensaje con IA
                    val mensaje = masterPlanController.generarMensajeNotificacion(
                        plan = plan,
                        eventosANotificar = notificacionesPendientes,
                        horaActual = nowStr
                    )

                    // Determinar tipo de notificación
                    val esCritica = notificacionesPendientes.any { evento ->
                        evento.notificaciones.any { it.tipo == "alerta_critica" && it.horaExacta <= nowStr }
                    }

                    // Mostrar notificación
                    if (esCritica) {
                        mostrarAlertaCritica(mensaje)
                    } else {
                        mostrarNotificacionNormal(mensaje)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "🐵 Error generando notificación: ${e.message}")
                    // Mostrar notificación básica como fallback
                    val mensajeFallback = buildString {
                        if (notificacionesPendientes.isNotEmpty()) {
                            append("Próximos eventos:\n")
                            notificacionesPendientes.forEach { evento ->
                                append("• ${evento.horaInicio} ${evento.titulo}\n")
                            }
                        }
                        if (eventosCriticosFuturos.isNotEmpty()) {
                            append("\n⚠️ Eventos críticos pendientes: ${eventosCriticosFuturos.size}")
                        }
                    }
                    mostrarNotificacionNormal(mensajeFallback.ifBlank { "Tenés eventos próximos. Revisá tu calendario." })
                }

                // Guardar plan actualizado
                saveMasterPlan(plan)
            }

            // Actualizar próximo checkeo
            plan.proximoCheckeo = now.plusMinutes(5).format(DateTimeFormatter.ofPattern("HH:mm"))
            saveMasterPlan(plan)

            // Actualizar eventos críticos pendientes para la UI
            plan.eventosCriticosPendientes.clear()
            plan.eventosCriticosPendientes.addAll(eventosCriticosFuturos.map { it.titulo })

        } catch (e: Exception) {
            Log.e(TAG, "🐵 Error general en checkNotifications: ${e.message}")
        }
    }

    /**
     * Guardar plan maestro en disco
     */
    fun saveMasterPlan(plan: MasterPlan) {
        try {
            val file = File(context.filesDir, MASTER_PLAN_FILE)
            file.writeText(gson.toJson(plan))
            Log.d(TAG, "🐵 Plan guardado")
        } catch (e: Exception) {
            Log.e(TAG, "🐵 Error guardando plan: ${e.message}")
        }
    }

    /**
     * Cargar plan maestro del disco
     */
    fun loadMasterPlan(): MasterPlan? {
        return try {
            val file = File(context.filesDir, MASTER_PLAN_FILE)
            if (file.exists()) {
                val json = file.readText()
                gson.fromJson(json, MasterPlan::class.java)
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "🐵 Error cargando plan: ${e.message}")
            null
        }
    }

    /**
     * Actualizar plan con eventos nuevos (llamado desde fuera)
     */
    suspend fun procesarEventoNuevo(eventoNuevo: CalendarEvent) {
        val planActual = loadMasterPlan() ?: MasterPlan()

        val (planActualizado, hayConflicto) = masterPlanController.procesarEventoNuevo(
            planActual,
            eventoNuevo
        )

        saveMasterPlan(planActualizado)

        if (hayConflicto) {
            mostrarNotificacionConflicto(planActualizado)
        } else {
            // Notificación simple de evento nuevo
            val mensaje = "📅 Nuevo evento agregado: ${eventoNuevo.title} a las ${
                eventoNuevo.startTime.format(DateTimeFormatter.ofPattern("HH:mm"))
            }"
            mostrarNotificacionNormal(mensaje)
        }
    }

    // Funciones de notificación

    private suspend fun mostrarNotificacionNormal(mensaje: String) {
        val notification = ChapelotasNotification(
            id = System.currentTimeMillis().toString(),
            eventId = 0L,
            scheduledTime = LocalDateTime.now(ZoneId.systemDefault()),
            message = mensaje,
            priority = NotificationPriority.NORMAL,
            type = NotificationType.EVENT_REMINDER
        )

        notificationRepository.showImmediateNotification(notification)
    }

    private suspend fun mostrarAlertaCritica(mensaje: String) {
        val notification = ChapelotasNotification(
            id = System.currentTimeMillis().toString(),
            eventId = 0L,
            scheduledTime = LocalDateTime.now(ZoneId.systemDefault()),
            message = mensaje,
            priority = NotificationPriority.CRITICAL,
            type = NotificationType.CRITICAL_ALERT
        )

        notificationRepository.showImmediateNotification(notification)
    }

    private suspend fun mostrarNotificacionConflicto(plan: MasterPlan) {
        val conflictos = plan.eventosHoy
            .filter { it.conflicto != null }
            .map { "${it.titulo}: ${it.conflicto?.mensaje}" }
            .joinToString("\n")

        val mensaje = "⚠️ CONFLICTOS DETECTADOS:\n$conflictos\n\nRevisá tu calendario"

        mostrarAlertaCritica(mensaje)
    }
}