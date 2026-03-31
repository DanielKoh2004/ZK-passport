package com.example.zk.ui.screens

import android.Manifest
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import android.util.Size
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.accompanist.permissions.*
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.delay
import org.json.JSONObject
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

private const val TAG = "ChallengeScannerScreen"

private val DarkBackground = Color(0xFF0D1421)
private val AccentCyan = Color(0xFF00D9FF)
private val ErrorRed = Color(0xFFFF5252)

/**
 * Camera screen that scans the Officer's challenge QR code.
 * Expects JSON: {"type":"auth_challenge","request":"age_over_18","nonce":"<NONCE>"}
 * On success, calls onChallengeScanned with the extracted nonce.
 */
@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ChallengeScannerScreen(
    onChallengeScanned: (nonce: String) -> Unit = {},
    onBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        if (!cameraPermissionState.status.isGranted) {
            cameraPermissionState.launchPermissionRequest()
        }
    }

    // Auto-clear error after 3 seconds so user can retry
    LaunchedEffect(errorMessage) {
        if (errorMessage != null) {
            delay(3000)
            errorMessage = null
        }
    }

    Scaffold(
        containerColor = DarkBackground,
        topBar = {
            TopAppBar(
                title = {
                    Text("Scan Officer's Challenge", color = Color.White, fontWeight = FontWeight.Medium)
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                cameraPermissionState.status.isGranted -> {
                    var framesAnalysed by remember { mutableIntStateOf(0) }

                    ChallengeCameraPreview(
                        onQRCodeScanned = { rawValue ->
                            // Parse and validate the challenge JSON
                            try {
                                val json = JSONObject(rawValue)
                                val type = json.optString("type", "")
                                if (type != "auth_challenge") {
                                    errorMessage = "Invalid QR: not an auth_challenge (type=$type)"
                                    Log.w(TAG, "Wrong QR type: $type")
                                    return@ChallengeCameraPreview
                                }

                                val nonce = json.optString("nonce", "")
                                if (nonce.isBlank()) {
                                    errorMessage = "Invalid challenge: missing nonce"
                                    Log.w(TAG, "Challenge has no nonce")
                                    return@ChallengeCameraPreview
                                }

                                // Validate timestamp (reject challenges older than 60s)
                                val timestamp = json.optLong("timestamp", 0L)
                                if (timestamp > 0) {
                                    val ageMs = System.currentTimeMillis() - timestamp
                                    if (ageMs > 60_000) {
                                        errorMessage = "Challenge expired (${ageMs / 1000}s old). Ask Officer to regenerate."
                                        Log.w(TAG, "Challenge expired: ${ageMs}ms old")
                                        return@ChallengeCameraPreview
                                    }
                                }

                                // Haptic feedback + beep
                                val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                    val vm = context.getSystemService(android.content.Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                                    vm?.defaultVibrator
                                } else {
                                    @Suppress("DEPRECATION")
                                    context.getSystemService(android.content.Context.VIBRATOR_SERVICE) as? Vibrator
                                }
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                    vibrator?.vibrate(VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE))
                                } else {
                                    @Suppress("DEPRECATION")
                                    vibrator?.vibrate(200)
                                }
                                try {
                                    val toneGen = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 80)
                                    toneGen.startTone(ToneGenerator.TONE_PROP_ACK, 150)
                                } catch (_: Exception) {}

                                Log.d(TAG, "Challenge scanned successfully — nonce=$nonce")
                                onChallengeScanned(nonce)
                            } catch (e: Exception) {
                                errorMessage = "Not a valid challenge QR code"
                                Log.w(TAG, "Failed to parse QR: ${e.message}")
                            }
                        },
                        onFrameAnalysed = { framesAnalysed++ }
                    )

                    // Scanning overlay
                    ChallengeScanningOverlay(
                        framesAnalysed = framesAnalysed,
                        errorMessage = errorMessage
                    )
                }
                cameraPermissionState.status.shouldShowRationale -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Camera permission is required to scan the Officer's challenge QR code",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.White,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                        Button(
                            onClick = { cameraPermissionState.launchPermissionRequest() },
                            colors = ButtonDefaults.buttonColors(containerColor = AccentCyan)
                        ) {
                            Text("Grant Permission", color = DarkBackground)
                        }
                    }
                }
                else -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Camera permission is required. Please enable it in settings.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

// ── Scanning overlay ─────────────────────────────────────────────────────────

@Composable
private fun ChallengeScanningOverlay(
    framesAnalysed: Int,
    errorMessage: String?
) {
    val infiniteTransition = rememberInfiniteTransition(label = "scanLine")
    val scanLineProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scanLine"
    )

    Box(modifier = Modifier.fillMaxSize()) {
        // Viewfinder overlay
        Canvas(modifier = Modifier.fillMaxSize()) {
            val viewfinderSize = size.minDimension * 0.7f
            val left = (size.width - viewfinderSize) / 2f
            val top = (size.height - viewfinderSize) / 2f

            val overlayPath = Path().apply {
                addRect(androidx.compose.ui.geometry.Rect(0f, 0f, size.width, size.height))
                addRoundRect(RoundRect(left, top, left + viewfinderSize, top + viewfinderSize, CornerRadius(24f, 24f)))
            }
            drawPath(overlayPath, color = Color.Black.copy(alpha = 0.5f), blendMode = BlendMode.SrcOver)

            drawRoundRect(
                color = AccentCyan,
                topLeft = Offset(left, top),
                size = androidx.compose.ui.geometry.Size(viewfinderSize, viewfinderSize),
                cornerRadius = CornerRadius(24f, 24f),
                style = Stroke(width = 3.dp.toPx())
            )

            // Corner accents
            val cornerLen = viewfinderSize * 0.1f
            val strokeW = 5.dp.toPx()
            drawLine(AccentCyan, Offset(left, top + 12), Offset(left, top + cornerLen), strokeW, StrokeCap.Round)
            drawLine(AccentCyan, Offset(left + 12, top), Offset(left + cornerLen, top), strokeW, StrokeCap.Round)
            drawLine(AccentCyan, Offset(left + viewfinderSize, top + 12), Offset(left + viewfinderSize, top + cornerLen), strokeW, StrokeCap.Round)
            drawLine(AccentCyan, Offset(left + viewfinderSize - 12, top), Offset(left + viewfinderSize - cornerLen, top), strokeW, StrokeCap.Round)
            drawLine(AccentCyan, Offset(left, top + viewfinderSize - 12), Offset(left, top + viewfinderSize - cornerLen), strokeW, StrokeCap.Round)
            drawLine(AccentCyan, Offset(left + 12, top + viewfinderSize), Offset(left + cornerLen, top + viewfinderSize), strokeW, StrokeCap.Round)
            drawLine(AccentCyan, Offset(left + viewfinderSize, top + viewfinderSize - 12), Offset(left + viewfinderSize, top + viewfinderSize - cornerLen), strokeW, StrokeCap.Round)
            drawLine(AccentCyan, Offset(left + viewfinderSize - 12, top + viewfinderSize), Offset(left + viewfinderSize - cornerLen, top + viewfinderSize), strokeW, StrokeCap.Round)

            // Animated scan line
            val lineY = top + 16 + (viewfinderSize - 32) * scanLineProgress
            drawLine(
                color = AccentCyan.copy(alpha = 0.6f),
                start = Offset(left + 16, lineY),
                end = Offset(left + viewfinderSize - 16, lineY),
                strokeWidth = 2.dp.toPx(),
                cap = StrokeCap.Round
            )
        }

        // Bottom panel
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(DarkBackground.copy(alpha = 0.85f))
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Error message
            if (errorMessage != null) {
                Card(
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = ErrorRed.copy(alpha = 0.15f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = errorMessage,
                        color = ErrorRed,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            // Scanning indicator
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    color = AccentCyan,
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Scan the Officer's challenge QR …",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "$framesAnalysed frames analysed",
                color = Color.Gray,
                fontSize = 12.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Point your camera at the Officer's screen",
                color = AccentCyan.copy(alpha = 0.8f),
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Step indicator
            Text(
                text = "Step 1 of 3 — Scan Challenge",
                color = Color.Gray.copy(alpha = 0.5f),
                fontSize = 11.sp
            )
        }
    }
}

// ── Camera preview ───────────────────────────────────────────────────────────

@Composable
private fun ChallengeCameraPreview(
    onQRCodeScanned: (String) -> Unit,
    onFrameAnalysed: () -> Unit = {}
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }

    val hasScanned = remember { AtomicBoolean(false) }

    val scannerOptions = remember {
        BarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
            .build()
    }
    val barcodeScanner = remember { BarcodeScanning.getClient(scannerOptions) }

    DisposableEffect(Unit) {
        onDispose {
            barcodeScanner.close()
        }
    }

    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx)
            val executor = ContextCompat.getMainExecutor(ctx)

            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()

                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

                val imageAnalysis = ImageAnalysis.Builder()
                    .setTargetResolution(Size(1920, 1080))
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also {
                        it.setAnalyzer(Executors.newSingleThreadExecutor()) { imageProxy ->
                            processChallengeImage(imageProxy, barcodeScanner, hasScanned, onFrameAnalysed) { barcode ->
                                Log.d(TAG, "QR detected (${barcode.length} chars)")
                                onQRCodeScanned(barcode)
                            }
                        }
                    }

                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview,
                        imageAnalysis
                    )
                    Log.d(TAG, "Camera bound successfully")
                } catch (e: Exception) {
                    Log.e(TAG, "Use case binding failed", e)
                }
            }, executor)

            previewView
        },
        modifier = Modifier.fillMaxSize()
    )
}

@androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
private fun processChallengeImage(
    imageProxy: ImageProxy,
    scanner: com.google.mlkit.vision.barcode.BarcodeScanner,
    hasScanned: AtomicBoolean,
    onFrameAnalysed: () -> Unit,
    onQRCodeDetected: (String) -> Unit
) {
    if (hasScanned.get()) {
        imageProxy.close()
        return
    }

    val mediaImage = imageProxy.image
    if (mediaImage != null) {
        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

        scanner.process(image)
            .addOnSuccessListener { barcodes ->
                onFrameAnalysed()
                for (barcode in barcodes) {
                    barcode.rawValue?.let { value ->
                        if (hasScanned.compareAndSet(false, true)) {
                            onQRCodeDetected(value)
                        }
                        return@addOnSuccessListener
                    }
                }
            }
            .addOnFailureListener { e ->
                onFrameAnalysed()
                Log.e(TAG, "Barcode scanning failed", e)
            }
            .addOnCompleteListener {
                imageProxy.close()
            }
    } else {
        imageProxy.close()
    }
}
