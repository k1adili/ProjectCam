package ir.k1adili.projectcam.ui.screens.photo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ir.k1adili.projectcam.data.local.PhotoEntity
import ir.k1adili.projectcam.data.repository.PhotoRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

class PhotoViewerViewModel(
    private val photoId: Long,
    private val photoRepository: PhotoRepository
) : ViewModel() {

    val photo: StateFlow<PhotoEntity?> = photoRepository.observePhoto(photoId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun absoluteFile(photo: PhotoEntity): File = photoRepository.absoluteFile(photo)

    /**
     * Suspends until the write actually completes. IMPORTANT: callers that also navigate away
     * right after saving must await this (e.g. via rememberCoroutineScope in the composable, NOT
     * viewModelScope.launch fire-and-forget) before calling their back/navigate callback. Popping
     * the back stack clears this ViewModel and cancels viewModelScope almost immediately, so a
     * fire-and-forget viewModelScope.launch here could get cancelled mid-write - which was
     * exactly why notes silently failed to save when the user tapped back right after typing.
     */
    suspend fun updateNote(photo: PhotoEntity, note: String) {
        photoRepository.updateNote(photo, note)
    }

    fun delete(photo: PhotoEntity, onDeleted: () -> Unit) {
        viewModelScope.launch {
            photoRepository.deletePhotos(listOf(photo))
            onDeleted()
        }
    }
}
