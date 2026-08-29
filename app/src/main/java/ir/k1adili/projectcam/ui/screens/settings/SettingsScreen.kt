package ir.k1adili.projectcam.ui.screens.settings

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import ir.k1adili.projectcam.BuildConfig
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import ir.k1adili.projectcam.ProjectCamApp
import ir.k1adili.projectcam.R
import ir.k1adili.projectcam.data.ThemeMode
import ir.k1adili.projectcam.ui.components.ConfirmDialog
import ir.k1adili.projectcam.ui.theme.Spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val app = context.applicationContext as ProjectCamApp
    val viewModel: SettingsViewModel = viewModel(
        factory = viewModelFactory {
            initializer { SettingsViewModel(app.settingsRepository, app.database) }
        }
    )

    val photographerName by viewModel.photographerName.collectAsState()
    val themeMode by viewModel.themeMode.collectAsState()
    val isBusy by viewModel.isBusy.collectAsState()
    val event by viewModel.events.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Starts as null ("not loaded yet") rather than "" so we can tell the difference between
    // "DataStore hasn't emitted the real value yet" and "the user actually cleared the field".
    // Initializing eagerly from photographerName here would freeze the field at whatever value
    // existed on the very first composition (usually the "" default, before DataStore loads),
    // which is exactly why the saved name previously never showed up when reopening Settings.
    var nameField by rememberSaveable { mutableStateOf<String?>(null) }
    LaunchedEffect(photographerName) {
        if (nameField == null) nameField = photographerName
    }

    var showImportConfirm by remember { mutableStateOf(false) }
    var pendingImportUri by remember { mutableStateOf<android.net.Uri?>(null) }

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            pendingImportUri = uri
            showImportConfirm = true
        }
    }

    LaunchedEffect(nameField) {
        val current = nameField
        if (current != null && current != photographerName) {
            kotlinx.coroutines.delay(500)
            viewModel.setPhotographerName(current)
        }
    }

    LaunchedEffect(event) {
        when (val e = event) {
            is BackupUiEvent.ExportReady -> {
                val uri = FileProvider.getUriForFile(context, "${BuildConfig.APPLICATION_ID}.fileprovider", e.file)
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/zip"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(intent, null))
                viewModel.clearEvent()
            }
            is BackupUiEvent.ExportFailed -> {
                snackbarHostState.showSnackbar(e.message)
                viewModel.clearEvent()
            }
            is BackupUiEvent.ImportSucceeded -> {
                snackbarHostState.showSnackbar("بازیابی شد: ${e.projectCount} پروژه، ${e.photoCount} عکس")
                viewModel.clearEvent()
            }
            is BackupUiEvent.ImportFailed -> {
                snackbarHostState.showSnackbar(e.message)
                viewModel.clearEvent()
            }
            null -> Unit
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBackIosNew, contentDescription = stringResource(R.string.back))
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(padding)
                .padding(Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.lg)
        ) {
            OutlinedTextField(
                value = nameField.orEmpty(),
                onValueChange = { nameField = it },
                label = { Text(stringResource(R.string.photographer_name_label)) },
                supportingText = { Text(stringResource(R.string.photographer_name_hint)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Divider()

            Text(stringResource(R.string.theme_section_title), style = MaterialTheme.typography.titleSmall)
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                val options = listOf(
                    ThemeMode.SYSTEM to stringResource(R.string.theme_system),
                    ThemeMode.LIGHT to stringResource(R.string.theme_light),
                    ThemeMode.DARK to stringResource(R.string.theme_dark)
                )
                options.forEachIndexed { index, (mode, label) ->
                    SegmentedButton(
                        selected = themeMode == mode,
                        onClick = { viewModel.setThemeMode(mode) },
                        shape = androidx.compose.material3.SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = options.size
                        )
                    ) {
                        Text(label)
                    }
                }
            }

            Divider()

            Text(stringResource(R.string.backup_section_title), style = MaterialTheme.typography.titleSmall)

            ListItem(
                headlineContent = { Text(stringResource(R.string.export_backup)) },
                supportingContent = { Text(stringResource(R.string.export_backup_desc)) },
                leadingContent = { Icon(Icons.Filled.CloudUpload, contentDescription = null) },
                modifier = Modifier.clickableIfNotBusy(isBusy) { viewModel.exportBackup(context) }
            )

            ListItem(
                headlineContent = { Text(stringResource(R.string.import_backup)) },
                supportingContent = { Text(stringResource(R.string.import_backup_desc)) },
                leadingContent = { Icon(Icons.Filled.CloudDownload, contentDescription = null) },
                modifier = Modifier.clickableIfNotBusy(isBusy) { importLauncher.launch("application/zip") }
            )

            if (isBusy) {
                CircularProgressIndicator(modifier = Modifier.padding(top = Spacing.sm))
            }

            Divider()

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Spacing.xs)
            ) {
                Text(
                    stringResource(R.string.about_section_title),
                    style = MaterialTheme.typography.titleSmall,
                    textAlign = TextAlign.Center
                )
                Text(
                    stringResource(R.string.about_developer_format, "Keyvan Adili"),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Text(
                    stringResource(R.string.version_format, BuildConfig.VERSION_NAME),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }

    if (showImportConfirm) {
        ConfirmDialog(
            title = stringResource(R.string.import_backup_confirm_title),
            message = stringResource(R.string.import_backup_confirm_message),
            onConfirm = {
                showImportConfirm = false
                pendingImportUri?.let { viewModel.importBackup(context, it) }
                pendingImportUri = null
            },
            onDismiss = {
                showImportConfirm = false
                pendingImportUri = null
            }
        )
    }
}

private fun Modifier.clickableIfNotBusy(isBusy: Boolean, onClick: () -> Unit): Modifier =
    if (isBusy) this else this.clickable(onClick = onClick)
