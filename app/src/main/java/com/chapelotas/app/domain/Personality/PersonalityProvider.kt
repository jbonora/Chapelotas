package com.chapelotas.app.domain.personality

import android.content.Context
import com.chapelotas.app.domain.debug.DebugLog
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

data class Personality(
    val displayName: String,
    val description: String,
    val alarm_greeting: Map<String, List<String>>,
    val upcoming_reminder: Map<String, List<String>>,
    val ongoing_reminder: Map<String, List<String>>,
    val delayed_reminder: Map<String, List<String>>,
    val action_confirmation: Map<String, List<String>>? = null
)

@Singleton
class PersonalityProvider @Inject constructor(
    @ApplicationContext private val context: Context,
    private val debugLog: DebugLog
) {

    private val gson = Gson()
    private var personalities: Map<String, Personality> = emptyMap()

    init {
        loadPersonalities()
    }

    private fun loadPersonalities() {
        try {
            val jsonString = context.assets.open("personalities.json").bufferedReader().use { it.readText() }
            val type = object : TypeToken<Map<String, Personality>>() {}.type
            personalities = gson.fromJson(jsonString, type)
            debugLog.add("🧠 PERSONALITY: Base de datos de personalidades cargada con éxito.")
        } catch (e: Exception) {
            debugLog.add("🧠 PERSONALITY: ❌ Error fatal al cargar personalities.json: ${e.message}")
        }
    }

    fun get(
        personalityKey: String,
        contextKey: String,
        placeholders: Map<String, String> = emptyMap()
    ): String {
        val personality = personalities[personalityKey]
        if (personality == null) {
            debugLog.add("🧠 PERSONALITY: ⚠️ Personalidad no encontrada: $personalityKey")
            return "Error: Personalidad no encontrada."
        }

        val keys = contextKey.split('.')
        val mainContext = keys.getOrNull(0)
        val subContext = keys.getOrNull(1)

        val options: List<String>? = when (mainContext) {
            "alarm_greeting" -> personality.alarm_greeting["options"]
            "upcoming_reminder" -> personality.upcoming_reminder[subContext]
            "ongoing_reminder" -> personality.ongoing_reminder[subContext]
            "delayed_reminder" -> personality.delayed_reminder[subContext]
            // Se añade el caso "reset" a las confirmaciones de acción
            "action_confirmation" -> personality.action_confirmation?.get(subContext)
            else -> null
        }

        if (options.isNullOrEmpty()) {
            debugLog.add("🧠 PERSONALITY: ⚠️ Frases no encontradas para el contexto: $contextKey")
            // Devuelve un mensaje genérico si no encuentra el contexto específico
            return when (mainContext) {
                "action_confirmation" -> "¡Acción confirmada!"
                else -> "Contexto no encontrado."
            }
        }

        var selectedPhrase = options.random()

        placeholders.forEach { (key, value) ->
            selectedPhrase = selectedPhrase.replace(key, value, ignoreCase = true)
        }

        return selectedPhrase
    }
}