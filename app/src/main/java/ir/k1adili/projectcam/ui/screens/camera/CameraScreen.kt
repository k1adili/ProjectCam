package ir.k1adili.projectcam.ui.screens.camera

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.GpsOff
import androidx.compose.material.icons.filled.LocationSearching
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import ir.k1adili.projectcam.ProjectCamApp
import ir.k1adili.projectcam.R
import ir.k1adili.projectcam.util.ImageUtils
import ir.k1adili.projectcam.util.JalaliDateUtils.toPersianDigits
import ir.k1adili.projectcam.util.PhotoFileUtils
import ir.k1adili.projectcam.util.WatermarkInfo
import ir.k1adili.projectcam.util.WatermarkUtil
import kotlinx.coroutines.launch
import java.io.File
import java.time.LocalDateTime
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import kotlin.math.roundToInt
import kotlin.coroutines.resume

private val REQUIRED_PERMISSIONS = arrayOf(
    Manifest.permission.CAMERA,
    Manifest.permission.ACCESS_FINE_LOCATION
)

@Composable
fun CameraScreen(
    projectId: Long,
    onBack: () -> Unit,
    onPhotoSaved: () -> Unit
) {
    val context = LocalContext.current
    val app = context.applicationContext as ProjectCamApp
    val viewModel: CameraViewModel = viewModel(
        key = "camera_$projectId",
        factory = viewModelFactory {
            initializer {
                CameraViewModel(projectId, app.projectRepository, app.photoRepository, app.settingsRepository)
            }
        }
    )

    var permissionsGranted by remember {
        mutableStateOf(REQUIRED_PERMISSIONS.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        })
    }

    val permissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        permissionsGranted = results.values.all { it }
    }

    LaunchedEffect(Unit) {
        if (!permissionsGranted) permissionLauncher.launch(REQUIRED_PERMISSIONS)
    }

    if (!permissionsGranted) {
        PermissionRationale(
            onBack = onBack,
            onGrant = { permissionLauncher.launch(REQUIRED_PERMISSIONS) }
        )
        return
    }

    LaunchedEffect(Unit) {
        viewModel.refreshLocation(context)
    }

    CameraContent(projectId = projectId, viewModel = viewModel, onBack = onBack, onPhotoSaved = onPhotoSaved)
}

@Composable
private fun PermissionRationale(onBack: () -> Unit, onGrant: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(24.dp)
        ) {
            Text(
                text = stringResource(R.string.camera_permission_needed),
                style = MaterialTheme.typography.bodyLarge
            )
            androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(top = 16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TextButton(onClick = onBack) { Text(stringResource(R.string.cancel)) }
                TextButton(onClick = onGrant) { Text(stringResource(R.string.grant_permission)) }
            }
        }
    }
}

@Composable
private fun CameraContent(
    projectId: Long,
    viewModel: CameraViewModel,
    onBack: () -> Unit,
    onPhotoSaved: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()

    val project by viewModel.project.collectAsState()
    val photographerName by viewModel.photographerName.collectAsState()
    val locationState by viewModel.locationState.collectAsState()
    val isSaving by viewModel.isSaving.collectAsState()

    val previewView = remember { PreviewView(context) }
    val imageCapture = remember { ImageCapture.Builder().setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY).build() }
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    DisposableEffect(Unit) {
        onDispose { cameraExecutor.shutdown() }
    }

    LaunchedEffect(previewView) {
        val cameraProvider = getCameraProvider(context)
        val preview = Preview.Builder().build().also {
            it.setSurfaceProvider(previewView.surfaceProvider)
        }
        runCatching {
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                lifecycleOwner,
                CameraSelector.DEFAULT_BACK_CAMERA,
                preview,
                imageCapture
            )
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        androidx.compose.ui.viewinterop.AndroidView(
            factory = { previewView },
            modifier = Modifier.fillMaxSize()
        )

        // Top bar: back + location status
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .systemBarsPadding()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.4f))
            ) {
                Icon(Icons.Filled.ArrowBackIosNew, contentDescription = stringResource(R.string.back), tint = Color.White)
            }

            LocationBadge(
                state = locationState,
                onRetry = { viewModel.refreshLocation(context) }
            )
        }

        // Bottom: project name + capture button
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .systemBarsPadding()
                .padding(bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            project?.let {
                Surface(
                    color = Color.Black.copy(alpha = 0.45f),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Text(
                        text = it.name,
                        color = Color.White,
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
                androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(top = 16.dp))
            }

            if (isSaving) {
                CircularProgressIndicator(color = Color.White)
            } else {
                CaptureButton(onClick = {
                    val currentProject = project ?: return@CaptureButton
                    val fileName = PhotoFileUtils.newPhotoFileName()
                    val tempFile = File(context.cacheDir, "capture_${System.currentTimeMillis()}.jpg")

                    val outputOptions = ImageCapture.OutputFileOptions.Builder(tempFile).build()
                    imageCapture.takePicture(
                        outputOptions,
                        cameraExecutor,
                        object : ImageCapture.OnImageSavedCallback {
                            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                                coroutineScope.launch {
                                    try {
                                        val upright = ImageUtils.decodeUprightBitmap(tempFile)
                                        val loc = (locationState as? LocationUiState.Available)?.location
                                        val watermarked = WatermarkUtil.applyWatermark(
                                            source = upright,
                                            info = WatermarkInfo(
                                                projectName = currentProject.name,
                                                photographerName = photographerName,
                                                latitude = loc?.latitude,
                                                longitude = loc?.longitude,
                                                accuracyMeters = loc?.accuracyMeters,
                                                capturedAt = LocalDateTime.now()
                                            ),
                                            titleTypeface = ResourcesCompat.getFont(context, R.font.vazirmatn_semibold),
                                            bodyTypeface = ResourcesCompat.getFont(context, R.font.vazirmatn_regular)
                                        )
                                        val finalFile = PhotoFileUtils.absoluteFile(context, fileName)
                                        ImageUtils.saveJpeg(watermarked, finalFile)
                                        tempFile.delete()

                                        viewModel.savePhoto(
                                            fileName = fileName,
                                            photographerName = photographerName,
                                            onSaved = onPhotoSaved
                                        )
                                    } catch (t: Throwable) {
                                        Toast.makeText(context, context.getString(R.string.error_generic), Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }

                            override fun onError(exception: ImageCaptureException) {
                                Toast.makeText(context, context.getString(R.string.error_generic), Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                })
            }
        }
    }
}

@Composable
private fun LocationBadge(state: LocationUiState, onRetry: () -> Unit) {
    Surface(
        color = Color.Black.copy(alpha = 0.45f),
        shape = MaterialTheme.shapes.extraLarge
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            when (state) {
                is LocationUiState.Loading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                    Text(
                        text = stringResource(R.string.location_acquiring),
                        color = Color.White,
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
                is LocationUiState.Available -> {
                    Icon(Icons.Filled.GpsFixed, contentDescription = null, tint = Color(0xFF4CD964), modifier = Modifier.size(16.dp))
                    val accuracy = state.location.accuracyMeters
                    val accuracyText = if (!accuracy.isNaN()) toPersianDigits(accuracy.roundToInt().toString()) else "?"
                    Text(
                        text = stringResource(R.string.location_acquired_format, accuracyText),
                        color = Color.White,
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
                is LocationUiState.Unavailable -> {
                    Icon(Icons.Filled.GpsOff, contentDescription = null, tint = Color(0xFFFF6B6B), modifier = Modifier.size(16.dp))
                    Text(
                        text = stringResource(R.string.location_unavailable),
                        color = Color.White,
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(start = 8.dp, end = 4.dp)
                    )
                    IconButton(onClick = onRetry, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Filled.Refresh, contentDescription = stringResource(R.string.retry_location), tint = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
private fun CaptureButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(76.dp)
            .clip(CircleShape)
            .background(Color.White)
            .padding(4.dp)
            .clip(CircleShape)
            .background(Color.Transparent),
        contentAlignment = Alignment.Center
    ) {
        IconButton(
            onClick = onClick,
            modifier = Modifier
                .size(68.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary)
        ) {
            Icon(
                Icons.Filled.PhotoCamera,
                contentDescription = stringResource(R.string.capture_photo),
                tint = Color.White,
                modifier = Modifier.size(30.dp)
            )
        }
    }
}

private suspend fun getCameraProvider(context: android.content.Context): ProcessCameraProvider =
    kotlinx.coroutines.suspendCancellableCoroutine { continuation ->
        val future = ProcessCameraProvider.getInstance(context)
        future.addListener(
            { continuation.resume(future.get()) },
            ContextCompat.getMainExecutor(context)
        )
    }
