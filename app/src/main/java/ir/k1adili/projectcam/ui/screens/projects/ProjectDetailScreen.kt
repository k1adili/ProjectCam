package ir.k1adili.projectcam.ui.screens.projects

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import coil.compose.AsyncImage
import ir.k1adili.projectcam.BuildConfig
import ir.k1adili.projectcam.ProjectCamApp
import ir.k1adili.projectcam.R
import ir.k1adili.projectcam.data.local.PhotoEntity
import ir.k1adili.projectcam.ui.components.ConfirmDialog
import ir.k1adili.projectcam.ui.components.EmptyState
import ir.k1adili.projectcam.ui.theme.Spacing
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectDetailScreen(
    projectId: Long,
    onBack: () -> Unit,
    onOpenCamera: (Long) -> Unit,
    onOpenPhoto: (Long) -> Unit
) {
    val context = LocalContext.current
    val app = context.applicationContext as ProjectCamApp
    val viewModel: ProjectDetailViewModel = viewModel(
        key = "project_detail_$projectId",
        factory = viewModelFactory {
            initializer { ProjectDetailViewModel(projectId, app.projectRepository, app.photoRepository) }
        }
    )

    val project by viewModel.project.collectAsState()
    val photos by viewModel.photos.collectAsState()
    val selectedIds by viewModel.selectedIds.collectAsState()
    val exportedFile by viewModel.exportedFile.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    var showExportMenu by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var isExporting by remember { mutableStateOf(false) }

    val selectionMode = selectedIds.isNotEmpty()

    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            isExporting = false
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    LaunchedEffect(exportedFile) {
        exportedFile?.let { result ->
            isExporting = false
            val uri = FileProvider.getUriForFile(
                context,
                "${BuildConfig.APPLICATION_ID}.fileprovider",
                result.outputFile
            )
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/vnd.google-earth.kmz"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, null))
            viewModel.clearExportedFile()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (selectionMode) {
                        Text(stringResource(R.string.selected_count_format, selectedIds.size))
                    } else {
                        Text(project?.name.orEmpty())
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { if (selectionMode) viewModel.clearSelection() else onBack() }) {
                        Icon(
                            if (selectionMode) Icons.Filled.Close else Icons.Filled.ArrowBackIosNew,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                },
                actions = {
                    if (selectionMode) {
                        IconButton(onClick = { viewModel.selectAll() }) {
                            Icon(Icons.Filled.SelectAll, contentDescription = stringResource(R.string.select_all))
                        }
                        IconButton(onClick = { showDeleteConfirm = true }) {
                            Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.delete_selected))
                        }
                        IconButton(onClick = { showExportMenu = true }) {
                            Icon(Icons.Filled.IosShare, contentDescription = stringResource(R.string.export_kml))
                        }
                    } else if (photos.isNotEmpty()) {
                        IconButton(onClick = { showExportMenu = true }) {
                            Icon(Icons.Filled.IosShare, contentDescription = stringResource(R.string.export_kml))
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            if (!selectionMode) {
                FloatingActionButton(onClick = { onOpenCamera(projectId) }) {
                    Icon(Icons.Filled.PhotoCamera, contentDescription = stringResource(R.string.open_camera_cd))
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            if (photos.isEmpty()) {
                EmptyState(
                    title = stringResource(R.string.project_detail_empty_title),
                    subtitle = stringResource(R.string.project_detail_empty_subtitle)
                )
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(Spacing.sm),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                    verticalArrangement = Arrangement.spacedBy(Spacing.xs)
                ) {
                    items(photos, key = { it.id }) { photo ->
                        PhotoThumbnail(
                            file = viewModel.absoluteFile(photo),
                            isSelected = photo.id in selectedIds,
                            selectionMode = selectionMode,
                            onClick = {
                                if (selectionMode) viewModel.toggleSelection(photo.id) else onOpenPhoto(photo.id)
                            },
                            onLongClick = { viewModel.toggleSelection(photo.id) }
                        )
                    }
                }
            }

            if (isExporting) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        }
    }

    if (showExportMenu) {
        AlertDialog(
            onDismissRequest = { showExportMenu = false },
            title = { Text(stringResource(R.string.export_kml)) },
            text = { },
            confirmButton = {
                TextButton(onClick = {
                    showExportMenu = false
                    isExporting = true
                    val toExport = if (selectionMode) photos.filter { it.id in selectedIds } else null
                    viewModel.exportKml(context, toExport)
                }) {
                    Text(if (selectionMode) stringResource(R.string.export_selected) else stringResource(R.string.export_all))
                }
            },
            dismissButton = {
                if (selectionMode) {
                    TextButton(onClick = {
                        showExportMenu = false
                        isExporting = true
                        viewModel.exportKml(context, null)
                    }) {
                        Text(stringResource(R.string.export_all))
                    }
                } else {
                    TextButton(onClick = { showExportMenu = false }) {
                        Text(stringResource(R.string.cancel))
                    }
                }
            }
        )
    }

    if (showDeleteConfirm) {
        ConfirmDialog(
            title = stringResource(R.string.delete_photos_confirm_title),
            message = stringResource(R.string.delete_photos_confirm_message),
            onConfirm = {
                viewModel.deleteSelected()
                showDeleteConfirm = false
            },
            onDismiss = { showDeleteConfirm = false }
        )
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun PhotoThumbnail(
    file: File,
    isSelected: Boolean,
    selectionMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(MaterialTheme.shapes.small)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
    ) {
        AsyncImage(
            model = file,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        if (selectionMode) {
            Icon(
                imageVector = if (isSelected) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
                contentDescription = null,
                tint = if (isSelected) MaterialTheme.colorScheme.primary else Color.White,
                modifier = Modifier
                    .padding(Spacing.xs)
                    .size(22.dp)
            )
        }
    }
}
