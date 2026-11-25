package com.example.antimarcipan.settings

import com.example.antimarcipan.i18n.tr

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.antimarcipan.data.backup.BackupRepository
import com.example.antimarcipan.data.settings.AppBackground
import com.example.antimarcipan.data.settings.AppSettings
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
    private val backup: BackupRepository,
    private val settings: AppSettings,
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsUiState(currentSha = updates.currentShortSha))
    val state: StateFlow<SettingsUiState> = _state.asStateFlow()

    /** Cílový jazyk překladu karet ve skenu. */

    /** Krajinné pozadí aplikace. */
    val background: StateFlow<AppBackground> = settings.background

    fun setBackground(bg: AppBackground) = settings.setBackground(bg)

    fun suggestedBackupName(): String = backup.suggestedFileName()

    fun exportBackup(uri: Uri, onResult: (String) -> Unit) {
        viewModelScope.launch {
            backup.exportTo(uri)
                .onSuccess { onResult(tr("Záloha uložena.")) }
                .onFailure { onResult(tr("Záloha selhala: {0}", (it.message))) }
        }
    }

    fun importBackup(uri: Uri, onResult: (String) -> Unit) {
        viewModelScope.launch {
            backup.importFrom(uri)
                .onSuccess { onResult(tr("Obnoveno. Restartuj aplikaci, aby se data načetla.")) }
                .onFailure { onResult("Obnova selhala: ${it.message}") }
        }
    }

    fun checkForUpdate() {
        viewModelScope.launch {
            _state.value = _state.value.copy(phase = UpdatePhase.CHECKING, message = null)
            val result = updates.check()
            _state.value = _state.value.copy(
                phase = UpdatePhase.IDLE,
                status = result,
                message = when (result) {
                    is UpdateStatus.UpToDate -> tr("Máš nejnovější verzi.")
                    is UpdateStatus.Available -> tr("K dispozici je novější verze.")
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
                    message = tr("Staženo. Spusť instalaci."),
                )
            }.onFailure { e ->
                _state.value = _state.value.copy(
                    phase = UpdatePhase.IDLE,
                    message = tr("Stažení selhalo: {0}", (e.message)),
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
