package ir.k1adili.projectcam.ui.screens.projects

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ir.k1adili.projectcam.data.local.PhotoEntity
import ir.k1adili.projectcam.data.local.ProjectEntity
import ir.k1adili.projectcam.data.repository.PhotoRepository
import ir.k1adili.projectcam.data.repository.ProjectRepository
import ir.k1adili.projectcam.export.KmlExporter
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

class ProjectDetailViewModel(
    private val projectId: Long,
    private val projectRepository: ProjectRepository,
    private val photoRepository: PhotoRepository
) : ViewModel() {

    val project: StateFlow<ProjectEntity?> = projectRepository.observeProject(projectId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val photos: StateFlow<List<PhotoEntity>> = photoRepository.observePhotos(projectId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedIds: StateFlow<Set<Long>> = _selectedIds.asStateFlow()

    private val _exportedFile = MutableStateFlow<KmlExporter.ExportResult?>(null)
    val exportedFile: StateFlow<KmlExporter.ExportResult?> = _exportedFile.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    fun toggleSelection(photoId: Long) {
        _selectedIds.value = _selectedIds.value.toMutableSet().apply {
            if (!add(photoId)) remove(photoId)
        }
    }

    fun selectAll() {
        _selectedIds.value = photos.value.map { it.id }.toSet()
    }

    fun clearSelection() {
        _selectedIds.value = emptySet()
    }

    fun deleteSelected() {
        viewModelScope.launch {
            val toDelete = photos.value.filter { it.id in _selectedIds.value }
            photoRepository.deletePhotos(toDelete)
            clearSelection()
        }
    }

    /** [photosToExport] = null means "export the whole project"; otherwise export exactly this set. */
    fun exportKml(context: Context, photosToExport: List<PhotoEntity>?) {
        viewModelScope.launch {
            val currentProject = project.value ?: run {
                _errorMessage.value = "پروژه یافت نشد"
                return@launch
            }
            val targetPhotos = photosToExport ?: photos.value
            if (targetPhotos.isEmpty()) {
                _errorMessage.value = "هیچ عکسی برای خروجی گرفتن انتخاب نشده"
                return@launch
            }

            val result = withContext(Dispatchers.IO) {
                val safeName = currentProject.name.replace(Regex("[^A-Za-z0-9._\\-\\u0600-\\u06FF ]"), "_")
                val outputFile = File(
                    PhotoFileUtils.exportsDir(context),
                    "${safeName}_${System.currentTimeMillis()}.zip"
                )
                KmlExporter.export(currentProject, targetPhotos, outputFile) { photo ->
                    photoRepository.absoluteFile(photo)
                }
            }
            clearSelection()
            _exportedFile.value = result
        }
    }

    fun clearExportedFile() {
        _exportedFile.value = null
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun absoluteFile(photo: PhotoEntity): File = photoRepository.absoluteFile(photo)
}
