package com.chapelotas.app.domain.entities

import com.google.gson.annotations.SerializedName
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * JSON Maestro que el mono entiende
 */
data class MasterPlan(
    val version: String = LocalDateTime.now(ZoneId.systemDefault()).toString(),
    val usuario: String = "",
    @SerializedName("modo_sarcastico")
    val modoSarcastico: Boolean = false,
    @SerializedName("eventos_hoy")
    val eventosHoy: MutableList<EventoConNotificaciones> = mutableListOf(),
    @SerializedName("proximo_checkeo")
    var proximoCheckeo: String = "",
    @SerializedName("eventos_criticos_pendientes")
    val eventosCriticosPendientes: MutableList<String> = mutableListOf()
)

data class EventoConNotificaciones(
    // Campos del calendario (inmutables)
    val id: String,
    val titulo: String,
    @SerializedName("hora_inicio")
    val horaInicio: String,
    @SerializedName("hora_fin")
    val horaFin: String,
    val lugar: String? = null,

    // Campos modificables por IA
    @SerializedName("es_critico")
    var esCritico: Boolean = false,
    var distancia: String = "cerca", // "en la ofi", "cerca", "lejos"
    @SerializedName("avisos_sugeridos")
    var avisosSugeridos: List<Int> = listOf(15), // minutos antes
    var conflicto: ConflictoInfo? = null,

    // Notificaciones
    val notificaciones: MutableList<NotificacionProgramada> = mutableListOf()
)

data class ConflictoInfo(
    @SerializedName("con_evento_id")
    val conEventoId: String,
    val tipo: String, // "solapamiento", "muy_seguido"
    val mensaje: String
)

data class NotificacionProgramada(
    @SerializedName("hora_exacta")
    val horaExacta: String,
    @SerializedName("minutos_antes")
    val minutosAntes: Int,
    var ejecutada: Boolean = false,
    val tipo: String = "recordatorio", // "recordatorio", "alerta_critica"
    @SerializedName("razon_ia")
    val razonIa: String? = null
)