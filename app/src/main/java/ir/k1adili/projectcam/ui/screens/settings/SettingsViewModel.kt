package ir.k1adili.projectcam.ui.screens.settings

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ir.k1adili.projectcam.data.SettingsRepository
import ir.k1adili.projectcam.data.local.AppDatabase
import ir.k1adili.projectcam.export.BackupManager
import ir.k1adili.projectcam.util.PhotoFileUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

sealed interface BackupUiEvent {
    data class ExportReady(val file: File) : BackupUiEvent
    data class ExportFailed(val message: String) : BackupUiEvent
    data class ImportSucceeded(val projectCount: Int, val photoCount: Int) : BackupUiEvent
    data class ImportFailed(val message: String) : BackupUiEvent
}

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val database: AppDatabase
) : ViewModel() {

    val photographerName: StateFlow<String> = settingsRepository.photographerName
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    private val _isBusy = MutableStateFlow(false)
    val isBusy: StateFlow<Boolean> = _isBusy.asStateFlow()

    private val _events = MutableStateFlow<BackupUiEvent?>(null)
    val events: StateFlow<BackupUiEvent?> = _events.asStateFlow()

    fun setPhotographerName(name: String) {
        viewModelScope.launch {
            settingsRepository.setPhotographerName(name)
        }
    }

    fun exportBackup(context: Context) {
        viewModelScope.launch {
            _isBusy.value = true
            runCatching {
                withContext(Dispatchers.IO) {
                    val outputFile = File(
                        PhotoFileUtils.exportsDir(context),
                        "ProjectCam_Backup_${System.currentTimeMillis()}.zip"
                    )
                    BackupManager.exportBackup(context, database, outputFile)
                    outputFile
                }
            }.onSuccess { file ->
                _isBusy.value = false
                _events.value = BackupUiEvent.ExportReady(file)
            }.onFailure {
                _isBusy.value = false
                _events.value = BackupUiEvent.ExportFailed(it.message ?: "خطا در تهیه پشتیبان")
            }
        }
    }

    fun importBackup(context: Context, uri: Uri) {
        viewModelScope.launch {
            _isBusy.value = true
            runCatching {
                withContext(Dispatchers.IO) {
                    val tempFile = File(context.cacheDir, "restore_${System.currentTimeMillis()}.zip")
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        tempFile.outputStream().use { output -> input.copyTo(output) }
                    } ?: throw IllegalStateException("فایل قابل خواندن نیست")
                    val summary = BackupManager.importBackup(context, database, tempFile)
                    tempFile.delete()
                    summary
                }
            }.onSuccess { summary ->
                _isBusy.value = false
                _events.value = BackupUiEvent.ImportSucceeded(summary.projectCount, summary.photoCount)
            }.onFailure {
                _isBusy.value = false
                _events.value = BackupUiEvent.ImportFailed(it.message ?: "خطا در بازیابی پشتیبان")
            }
        }
    }

    fun clearEvent() {
        _events.value = null
    }
}
