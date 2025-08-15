package com.chapelotas.app.domain.personality

import android.content.Context
import com.chapelotas.app.domain.debug.DebugLog
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
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
    private val personalities = mutableMapOf<String, Personality>()

    init {
        loadAllPersonalities()
    }

    private fun loadAllPersonalities() {
        personalities.clear()
        loadFromAssets()
        loadFromInternalStorage()
    }

    private fun loadFromAssets() {
        try {
            val jsonString = context.assets.open("personalities.json").bufferedReader().use { it.readText() }
            val type = object : TypeToken<Map<String, Personality>>() {}.type
            val personalitiesFromAssets: Map<String, Personality> = gson.fromJson(jsonString, type)
            personalities.putAll(personalitiesFromAssets)
            debugLog.add("🧠 PERSONALITY: Cargadas ${personalitiesFromAssets.size} personalidades desde assets.")
        } catch (e: Exception) {
            debugLog.add("🧠 PERSONALITY: ❌ Error fatal al cargar personalities.json: ${e.message}")
        }
    }

    private fun loadFromInternalStorage() {
        val personalitiesDir = File(context.filesDir, "personalities")
        if (!personalitiesDir.exists()) {
            personalitiesDir.mkdirs()
            debugLog.add("🧠 PERSONALITY: Creado el directorio de personalidades internas.")
            return
        }

        var loadedCount = 0
        personalitiesDir.listFiles { _, name -> name.endsWith(".json") }?.forEach { file ->
            try {
                val jsonString = file.readText()
                val personality = gson.fromJson(jsonString, Personality::class.java)
                val personalityId = file.nameWithoutExtension
                personalities[personalityId] = personality // Sobrescribe si ya existe
                loadedCount++
            } catch (e: Exception) {
                debugLog.add("🧠 PERSONALITY: ❌ Error al cargar ${file.name}: ${e.message}")
            }
        }
        if (loadedCount > 0) {
            debugLog.add("🧠 PERSONALITY: Cargadas y/o actualizadas $loadedCount personalidades desde el almacenamiento interno.")
        }
    }

    fun refreshPersonalities() {
        loadAllPersonalities()
    }

    fun getAvailablePersonalities(): Map<String, String> {
        return personalities.mapValues { it.value.displayName }
    }

    fun get(
        personalityKey: String,
        contextKey: String,
        placeholders: Map<String, String> = emptyMap()
    ): String {
        val personality = personalities[personalityKey]
        if (personality == null) {
            debugLog.add("🧠 PERSONALITY: ⚠️ Personalidad no encontrada: $personalityKey. Usando 'sarcastic' por defecto.")
            return get("sarcastic", contextKey, placeholders)
        }

        val keys = contextKey.split('.')
        val mainContext = keys.getOrNull(0)
        val subContext = keys.getOrNull(1)

        val options: List<String>? = when (mainContext) {
            "alarm_greeting" -> personality.alarm_greeting["options"]
            "upcoming_reminder" -> personality.upcoming_reminder[subContext]
            "ongoing_reminder" -> personality.ongoing_reminder[subContext]
            "delayed_reminder" -> personality.delayed_reminder[subContext]
            "action_confirmation" -> personality.action_confirmation?.get(subContext)
            else -> null
        }

        if (options.isNullOrEmpty()) {
            debugLog.add("🧠 PERSONALITY: ⚠️ Frases no encontradas para el contexto: $contextKey en personalidad '$personalityKey'.")
            return when (mainContext) {
                "action_confirmation" -> "¡Acción confirmada!"
                else -> "Recordatorio para tu tarea."
            }
        }

        var selectedPhrase = options.random()

        placeholders.forEach { (key, value) ->
            selectedPhrase = selectedPhrase.replace(key, value, ignoreCase = true)
        }

        return selectedPhrase
    }
}