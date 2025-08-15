package com.chapelotas.app.presentation.viewmodels

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chapelotas.app.domain.debug.DebugLog
import com.chapelotas.app.domain.personality.PersonalityProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

data class PersonalityManagerUiState(
    val personalities: Map<String, String> = emptyMap()
)

@HiltViewModel
class PersonalityManagerViewModel @Inject constructor(
    private val personalityProvider: PersonalityProvider,
    private val debugLog: DebugLog,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(PersonalityManagerUiState())
    val uiState = _uiState.asStateFlow()

    init {
        loadPersonalities()
    }

    private fun loadPersonalities() {
        _uiState.update {
            it.copy(personalities = personalityProvider.getAvailablePersonalities())
        }
    }

    fun importPersonality(uri: Uri) {
        viewModelScope.launch {
            try {
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    val personalitiesDir = File(context.filesDir, "personalities")
                    if (!personalitiesDir.exists()) {
                        personalitiesDir.mkdirs()
                    }

                    // Usamos un nombre de archivo temporal único para evitar conflictos
                    val tempFile = File.createTempFile("personality_", ".json", personalitiesDir)
                    tempFile.outputStream().use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }

                    // TODO: Aquí podrías añadir una validación del JSON antes de renombrarlo
                    // Por ahora, lo renombramos basándonos en el contenido o un UUID si es necesario

                    debugLog.add("🧠 PERSONALITY: Archivo importado a: ${tempFile.absolutePath}")
                    personalityProvider.refreshPersonalities()
                    loadPersonalities()
                }
            } catch (e: Exception) {
                debugLog.add("🧠 PERSONALITY: ❌ Error al importar archivo: ${e.message}")
            }
        }
    }
}