package ir.k1adili.projectcam.ui.screens.camera

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ir.k1adili.projectcam.data.SettingsRepository
import ir.k1adili.projectcam.data.local.ProjectEntity
import ir.k1adili.projectcam.data.repository.PhotoRepository
import ir.k1adili.projectcam.data.repository.ProjectRepository
import ir.k1adili.projectcam.util.CapturedLocation
import ir.k1adili.projectcam.util.LocationHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface LocationUiState {
    data object Loading : LocationUiState
    data class Available(val location: CapturedLocation) : LocationUiState
    data object Unavailable : LocationUiState
}

class CameraViewModel(
    private val projectId: Long,
    private val projectRepository: ProjectRepository,
    private val photoRepository: PhotoRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val project: StateFlow<ProjectEntity?> = projectRepository.observeProject(projectId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val photographerName: StateFlow<String> = settingsRepository.photographerName
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    private val _locationState = MutableStateFlow<LocationUiState>(LocationUiState.Loading)
    val locationState: StateFlow<LocationUiState> = _locationState.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    fun refreshLocation(context: Context) {
        _locationState.value = LocationUiState.Loading
        viewModelScope.launch {
            val location = withContext(Dispatchers.IO) { LocationHelper.getCurrentLocation(context) }
            _locationState.value = if (location != null) {
                LocationUiState.Available(location)
            } else {
                LocationUiState.Unavailable
            }
        }
    }

    fun savePhoto(
        fileName: String,
        photographerName: String,
        note: String = "",
        onSaved: () -> Unit
    ) {
        viewModelScope.launch {
            _isSaving.value = true
            try {
                val loc = (locationState.value as? LocationUiState.Available)?.location
                photoRepository.addPhoto(
                    projectId = projectId,
                    fileName = fileName,
                    latitude = loc?.latitude,
                    longitude = loc?.longitude,
                    accuracyMeters = loc?.accuracyMeters,
                    photographerName = photographerName,
                    note = note
                )
                onSaved()
            } finally {
                _isSaving.value = false
            }
        }
    }
}
