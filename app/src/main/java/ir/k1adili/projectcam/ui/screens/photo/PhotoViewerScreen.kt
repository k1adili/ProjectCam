package ir.k1adili.projectcam.ui.screens.photo

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import coil.compose.AsyncImage
import ir.k1adili.projectcam.ProjectCamApp
import ir.k1adili.projectcam.R
import ir.k1adili.projectcam.ui.components.ConfirmDialog
import ir.k1adili.projectcam.ui.theme.Spacing
import ir.k1adili.projectcam.util.GallerySaver
import ir.k1adili.projectcam.util.JalaliDateUtils
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.util.Locale

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
    val coroutineScope = rememberCoroutineScope()

    val photo by viewModel.photo.collectAsState()
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var noteText by remember { mutableStateOf<String?>(null) }
    var showInfo by remember { mutableStateOf(true) }
    var noteSaved by remember { mutableStateOf(true) }

    LaunchedEffect(photo?.id) {
        if (noteText == null) noteText = photo?.note.orEmpty()
    }

    fun saveNoteNow() {
        val currentPhoto = photo
        val text = noteText
        if (currentPhoto != null && text != null && text != currentPhoto.note) {
            viewModel.updateNote(currentPhoto, text)
            noteSaved = true
        }
    }

    fun handleBack() {
        saveNoteNow()
        onBack()
    }

    androidx.activity.compose.BackHandler(enabled = true) {
        handleBack()
    }

    val needsLegacyStoragePermission = Build.VERSION.SDK_INT < Build.VERSION_CODES.Q
    val storagePermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            photo?.let { p ->
                val ok = GallerySaver.saveToGallery(context, viewModel.absoluteFile(p), p.fileName)
                Toast.makeText(
                    context,
                    context.getString(if (ok) R.string.photo_saved_to_gallery else R.string.photo_save_failed),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    fun onSaveClicked() {
        val currentPhoto = photo ?: return
        if (needsLegacyStoragePermission &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
        ) {
            storagePermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            return
        }
        coroutineScope.launch {
            val ok = GallerySaver.saveToGallery(context, viewModel.absoluteFile(currentPhoto), currentPhoto.fileName)
            Toast.makeText(
                context,
                context.getString(if (ok) R.string.photo_saved_to_gallery else R.string.photo_save_failed),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    val currentPhoto = photo
    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        if (currentPhoto != null) {
            ZoomableImage(
                model = viewModel.absoluteFile(currentPhoto),
                resetKey = currentPhoto.id,
                onTap = { showInfo = !showInfo },
                modifier = Modifier.fillMaxSize()
            )
        }

        // Top overlay: back / save / delete.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .systemBarsPadding()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            OverlayIconButton(
                icon = Icons.Filled.ArrowBackIosNew,
                contentDescription = stringResource(R.string.back),
                onClick = { handleBack() }
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OverlayIconButton(
                    icon = Icons.Filled.Download,
                    contentDescription = stringResource(R.string.save_photo),
                    onClick = { onSaveClicked() }
                )
                OverlayIconButton(
                    icon = Icons.Filled.Delete,
                    contentDescription = stringResource(R.string.delete),
                    tint = Color(0xFFFF6B6B),
                    onClick = { showDeleteConfirm = true }
                )
            }
        }

        // Bottom overlay: metadata + note, toggled by tapping the image.
        if (currentPhoto != null && showInfo) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(Color.Black.copy(alpha = 0.6f))
                    .systemBarsPadding()
                    .padding(Spacing.lg),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                Text(
                    text = stringResource(R.string.photographer_label_format, currentPhoto.photographerName),
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White
                )

                val capturedDate = JalaliDateUtils.formatDateTimeNumeric(
                    Instant.ofEpochMilli(currentPhoto.capturedAtEpochMillis).atZone(ZoneId.systemDefault()).toLocalDateTime()
                )
                Text(
                    text = stringResource(R.string.date_label_format, capturedDate),
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White
                )

                val locationText = if (currentPhoto.latitude != null && currentPhoto.longitude != null) {
                    String.format(Locale.US, "%.6f, %.6f", currentPhoto.latitude, currentPhoto.longitude)
                } else {
                    stringResource(R.string.location_unavailable)
                }
                Text(
                    text = stringResource(R.string.location_label_format, locationText),
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White
                )

                OutlinedTextField(
                    value = noteText.orEmpty(),
                    onValueChange = {
                        noteText = it
                        noteSaved = (it == currentPhoto.note)
                    },
                    label = { Text(stringResource(R.string.photo_note_label)) },
                    trailingIcon = {
                        IconButton(
                            onClick = {
                                saveNoteNow()
                                Toast.makeText(context, context.getString(R.string.note_saved), Toast.LENGTH_SHORT).show()
                            },
                            enabled = !noteSaved
                        ) {
                            Icon(
                                Icons.Filled.Check,
                                contentDescription = stringResource(R.string.save),
                                tint = if (noteSaved) Color.White.copy(alpha = 0.4f) else MaterialTheme.colorScheme.primary
                            )
                        }
                    },
                    colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedLabelColor = Color.White,
                        unfocusedLabelColor = Color.White.copy(alpha = 0.7f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }

    if (showDeleteConfirm) {
        photo?.let { p ->
            ConfirmDialog(
                title = stringResource(R.string.delete_photos_confirm_title),
                message = stringResource(R.string.delete_photos_confirm_message),
                onConfirm = {
                    showDeleteConfirm = false
                    viewModel.delete(p, onDeleted = onBack)
                },
                onDismiss = { showDeleteConfirm = false }
            )
        }
    }
}

@Composable
private fun OverlayIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    tint: Color = Color.White,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.45f))
    ) {
        Icon(icon, contentDescription = contentDescription, tint = tint)
    }
}

/** Pinch-to-zoom + pan + double-tap-to-reset image, resetting its transform whenever [resetKey] changes. */
@Composable
private fun ZoomableImage(
    model: Any,
    resetKey: Any,
    onTap: () -> Unit,
    modifier: Modifier = Modifier
) {
    var scale by remember(resetKey) { mutableStateOf(1f) }
    var offset by remember(resetKey) { mutableStateOf(Offset.Zero) }

    val transformState = rememberTransformableState { zoomChange, panChange, _ ->
        scale = (scale * zoomChange).coerceIn(1f, 6f)
        offset = if (scale <= 1f) Offset.Zero else offset + panChange
    }

    Box(modifier = modifier) {
        AsyncImage(
            model = model,
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = offset.x,
                    translationY = offset.y
                )
                .transformable(state = transformState)
                .pointerInput(resetKey) {
                    detectTapGestures(
                        onTap = { onTap() },
                        onDoubleTap = {
                            scale = 1f
                            offset = Offset.Zero
                        }
                    )
                }
        )
    }
}
