package ir.k1adili.projectcam.ui.screens.camera

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ir.k1adili.projectcam.data.SettingsRepository
import ir.k1adili.projectcam.data.local.ProjectEntity
import ir.k1adili.projectcam.data.repository.PhotoRepository
import ir.k1adili.projectcam.data.repository.ProjectRepository
import ir.k1adili.projectcam.util.CapturedLocation
import ir.k1adili.projectcam.util.CompassHelper
import ir.k1adili.projectcam.util.LocationHelper
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

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

    /** Latest compass heading in degrees (0=north...), null until the sensor has reported at least once. */
    private val _headingDegrees = MutableStateFlow<Float?>(null)
    val headingDegrees: StateFlow<Float?> = _headingDegrees.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    private var locationJob: Job? = null
    private var headingJob: Job? = null

    /**
     * Starts (or restarts, for the manual retry button) a CONTINUOUS location subscription that
     * stays active for as long as the camera screen is open. This matters for multi-photo
     * sessions: a single one-shot fetch made only when the screen first opens can still be
     * "Loading" (or have failed) by the time the 2nd, 3rd... photo in the same session is taken,
     * silently leaving those photos without GPS data. Continuously listening means every shot
     * uses the freshest fix available, and a fix that arrives late still reaches later photos.
     */
    fun startObservingLocation(context: Context) {
        locationJob?.cancel()
        _locationState.value = LocationUiState.Loading
        locationJob = viewModelScope.launch {
            launch {
                // Only downgrade to "unavailable" if nothing has come in yet after a while;
                // a fix that arrives afterwards will still flip this back to Available.
                delay(12_000L)
                if (_locationState.value is LocationUiState.Loading) {
                    _locationState.value = LocationUiState.Unavailable
                }
            }
            LocationHelper.observeLocationUpdates(context).collect { location ->
                _locationState.value = LocationUiState.Available(location)
            }
        }
    }

    fun stopObservingLocation() {
        locationJob?.cancel()
        locationJob = null
    }

    /** Same continuous-while-open reasoning as location: the compass heading at the exact moment of each shot. */
    fun startObservingHeading(context: Context) {
        headingJob?.cancel()
        headingJob = viewModelScope.launch {
            CompassHelper.observeHeadingDegrees(context).collect { heading ->
                _headingDegrees.value = heading
            }
        }
    }

    fun stopObservingHeading() {
        headingJob?.cancel()
        headingJob = null
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
                    headingDegrees = headingDegrees.value,
                    photographerName = photographerName,
                    note = note
                )
                onSaved()
            } finally {
                _isSaving.value = false
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        locationJob?.cancel()
        headingJob?.cancel()
    }
}
