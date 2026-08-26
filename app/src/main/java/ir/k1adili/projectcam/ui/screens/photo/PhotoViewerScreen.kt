package ir.k1adili.projectcam.ui.screens.photo

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import coil.compose.AsyncImage
import ir.k1adili.projectcam.ProjectCamApp
import ir.k1adili.projectcam.R
import ir.k1adili.projectcam.ui.components.ConfirmDialog
import ir.k1adili.projectcam.ui.theme.Spacing
import ir.k1adili.projectcam.util.JalaliDateUtils
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.ZoneId
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoViewerScreen(
    photoId: Long,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val app = context.applicationContext as ProjectCamApp
    val viewModel: PhotoViewerViewModel = viewModel(
        key = "photo_$photoId",
        factory = viewModelFactory {
            initializer { PhotoViewerViewModel(photoId, app.photoRepository) }
        }
    )

    val photo by viewModel.photo.collectAsState()
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var noteText by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(photo?.id) {
        if (noteText == null) noteText = photo?.note.orEmpty()
    }

    // Debounced note save.
    LaunchedEffect(noteText) {
        val currentPhoto = photo
        if (currentPhoto != null && noteText != null && noteText != currentPhoto.note) {
            delay(600)
            viewModel.updateNote(currentPhoto, noteText.orEmpty())
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBackIosNew, contentDescription = stringResource(R.string.back))
                    }
                },
                actions = {
                    IconButton(onClick = { showDeleteConfirm = true }) {
                        Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.delete), tint = MaterialTheme.colorScheme.error)
                    }
                }
            )
        }
    ) { padding ->
        val currentPhoto = photo
        if (currentPhoto == null) {
            Box(modifier = Modifier.fillMaxSize())
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
            ) {
                AsyncImage(
                    model = viewModel.absoluteFile(currentPhoto),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(360.dp)
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(Spacing.lg),
                    verticalArrangement = Arrangement.spacedBy(Spacing.sm)
                ) {
                    Text(
                        text = stringResource(R.string.photographer_label_format, currentPhoto.photographerName),
                        style = MaterialTheme.typography.bodyLarge
                    )

                    val capturedDate = JalaliDateUtils.formatDateTimeNumeric(
                        Instant.ofEpochMilli(currentPhoto.capturedAtEpochMillis).atZone(ZoneId.systemDefault()).toLocalDateTime()
                    )
                    Text(
                        text = stringResource(R.string.date_label_format, capturedDate),
                        style = MaterialTheme.typography.bodyLarge
                    )

                    val locationText = if (currentPhoto.latitude != null && currentPhoto.longitude != null) {
                        String.format(Locale.US, "%.6f, %.6f", currentPhoto.latitude, currentPhoto.longitude)
                    } else {
                        stringResource(R.string.location_unavailable)
                    }
                    Text(
                        text = stringResource(R.string.location_label_format, locationText),
                        style = MaterialTheme.typography.bodyLarge
                    )

                    OutlinedTextField(
                        value = noteText.orEmpty(),
                        onValueChange = { noteText = it },
                        label = { Text(stringResource(R.string.photo_note_label)) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }

    if (showDeleteConfirm) {
        photo?.let { currentPhoto ->
            ConfirmDialog(
                title = stringResource(R.string.delete_photos_confirm_title),
                message = stringResource(R.string.delete_photos_confirm_message),
                onConfirm = {
                    showDeleteConfirm = false
                    viewModel.delete(currentPhoto, onDeleted = onBack)
                },
                onDismiss = { showDeleteConfirm = false }
            )
        }
    }
}
