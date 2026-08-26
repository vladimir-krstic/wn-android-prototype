package dev.ipf.whitenoise.ui.onboarding

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.view.View
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.mlkit.vision.MlKitAnalyzer
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import dev.ipf.whitenoise.R
import dev.ipf.whitenoise.ui.theme.WhiteNoiseSpacing
import java.util.concurrent.atomic.AtomicBoolean

private const val NearFullScannerSheetHeightFraction = 0.94f

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PrivateKeyQrScannerSheet(
    onDismiss: () -> Unit,
    onCodeScanned: (String) -> Unit,
    onUnavailable: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val lifecycleOwner = LocalLifecycleOwner.current
    val hasCameraHardware = remember(context) {
        context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)
    }
    var hasCameraPermission by remember {
        mutableStateOf(context.hasCameraPermission())
    }
    var permissionRequestFinished by rememberSaveable {
        mutableStateOf(hasCameraPermission)
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        hasCameraPermission = granted
        permissionRequestFinished = true
    }

    DisposableEffect(context, lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasCameraPermission = context.hasCameraPermission()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(hasCameraPermission, permissionRequestFinished) {
        if (!hasCameraHardware) {
            onUnavailable()
        } else if (!hasCameraPermission && !permissionRequestFinished) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val density = LocalDensity.current
    val windowHeightPx = LocalWindowInfo.current.containerSize.height
    val sheetContentHeight = with(density) {
        (windowHeightPx.toDp() * NearFullScannerSheetHeightFraction).coerceAtLeast(0.dp)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = modifier,
        containerColor = Color.Black,
        contentColor = Color.White,
        contentWindowInsets = { WindowInsets(0, 0, 0, 0) },
        dragHandle = null,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(sheetContentHeight),
        ) {
            if (hasCameraPermission) {
                CameraScannerContent(
                    onDismiss = onDismiss,
                    onCodeScanned = onCodeScanned,
                    onUnavailable = onUnavailable,
                )
            } else {
                PermissionScannerContent(
                    onDismiss = onDismiss,
                    isRequestPending = !permissionRequestFinished,
                    openSettings = permissionRequestFinished &&
                        activity?.let {
                            !ActivityCompat.shouldShowRequestPermissionRationale(
                                it,
                                Manifest.permission.CAMERA,
                            )
                        } == true,
                    onRequestPermission = {
                        permissionRequestFinished = false
                    },
                    onOpenSettings = {
                        context.startActivity(
                            Intent(
                                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                Uri.fromParts("package", context.packageName, null),
                            ),
                        )
                    },
                )
            }
        }
    }
}

@Composable
private fun CameraScannerContent(
    onDismiss: () -> Unit,
    onCodeScanned: (String) -> Unit,
    onUnavailable: () -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val mainExecutor = remember(context) { ContextCompat.getMainExecutor(context) }
    val currentOnCodeScanned = rememberUpdatedState(onCodeScanned)
    val currentOnUnavailable = rememberUpdatedState(onUnavailable)
    val deliveredResult = remember { AtomicBoolean(false) }
    val barcodeScanner = remember {
        BarcodeScanning.getClient(
            BarcodeScannerOptions.Builder()
                .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                .build(),
        )
    }
    val cameraController = remember(context) {
        LifecycleCameraController(context).apply {
            cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            setEnabledUseCases(CameraController.IMAGE_ANALYSIS)
            imageAnalysisBackpressureStrategy = ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST
        }
    }
    val analyzer = remember(barcodeScanner, mainExecutor) {
        MlKitAnalyzer(
            listOf(barcodeScanner),
            ImageAnalysis.COORDINATE_SYSTEM_ORIGINAL,
            mainExecutor,
        ) { result ->
            val payload = result
                ?.getValue(barcodeScanner)
                ?.firstOrNull()
                ?.rawValue
            if (!payload.isNullOrBlank() && deliveredResult.compareAndSet(false, true)) {
                currentOnCodeScanned.value(payload)
            }
        }
    }
    var hasFlashUnit by remember { mutableStateOf(false) }
    var torchEnabled by remember { mutableStateOf(false) }

    DisposableEffect(cameraController, lifecycleOwner, analyzer) {
        runCatching {
            cameraController.setImageAnalysisAnalyzer(mainExecutor, analyzer)
            cameraController.bindToLifecycle(lifecycleOwner)
            hasFlashUnit = cameraController.cameraInfo?.hasFlashUnit() == true
        }.onFailure {
            currentOnUnavailable.value()
        }

        onDispose {
            cameraController.clearImageAnalysisAnalyzer()
            cameraController.unbind()
            barcodeScanner.close()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { viewContext ->
                PreviewView(viewContext).apply {
                    implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                    controller = cameraController
                    importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
                }
            },
            update = { it.controller = cameraController },
            modifier = Modifier.fillMaxSize(),
        )

        BoxWithConstraints(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            val targetSize = minOf(
                maxWidth - 64.dp,
                maxHeight - 192.dp,
                280.dp,
            ).coerceAtLeast(160.dp)
            RoundedQrTarget(modifier = Modifier.size(targetSize))
        }

        ScannerHeader(
            onDismiss = onDismiss,
            hasFlashUnit = hasFlashUnit,
            torchEnabled = torchEnabled,
            onToggleTorch = {
                val requestedState = !torchEnabled
                val future = cameraController.enableTorch(requestedState)
                future.addListener(
                    {
                        runCatching { future.get() }
                            .onSuccess { torchEnabled = requestedState }
                    },
                    mainExecutor,
                )
            },
            modifier = Modifier.align(Alignment.TopCenter),
        )
    }
}

@Composable
private fun PermissionScannerContent(
    onDismiss: () -> Unit,
    isRequestPending: Boolean,
    openSettings: Boolean,
    onRequestPermission: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF202020)),
        contentAlignment = Alignment.Center,
    ) {
        if (isRequestPending) {
            CircularProgressIndicator(color = Color.White)
        } else {
            Surface(
                modifier = Modifier
                    .padding(WhiteNoiseSpacing.Section)
                    .widthIn(max = 520.dp)
                    .fillMaxWidth(),
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
                contentColor = MaterialTheme.colorScheme.onSurface,
            ) {
                Column(
                    modifier = Modifier.padding(WhiteNoiseSpacing.Section),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(WhiteNoiseSpacing.FormField),
                ) {
                    Text(
                        text = stringResource(R.string.camera_access_needed),
                        style = MaterialTheme.typography.titleLarge,
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        text = stringResource(R.string.camera_permission_detail),
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                    )
                    Button(
                        onClick = if (openSettings) onOpenSettings else onRequestPermission,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                        ),
                    ) {
                        Text(
                            stringResource(
                                if (openSettings) R.string.open_settings else R.string.allow_camera,
                            ),
                        )
                    }
                }
            }
        }

        ScannerHeader(
            onDismiss = onDismiss,
            modifier = Modifier.align(Alignment.TopCenter),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ScannerHeader(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    hasFlashUnit: Boolean = false,
    torchEnabled: Boolean = false,
    onToggleTorch: () -> Unit = {},
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(112.dp)
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color.Black.copy(alpha = 0.68f),
                        Color.Black.copy(alpha = 0.32f),
                        Color.Transparent,
                    ),
                ),
            ),
    ) {
        BottomSheetDefaults.DragHandle(
            modifier = Modifier.align(Alignment.TopCenter),
            color = Color.White.copy(alpha = 0.72f),
        )

        CenterAlignedTopAppBar(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 16.dp),
            title = {
                Text(
                    text = stringResource(R.string.scan_qr_code),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            },
            navigationIcon = {
                IconButton(onClick = onDismiss) {
                    Icon(
                        painter = painterResource(R.drawable.ic_close),
                        contentDescription = stringResource(R.string.close),
                    )
                }
            },
            actions = {
                if (hasFlashUnit) {
                    IconToggleButton(
                        checked = torchEnabled,
                        onCheckedChange = { onToggleTorch() },
                        colors = IconButtonDefaults.iconToggleButtonColors(
                            contentColor = Color.White.copy(alpha = 0.72f),
                            checkedContentColor = Color.White,
                        ),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_flash_on),
                            contentDescription = stringResource(
                                if (torchEnabled) {
                                    R.string.turn_flashlight_off
                                } else {
                                    R.string.turn_flashlight_on
                                },
                            ),
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.size(48.dp))
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent,
                titleContentColor = Color.White,
                navigationIconContentColor = Color.White,
                actionIconContentColor = Color.White,
            ),
            windowInsets = WindowInsets(0, 0, 0, 0),
        )
    }
}

@Composable
private fun RoundedQrTarget(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val inset = 4.dp.toPx()
        val cornerLength = 58.dp.toPx().coerceAtMost(size.minDimension * 0.28f)
        val cornerRadius = 24.dp.toPx().coerceAtMost(cornerLength)
        val right = size.width - inset
        val bottom = size.height - inset
        val paths = listOf(
            Path().apply {
                moveTo(inset + cornerLength, inset)
                lineTo(inset + cornerRadius, inset)
                arcTo(
                    rect = Rect(inset, inset, inset + cornerRadius * 2f, inset + cornerRadius * 2f),
                    startAngleDegrees = -90f,
                    sweepAngleDegrees = -90f,
                    forceMoveTo = false,
                )
                lineTo(inset, inset + cornerLength)
            },
            Path().apply {
                moveTo(right - cornerLength, inset)
                lineTo(right - cornerRadius, inset)
                arcTo(
                    rect = Rect(
                        right - cornerRadius * 2f,
                        inset,
                        right,
                        inset + cornerRadius * 2f,
                    ),
                    startAngleDegrees = -90f,
                    sweepAngleDegrees = 90f,
                    forceMoveTo = false,
                )
                lineTo(right, inset + cornerLength)
            },
            Path().apply {
                moveTo(inset, bottom - cornerLength)
                lineTo(inset, bottom - cornerRadius)
                arcTo(
                    rect = Rect(
                        inset,
                        bottom - cornerRadius * 2f,
                        inset + cornerRadius * 2f,
                        bottom,
                    ),
                    startAngleDegrees = 180f,
                    sweepAngleDegrees = -90f,
                    forceMoveTo = false,
                )
                lineTo(inset + cornerLength, bottom)
            },
            Path().apply {
                moveTo(right, bottom - cornerLength)
                lineTo(right, bottom - cornerRadius)
                arcTo(
                    rect = Rect(
                        right - cornerRadius * 2f,
                        bottom - cornerRadius * 2f,
                        right,
                        bottom,
                    ),
                    startAngleDegrees = 0f,
                    sweepAngleDegrees = 90f,
                    forceMoveTo = false,
                )
                lineTo(right - cornerLength, bottom)
            },
        )
        val targetStroke = Stroke(
            width = 3.dp.toPx(),
            cap = StrokeCap.Round,
            join = StrokeJoin.Round,
        )

        paths.forEach { path ->
            drawPath(path, color = Color.White, style = targetStroke)
        }
    }
}

private fun Context.hasCameraPermission(): Boolean =
    ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
