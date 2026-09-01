package ir.k1adili.projectcam.ui.screens.projects

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ir.k1adili.projectcam.data.local.ProjectWithPhotoCount
import ir.k1adili.projectcam.data.repository.PhotoRepository
import ir.k1adili.projectcam.data.repository.ProjectRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ProjectListViewModel(
    private val projectRepository: ProjectRepository,
    private val photoRepository: PhotoRepository
) : ViewModel() {

    val projects: StateFlow<List<ProjectWithPhotoCount>> = projectRepository.observeProjectsWithPhotoCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    fun createProject(name: String, note: String, onCreated: (Long) -> Unit) {
        viewModelScope.launch {
            runCatching { projectRepository.createProject(name, note) }
                .onSuccess { onCreated(it) }
                .onFailure { _errorMessage.value = it.message }
        }
    }

    fun renameProject(project: ProjectWithPhotoCount, newName: String, newNote: String) {
        viewModelScope.launch {
            val entity = projectRepository.getProject(project.id) ?: return@launch
            runCatching { projectRepository.renameProject(entity, newName, newNote) }
                .onFailure { _errorMessage.value = it.message }
        }
    }

    /** Deletes photo files first (cascade only removes DB rows, not files on disk), then the project. */
    fun deleteProject(projectId: Long) {
        viewModelScope.launch {
            val photos = photoRepository.getPhotosByProjectOnce(projectId)
            photoRepository.deletePhotos(photos)
            projectRepository.deleteProjectById(projectId)
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }
}
