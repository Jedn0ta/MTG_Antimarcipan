package com.example.antimarcipan.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.antimarcipan.data.update.UpdateRepository
import com.example.antimarcipan.data.update.UpdateStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

enum class UpdatePhase { IDLE, CHECKING, DOWNLOADING, READY_TO_INSTALL }

data class SettingsUiState(
    val currentSha: String = "",
    val phase: UpdatePhase = UpdatePhase.IDLE,
    val status: UpdateStatus? = null,
    val downloadProgress: Float = 0f,
    val downloadedApk: File? = null,
    val message: String? = null,
)

class SettingsViewModel(
    private val updates: UpdateRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsUiState(currentSha = updates.currentShortSha))
    val state: StateFlow<SettingsUiState> = _state.asStateFlow()

    fun checkForUpdate() {
        viewModelScope.launch {
            _state.value = _state.value.copy(phase = UpdatePhase.CHECKING, message = null)
            val result = updates.check()
            _state.value = _state.value.copy(
                phase = UpdatePhase.IDLE,
                status = result,
                message = when (result) {
                    is UpdateStatus.UpToDate -> "Máš nejnovější verzi."
                    is UpdateStatus.Available -> "K dispozici je novější verze."
                    is UpdateStatus.Error -> "Chyba: ${result.message}"
                },
            )
        }
    }

    fun downloadUpdate() {
        val available = _state.value.status as? UpdateStatus.Available ?: return
        viewModelScope.launch {
            _state.value = _state.value.copy(
                phase = UpdatePhase.DOWNLOADING,
                downloadProgress = 0f,
                message = null,
            )
            runCatching {
                updates.download(available.downloadUrl) { p ->
                    _state.value = _state.value.copy(downloadProgress = p.coerceIn(0f, 1f))
                }
            }.onSuccess { file ->
                _state.value = _state.value.copy(
                    phase = UpdatePhase.READY_TO_INSTALL,
                    downloadedApk = file,
                    message = "Staženo. Spusť instalaci.",
                )
            }.onFailure { e ->
                _state.value = _state.value.copy(
                    phase = UpdatePhase.IDLE,
                    message = "Stažení selhalo: ${e.message}",
                )
            }
        }
    }

    /** Vrací true, pokud lze rovnou instalovat; false = je potřeba povolit neznámé zdroje. */
    fun tryInstall(): Boolean {
        val apk = _state.value.downloadedApk ?: return true
        return if (updates.canInstallUnknownApps()) {
            updates.install(apk)
            true
        } else {
            false
        }
    }

    fun unknownSourcesSettingsIntent() = updates.unknownSourcesSettingsIntent()
}
