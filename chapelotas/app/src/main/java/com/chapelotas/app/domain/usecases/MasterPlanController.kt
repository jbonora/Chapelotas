package com.chapelotas.app.domain.usecases

import com.chapelotas.app.domain.entities.*
import com.chapelotas.app.domain.repositories.AIRepository
import com.google.gson.Gson
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Controlador principal que maneja el JSON maestro
 * Este es el cerebro que coordina todo
 */
@Singleton
class MasterPlanController @Inject constructor(
    private val aiRepository: AIRepository,
    private val gson: Gson
) {

    companion object {
        // Prompts configurables para fácil ajuste
        const val PROMPT_ANALISIS_MATUTINO = """
            Sos el módulo analítico de Chapelotas.
            Analiza estos eventos del calendario y sugiere mejoras.
            
            JSON: %s
            
            SOLO PUEDES MODIFICAR:
            - es_critico: true/false (basado en título/contexto)
            - avisos_sugeridos: array de minutos antes
            - conflicto: detectar solapamientos o tiempos ajustados
            
            NO MODIFIQUES: id, titulo, hora_inicio, hora_fin, lugar
            
            Responde SOLO el JSON actualizado.
        """

        const val PROMPT_EVENTO_NUEVO = """
            Sos el módulo analítico de Chapelotas.
            Actualiza el calendario con un evento nuevo.
            
            JSON ACTUAL: %s
            EVENTO NUEVO: %s
            
            TAREAS:
            1. Agregar el evento al JSON
            2. Sugerir es_critico y avisos_sugeridos según:
               - Título sugiere importancia
               - Distancia del lugar (si está lejos, más tiempo)
            3. IMPORTANTE: Detectar conflictos con eventos existentes
            4. Si hay conflicto, actualizar campo "conflicto" en AMBOS eventos
            
            Responde SOLO el JSON actualizado.
        """

        const val PROMPT_GENERAR_MENSAJE = """
            Sos Chapelotas, secretaria ejecutiva de %s.
            %s
            Hora actual: %s
            
            EVENTOS A NOTIFICAR: %s
            EVENTOS CRÍTICOS PENDIENTES: %s
            
            Genera un mensaje claro mencionando:
            - Hora, título y lugar de próximos eventos
            - SIEMPRE incluir eventos críticos pendientes
            - Si hay conflictos, mencionarlos CLARAMENTE
            - Máximo 4 líneas
            
            %s
        """
    }

    /**
     * CASO 1: Análisis matutino inicial
     */
    suspend fun analizarDiaInicial(eventos: List<CalendarEvent>): MasterPlan {
        val planInicial = crearPlanDesdeEventos(eventos)
        val jsonInicial = gson.toJson(planInicial)

        return try {
            val respuestaIA = aiRepository.callOpenAIForJSON(
                prompt = PROMPT_ANALISIS_MATUTINO.format(jsonInicial),
                temperature = 0.3 // Análisis preciso
            )

            gson.fromJson(respuestaIA, MasterPlan::class.java)
        } catch (e: Exception) {
            // Si falla IA, devolver plan básico
            planInicial
        }
    }

    /**
     * CASO 2: Procesar evento nuevo
     */
    suspend fun procesarEventoNuevo(
        planActual: MasterPlan,
        eventoNuevo: CalendarEvent
    ): Pair<MasterPlan, Boolean> {
        val eventoJson = gson.toJson(convertirAEventoConNotificaciones(eventoNuevo))
        val planJson = gson.toJson(planActual)

        return try {
            val respuestaIA = aiRepository.callOpenAIForJSON(
                prompt = PROMPT_EVENTO_NUEVO.format(planJson, eventoJson),
                temperature = 0.3
            )

            val planActualizado = gson.fromJson(respuestaIA, MasterPlan::class.java)
            val hayConflicto = planActualizado.eventosHoy.any { it.conflicto != null }

            Pair(planActualizado, hayConflicto)
        } catch (e: Exception) {
            // Fallback: agregar manualmente sin IA
            planActual.eventosHoy.add(convertirAEventoConNotificaciones(eventoNuevo))
            Pair(planActual, false)
        }
    }

    /**
     * CASO 3: Generar mensaje para notificación
     */
    suspend fun generarMensajeNotificacion(
        plan: MasterPlan,
        eventosANotificar: List<EventoConNotificaciones>,
        horaActual: String = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))
    ): String {
        val eventosCriticos = plan.eventosHoy
            .filter { it.esCritico && it.horaInicio > horaActual }
            .map { "${it.titulo} a las ${it.horaInicio}" }

        val modoTexto = if (plan.modoSarcastico) "Modo: sarcástico argentino" else "Modo: profesional"
        val instruccionSarcasmo = if (plan.modoSarcastico)
            "Usá humor argentino, podés ser irónico pero siempre claro con la info"
        else
            "Sé profesional y cordial"

        val eventosTexto = eventosANotificar.joinToString("\n") { evento ->
            "- ${evento.horaInicio} ${evento.titulo}" +
                    (evento.lugar?.let { " en $it" } ?: "") +
                    (evento.conflicto?.let { " [CONFLICTO: ${it.mensaje}]" } ?: "")
        }

        val prompt = PROMPT_GENERAR_MENSAJE.format(
            plan.usuario,
            modoTexto,
            horaActual,
            eventosTexto,
            eventosCriticos.joinToString(", "),
            instruccionSarcasmo
        )

        return aiRepository.callOpenAIForText(
            prompt = prompt,
            temperature = 0.8 // Más creatividad para mensajes
        )
    }

    // Funciones auxiliares

    private fun crearPlanDesdeEventos(eventos: List<CalendarEvent>): MasterPlan {
        val plan = MasterPlan()

        eventos.forEach { evento ->
            plan.eventosHoy.add(convertirAEventoConNotificaciones(evento))
        }

        return plan
    }

    private fun convertirAEventoConNotificaciones(evento: CalendarEvent): EventoConNotificaciones {
        val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

        return EventoConNotificaciones(
            id = evento.id.toString(),
            titulo = evento.title,
            horaInicio = evento.startTime.format(timeFormatter),
            horaFin = evento.endTime.format(timeFormatter),
            lugar = evento.location,
            esCritico = evento.isCritical,
            avisosSugeridos = listOf(15) // Default
        )
    }
}