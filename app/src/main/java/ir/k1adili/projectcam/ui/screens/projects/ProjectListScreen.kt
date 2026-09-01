package ir.k1adili.projectcam.ui.screens.projects

import androidx.compose.foundation.background
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Engineering
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import coil.compose.AsyncImage
import ir.k1adili.projectcam.ProjectCamApp
import ir.k1adili.projectcam.R
import ir.k1adili.projectcam.data.local.ProjectWithPhotoCount
import ir.k1adili.projectcam.ui.components.ConfirmDialog
import ir.k1adili.projectcam.ui.components.EmptyState
import ir.k1adili.projectcam.ui.theme.Spacing
import ir.k1adili.projectcam.ui.theme.projectCamTopAppBarColors
import ir.k1adili.projectcam.util.JalaliDateUtils
import ir.k1adili.projectcam.util.PhotoFileUtils
import java.time.Instant
import java.time.ZoneId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectListScreen(
    onOpenProject: (Long) -> Unit,
    onOpenSettings: () -> Unit
) {
    val context = LocalContext.current
    val app = context.applicationContext as ProjectCamApp
    val viewModel: ProjectListViewModel = viewModel(
        factory = viewModelFactory {
            initializer { ProjectListViewModel(app.projectRepository, app.photoRepository) }
        }
    )

    val projects by viewModel.projects.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var showCreateDialog by rememberSaveable { mutableStateOf(false) }
    var projectPendingEdit by remember { mutableStateOf<ProjectWithPhotoCount?>(null) }
    var projectPendingDelete by remember { mutableStateOf<ProjectWithPhotoCount?>(null) }

    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.projects_title)) },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = stringResource(R.string.settings_title))
                    }
                },
                colors = projectCamTopAppBarColors()
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                text = { Text(stringResource(R.string.new_project)) },
                icon = { Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.add_project_cd)) },
                onClick = { showCreateDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        if (projects.isEmpty()) {
            EmptyState(
                title = stringResource(R.string.projects_empty_title),
                subtitle = stringResource(R.string.projects_empty_subtitle),
                icon = Icons.Filled.Engineering,
                modifier = Modifier.padding(padding)
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(Spacing.lg),
                verticalArrangement = Arrangement.spacedBy(Spacing.md)
            ) {
                item {
                    ProjectsSummaryHeader(
                        projectCount = projects.size,
                        photoCount = projects.sumOf { it.photoCount }
                    )
                }
                items(projects, key = { it.id }) { project ->
                    ProjectRow(
                        project = project,
                        onClick = { onOpenProject(project.id) },
                        onEditRequest = { projectPendingEdit = project },
                        onDeleteRequest = { projectPendingDelete = project }
                    )
                }
            }
        }
    }

    if (showCreateDialog) {
        ProjectDialog(
            title = stringResource(R.string.new_project),
            initialName = "",
            initialNote = "",
            confirmLabel = stringResource(R.string.create),
            onDismiss = { showCreateDialog = false },
            onConfirm = { name, note ->
                viewModel.createProject(name, note) { newId ->
                    showCreateDialog = false
                    onOpenProject(newId)
                }
            }
        )
    }

    projectPendingEdit?.let { project ->
        ProjectDialog(
            title = stringResource(R.string.edit_project_title),
            initialName = project.name,
            initialNote = project.note,
            confirmLabel = stringResource(R.string.save),
            onDismiss = { projectPendingEdit = null },
            onConfirm = { name, note ->
                viewModel.renameProject(project, name, note)
                projectPendingEdit = null
            }
        )
    }

    projectPendingDelete?.let { project ->
        ConfirmDialog(
            title = stringResource(R.string.delete_project_confirm_title),
            message = stringResource(R.string.delete_project_confirm_message),
            onConfirm = {
                viewModel.deleteProject(project.id)
                projectPendingDelete = null
            },
            onDismiss = { projectPendingDelete = null }
        )
    }
}

/** A small stat strip so the list doesn't open on a bare, lifeless list of rows. */
@Composable
private fun ProjectsSummaryHeader(projectCount: Int, photoCount: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primary
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.lg, vertical = Spacing.md),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Filled.Engineering,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(28.dp)
            )
            Text(
                text = stringResource(R.string.projects_summary_format, projectCount, photoCount),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onPrimary
            )
        }
    }
}

@Composable
private fun ProjectRow(
    project: ProjectWithPhotoCount,
    onClick: () -> Unit,
    onEditRequest: () -> Unit,
    onDeleteRequest: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primary
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            ProjectThumbnail(fileName = project.latestPhotoFileName)

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = project.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Text(
                    text = stringResource(R.string.photo_count_format, project.photoCount),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.75f)
                )
                val createdDate = JalaliDateUtils.formatDateLong(
                    Instant.ofEpochMilli(project.createdAtEpochMillis).atZone(ZoneId.systemDefault()).toLocalDate()
                )
                Text(
                    text = createdDate,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.75f)
                )
            }

            Box {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(
                        Icons.Filled.MoreVert,
                        contentDescription = stringResource(R.string.more_options),
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
                DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.edit)) },
                        leadingIcon = { Icon(Icons.Filled.Edit, contentDescription = null) },
                        onClick = {
                            menuExpanded = false
                            onEditRequest()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.delete)) },
                        leadingIcon = { Icon(Icons.Filled.Delete, contentDescription = null) },
                        onClick = {
                            menuExpanded = false
                            onDeleteRequest()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ProjectThumbnail(fileName: String?) {
    val context = LocalContext.current
    val shape = RoundedCornerShape(12.dp)
    Box(
        modifier = Modifier
            .size(56.dp)
            .clip(shape)
            .background(MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.15f)),
        contentAlignment = Alignment.Center
    ) {
        if (fileName != null) {
            AsyncImage(
                model = PhotoFileUtils.absoluteFile(context, fileName),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Icon(
                Icons.Filled.Engineering,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProjectDialog(
    title: String,
    initialName: String,
    initialNote: String,
    confirmLabel: String,
    onDismiss: () -> Unit,
    onConfirm: (name: String, note: String) -> Unit
) {
    var name by rememberSaveable(initialName) { mutableStateOf(initialName) }
    var note by rememberSaveable(initialNote) { mutableStateOf(initialNote) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.project_name_label)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text(stringResource(R.string.project_note_label)) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(name, note) },
                enabled = name.isNotBlank()
            ) {
                Text(confirmLabel)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}
