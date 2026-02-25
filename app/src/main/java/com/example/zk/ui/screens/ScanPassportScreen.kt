package com.example.zk.ui.screens

import android.Manifest
import android.graphics.Bitmap
import android.util.Log
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.zk.data.PassportData
import com.example.zk.data.PassportReadingState
import com.example.zk.passport.MrzScanner
import com.example.zk.ui.theme.ZKTheme
import com.example.zk.viewmodel.PassportScanStep
import com.example.zk.viewmodel.PassportUiState
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.util.concurrent.Executors

private val DarkBackground = Color(0xFF0D1421)
private val CardBackground = Color(0xFF1A2332)
private val AccentCyan = Color(0xFF00D9FF)
private val AccentGreen = Color(0xFF4CAF50)

/**
 * ScanPassportScreen - Comprehensive passport scanning
 *
 * Flow:
 * 1. Scan MRZ with camera (or enter manually)
 * 2. Confirm MRZ data
 * 3. Tap passport on phone for NFC reading
 * 4. Display extracted data
 *
 * PRIVACY: All processing is LOCAL. No data is uploaded.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun ScanPassportScreen(
    onBack: () -> Unit = {},
    onCapture: () -> Unit = {},
    onGallery: () -> Unit = {},
    onFlash: () -> Unit = {},
    // ViewModel state
    uiState: PassportUiState = PassportUiState(),
    readingState: PassportReadingState = PassportReadingState.Idle,
    passportData: PassportData? = null,
    documentNumber: String = "",
    dateOfBirth: String = "",
    expiryDate: String = "",
    onDocumentNumberChange: (String) -> Unit = {},
    onDateOfBirthChange: (String) -> Unit = {},
    onExpiryDateChange: (String) -> Unit = {},
    onConfirmMrz: () -> Unit = {},
    onEnterManually: () -> Unit = {},
    onBackToScan: () -> Unit = {},
    onRetryNfc: () -> Unit = {},
    onDevBypassNfc: () -> Unit = {},
    onComplete: () -> Unit = {}
) {
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)

    Scaffold(
        containerColor = DarkBackground,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = when (uiState.step) {
                            PassportScanStep.SCAN_MRZ -> "Scan Passport"
                            PassportScanStep.MANUAL_MRZ -> "Enter MRZ Data"
                            PassportScanStep.CONFIRM_MRZ -> "Confirm Details"
                            PassportScanStep.SCAN_NFC -> "Tap Passport"
                            PassportScanStep.COMPLETE -> "Passport Read"
                        },
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        when (uiState.step) {
                            PassportScanStep.MANUAL_MRZ -> onBackToScan()
                            PassportScanStep.CONFIRM_MRZ -> onBackToScan()
                            PassportScanStep.SCAN_NFC -> onBackToScan()
                            else -> onBack()
                        }
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Step indicator
            StepProgressBar(
                currentStep = when (uiState.step) {
                    PassportScanStep.SCAN_MRZ, PassportScanStep.MANUAL_MRZ -> 1
                    PassportScanStep.CONFIRM_MRZ -> 2
                    PassportScanStep.SCAN_NFC -> 3
                    PassportScanStep.COMPLETE -> 4
                },
                totalSteps = 4
            )

            when (uiState.step) {
                PassportScanStep.SCAN_MRZ -> {
                    if (cameraPermissionState.status.isGranted) {
                        MrzCameraScanContent(
                            onEnterManually = onEnterManually,
                            onMrzDetected = { docNum, dob, expiry ->
                                onDocumentNumberChange(docNum)
                                onDateOfBirthChange(dob)
                                onExpiryDateChange(expiry)
                                onConfirmMrz()
                            }
                        )
                    } else {
                        CameraPermissionContent(
                            onRequestPermission = { cameraPermissionState.launchPermissionRequest() },
                            onEnterManually = onEnterManually
                        )
                    }
                }

                PassportScanStep.MANUAL_MRZ -> {
                    ManualMrzEntryContent(
                        documentNumber = documentNumber,
                        dateOfBirth = dateOfBirth,
                        expiryDate = expiryDate,
                        onDocumentNumberChange = onDocumentNumberChange,
                        onDateOfBirthChange = onDateOfBirthChange,
                        onExpiryDateChange = onExpiryDateChange,
                        onConfirm = onConfirmMrz,
                        errorMessage = uiState.errorMessage
                    )
                }

                PassportScanStep.CONFIRM_MRZ -> {
                    ConfirmMrzContent(
                        documentNumber = documentNumber,
                        dateOfBirth = dateOfBirth,
                        expiryDate = expiryDate,
                        onConfirm = onConfirmMrz,
                        onEdit = onEnterManually
                    )
                }

                PassportScanStep.SCAN_NFC -> {
                    NfcScanContent(
                        readingState = readingState,
                        errorMessage = uiState.errorMessage,
                        onRetry = onRetryNfc,
                        onDevBypass = onDevBypassNfc
                    )
                }

                PassportScanStep.COMPLETE -> {
                    PassportDataContent(
                        passportData = passportData,
                        onContinue = {
                            onCapture() // Navigate to next screen
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun StepProgressBar(currentStep: Int, totalSteps: Int) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = "Step $currentStep of $totalSteps",
            color = Color.White,
            fontSize = 14.sp
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            repeat(totalSteps) { index ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(
                            if (index < currentStep) AccentCyan
                            else Color.Gray.copy(alpha = 0.3f)
                        )
                )
            }
        }
    }
}

@Composable
private fun CameraPermissionContent(
    onRequestPermission: () -> Unit,
    onEnterManually: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Outlined.Lock,
            contentDescription = null,
            tint = AccentCyan,
            modifier = Modifier.size(64.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Camera Permission Required",
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "To scan your passport MRZ, we need camera access.\nYour data stays on your device.",
            color = Color.Gray,
            fontSize = 14.sp,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onRequestPermission,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = AccentCyan)
        ) {
            Text("Grant Camera Access", color = DarkBackground, fontWeight = FontWeight.SemiBold)
        }

        Spacer(modifier = Modifier.height(16.dp))

        TextButton(onClick = onEnterManually) {
            Text("Enter MRZ Manually", color = AccentCyan)
        }
    }
}

@Composable
private fun MrzCameraScanContent(
    onEnterManually: () -> Unit,
    onMrzDetected: ((String, String, String) -> Unit)? = null
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var scanStatus by remember { mutableStateOf("Position passport MRZ in the frame") }
    var isScanning by remember { mutableStateOf(true) }

    // MRZ scanner & ML Kit recognizer – created once per composition
    val mrzScanner = remember { MrzScanner() }
    val textRecognizer = remember { TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS) }
    val analysisExecutor = remember { Executors.newSingleThreadExecutor() }
    var mrzDetected by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        onDispose {
            mrzScanner.close()
            textRecognizer.close()
            analysisExecutor.shutdown()
        }
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Camera preview with MRZ overlay
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            // Camera preview
            AndroidView(
                factory = { ctx ->
                    PreviewView(ctx).apply {
                        implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                        scaleType = PreviewView.ScaleType.FILL_CENTER
                    }
                },
                modifier = Modifier.fillMaxSize(),
                update = { previewView ->
                    val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
                    cameraProviderFuture.addListener({
                        try {
                            val cameraProvider = cameraProviderFuture.get()

                            val preview = androidx.camera.core.Preview.Builder().build().also {
                                it.setSurfaceProvider(previewView.surfaceProvider)
                            }

                            // ── ImageAnalysis for live MRZ detection ──
                            val imageAnalysis = ImageAnalysis.Builder()
                                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                .build()

                            imageAnalysis.setAnalyzer(analysisExecutor) { imageProxy ->
                                // Skip if we already detected MRZ or callback is null
                                if (mrzDetected || onMrzDetected == null) {
                                    imageProxy.close()
                                    return@setAnalyzer
                                }

                                @androidx.camera.core.ExperimentalGetImage
                                val mediaImage = imageProxy.image
                                if (mediaImage == null) {
                                    imageProxy.close()
                                    return@setAnalyzer
                                }

                                val inputImage = InputImage.fromMediaImage(
                                    mediaImage,
                                    imageProxy.imageInfo.rotationDegrees
                                )

                                textRecognizer.process(inputImage)
                                    .addOnSuccessListener { visionText ->
                                        if (!mrzDetected) {
                                            val parsed = mrzScanner.extractMrzData(visionText.text)
                                            if (parsed != null && parsed.isValid()) {
                                                Log.d("MrzCamera", "MRZ detected: doc=${parsed.documentNumber} dob=${parsed.dateOfBirth} exp=${parsed.expiryDate}")
                                                mrzDetected = true
                                                isScanning = false
                                                scanStatus = "MRZ detected!"
                                                onMrzDetected(
                                                    parsed.documentNumber,
                                                    parsed.dateOfBirth,
                                                    parsed.expiryDate
                                                )
                                            }
                                        }
                                        imageProxy.close()
                                    }
                                    .addOnFailureListener {
                                        imageProxy.close()
                                    }
                            }

                            cameraProvider.unbindAll()
                            cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                CameraSelector.DEFAULT_BACK_CAMERA,
                                preview,
                                imageAnalysis
                            )

                            scanStatus = "Scanning for MRZ..."
                        } catch (e: Exception) {
                            scanStatus = "Camera error: ${e.message}"
                            isScanning = false
                        }
                    }, ContextCompat.getMainExecutor(context))
                }
            )

            // MRZ scan overlay - draw dark areas around the clear scanning zone
            Canvas(modifier = Modifier.fillMaxSize()) {
                val overlayColor = Color.Black.copy(alpha = 0.7f)

                val frameWidth = size.width * 0.9f
                val frameHeight = frameWidth * 0.18f // MRZ aspect ratio
                val frameLeft = (size.width - frameWidth) / 2
                val frameRight = frameLeft + frameWidth
                val frameTop = size.height * 0.55f
                val frameBottom = frameTop + frameHeight

                // Draw 4 rectangles around the clear area (top, bottom, left, right)

                // Top rectangle (from top of screen to top of frame)
                drawRect(
                    color = overlayColor,
                    topLeft = Offset(0f, 0f),
                    size = Size(size.width, frameTop)
                )

                // Bottom rectangle (from bottom of frame to bottom of screen)
                drawRect(
                    color = overlayColor,
                    topLeft = Offset(0f, frameBottom),
                    size = Size(size.width, size.height - frameBottom)
                )

                // Left rectangle (between top and bottom overlays, left side)
                drawRect(
                    color = overlayColor,
                    topLeft = Offset(0f, frameTop),
                    size = Size(frameLeft, frameHeight)
                )

                // Right rectangle (between top and bottom overlays, right side)
                drawRect(
                    color = overlayColor,
                    topLeft = Offset(frameRight, frameTop),
                    size = Size(size.width - frameRight, frameHeight)
                )

                // Frame border around the clear area
                drawRoundRect(
                    color = AccentCyan,
                    topLeft = Offset(frameLeft, frameTop),
                    size = Size(frameWidth, frameHeight),
                    cornerRadius = CornerRadius(8.dp.toPx()),
                    style = Stroke(width = 3.dp.toPx())
                )

                // Corner accents for better visibility
                val cornerLength = 20.dp.toPx()
                val cornerWidth = 4.dp.toPx()

                // Top-left corner
                drawLine(AccentCyan, Offset(frameLeft, frameTop), Offset(frameLeft + cornerLength, frameTop), cornerWidth)
                drawLine(AccentCyan, Offset(frameLeft, frameTop), Offset(frameLeft, frameTop + cornerLength), cornerWidth)

                // Top-right corner
                drawLine(AccentCyan, Offset(frameRight - cornerLength, frameTop), Offset(frameRight, frameTop), cornerWidth)
                drawLine(AccentCyan, Offset(frameRight, frameTop), Offset(frameRight, frameTop + cornerLength), cornerWidth)

                // Bottom-left corner
                drawLine(AccentCyan, Offset(frameLeft, frameBottom), Offset(frameLeft + cornerLength, frameBottom), cornerWidth)
                drawLine(AccentCyan, Offset(frameLeft, frameBottom - cornerLength), Offset(frameLeft, frameBottom), cornerWidth)

                // Bottom-right corner
                drawLine(AccentCyan, Offset(frameRight - cornerLength, frameBottom), Offset(frameRight, frameBottom), cornerWidth)
                drawLine(AccentCyan, Offset(frameRight, frameBottom - cornerLength), Offset(frameRight, frameBottom), cornerWidth)
            }

            // Instructions
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .padding(top = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Position the MRZ within the frame",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "The MRZ is the 2-line code at the\nbottom of your passport photo page",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center
                )
            }
        }

        // Bottom section
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(CardBackground)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = scanStatus,
                color = Color.White,
                fontSize = 16.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (isScanning) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = AccentCyan,
                    trackColor = Color.Gray.copy(alpha = 0.3f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Info text
            Text(
                text = "Hold your passport steady in good lighting.\nThe MRZ will be detected automatically.",
                color = Color.Gray,
                fontSize = 12.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedButton(
                onClick = onEnterManually,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, AccentCyan)
            ) {
                Text("Enter MRZ Manually Instead", color = AccentCyan, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun ManualMrzEntryContent(
    documentNumber: String,
    dateOfBirth: String,
    expiryDate: String,
    onDocumentNumberChange: (String) -> Unit,
    onDateOfBirthChange: (String) -> Unit,
    onExpiryDateChange: (String) -> Unit,
    onConfirm: () -> Unit,
    errorMessage: String?
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        Text(
            text = "Enter your passport MRZ data",
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "This information is found in the Machine Readable Zone at the bottom of your passport's photo page.",
            color = Color.Gray,
            fontSize = 14.sp
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Document Number
        Text("Document Number", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = documentNumber,
            onValueChange = onDocumentNumberChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("e.g., AB1234567", color = Color.Gray) },
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = AccentCyan,
                unfocusedBorderColor = Color.Gray.copy(alpha = 0.5f),
                focusedContainerColor = CardBackground,
                unfocusedContainerColor = CardBackground,
                cursorColor = AccentCyan
            ),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Date of Birth
        Text("Date of Birth (YYMMDD)", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = dateOfBirth,
            onValueChange = onDateOfBirthChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("e.g., 900115", color = Color.Gray) },
            shape = RoundedCornerShape(12.dp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = AccentCyan,
                unfocusedBorderColor = Color.Gray.copy(alpha = 0.5f),
                focusedContainerColor = CardBackground,
                unfocusedContainerColor = CardBackground,
                cursorColor = AccentCyan
            ),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Expiry Date
        Text("Expiry Date (YYMMDD)", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = expiryDate,
            onValueChange = onExpiryDateChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("e.g., 300115", color = Color.Gray) },
            shape = RoundedCornerShape(12.dp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = AccentCyan,
                unfocusedBorderColor = Color.Gray.copy(alpha = 0.5f),
                focusedContainerColor = CardBackground,
                unfocusedContainerColor = CardBackground,
                cursorColor = AccentCyan
            ),
            singleLine = true
        )

        // Error message
        if (errorMessage != null) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = errorMessage,
                color = Color(0xFFFF5252),
                fontSize = 14.sp
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        // Privacy notice
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = CardBackground)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Outlined.Lock,
                    contentDescription = null,
                    tint = AccentGreen,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "This data is only used to authenticate with your passport chip. It's never uploaded.",
                    color = Color.Gray,
                    fontSize = 12.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onConfirm,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(28.dp),
            colors = ButtonDefaults.buttonColors(containerColor = AccentCyan),
            enabled = documentNumber.isNotEmpty() && dateOfBirth.length == 6 && expiryDate.length == 6
        ) {
            Text("Continue", color = DarkBackground, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun ConfirmMrzContent(
    documentNumber: String,
    dateOfBirth: String,
    expiryDate: String,
    onConfirm: () -> Unit,
    onEdit: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(20.dp))

        Icon(
            Icons.Outlined.CheckCircle,
            contentDescription = null,
            tint = AccentGreen,
            modifier = Modifier.size(64.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "MRZ Data Detected",
            color = Color.White,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Data cards
        MrzDataCard("Document Number", documentNumber)
        Spacer(modifier = Modifier.height(12.dp))
        MrzDataCard("Date of Birth", formatDate(dateOfBirth))
        Spacer(modifier = Modifier.height(12.dp))
        MrzDataCard("Expiry Date", formatDate(expiryDate))

        Spacer(modifier = Modifier.height(24.dp))

        TextButton(onClick = onEdit) {
            Text("Edit Details", color = AccentCyan)
        }

        Spacer(modifier = Modifier.weight(1f))

        Text(
            text = "Next: Tap your passport on your phone",
            color = Color.Gray,
            fontSize = 14.sp
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onConfirm,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(28.dp),
            colors = ButtonDefaults.buttonColors(containerColor = AccentCyan)
        ) {
            Text("Proceed to NFC Scan", color = DarkBackground, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun MrzDataCard(label: String, value: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, color = Color.Gray, fontSize = 14.sp)
            Text(value, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Medium)
        }
    }
}

private fun formatDate(yymmdd: String): String {
    if (yymmdd.length != 6) return yymmdd
    val yy = yymmdd.substring(0, 2)
    val mm = yymmdd.substring(2, 4)
    val dd = yymmdd.substring(4, 6)
    return "$dd/$mm/20$yy"
}

@Composable
private fun NfcScanContent(
    readingState: PassportReadingState,
    errorMessage: String?,
    onRetry: () -> Unit,
    onDevBypass: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // NFC animation/icon
        Box(
            modifier = Modifier
                .size(150.dp)
                .clip(CircleShape)
                .background(CardBackground)
                .border(3.dp, AccentCyan, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            when (readingState) {
                PassportReadingState.Connecting,
                PassportReadingState.ReadingData,
                PassportReadingState.VerifyingAuthenticity -> {
                    CircularProgressIndicator(
                        color = AccentCyan,
                        strokeWidth = 4.dp,
                        modifier = Modifier.size(80.dp)
                    )
                }
                is PassportReadingState.Error -> {
                    Icon(
                        Icons.Outlined.Warning,
                        contentDescription = null,
                        tint = Color(0xFFFF5252),
                        modifier = Modifier.size(60.dp)
                    )
                }
                else -> {
                    // NFC icon representation - phone with waves
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Outlined.AccountBox,
                            contentDescription = null,
                            tint = AccentCyan,
                            modifier = Modifier.size(50.dp)
                        )
                        Text(
                            text = "NFC",
                            color = AccentCyan,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = when (readingState) {
                PassportReadingState.WaitingForNfc -> "Hold Passport to Phone"
                PassportReadingState.Connecting -> "Connecting..."
                PassportReadingState.ReadingData -> "Reading Passport Data..."
                PassportReadingState.VerifyingAuthenticity -> "Verifying Authenticity..."
                is PassportReadingState.Error -> "Reading Failed"
                else -> "Ready to Scan"
            },
            color = Color.White,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = when (readingState) {
                PassportReadingState.WaitingForNfc -> "Place the passport's chip area against the back of your phone"
                PassportReadingState.Connecting -> "Establishing secure connection..."
                PassportReadingState.ReadingData -> "Extracting passport information..."
                PassportReadingState.VerifyingAuthenticity -> "Checking passport authenticity..."
                is PassportReadingState.Error -> errorMessage ?: readingState.message
                else -> "Position your passport's NFC chip"
            },
            color = Color.Gray,
            fontSize = 14.sp,
            textAlign = TextAlign.Center
        )

        if (readingState is PassportReadingState.Error || errorMessage != null) {
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = onRetry,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AccentCyan)
            ) {
                Text("Try Again", color = DarkBackground, fontWeight = FontWeight.SemiBold)
            }
        }

        Spacer(modifier = Modifier.height(40.dp))

        // Developer bypass button (REMOVE BEFORE PRODUCTION)
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onDevBypass,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F))
        ) {
            Text(
                "DEV: Bypass NFC & Get Credential",
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Tips
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = CardBackground)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Tips for NFC Scanning:", color = Color.White, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(8.dp))
                Text("• Remove passport from any cover", color = Color.Gray, fontSize = 13.sp)
                Text("• The NFC chip is usually near the photo", color = Color.Gray, fontSize = 13.sp)
                Text("• Hold still until reading completes", color = Color.Gray, fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun PassportDataContent(
    passportData: PassportData?,
    onContinue: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Filled.CheckCircle,
            contentDescription = null,
            tint = AccentGreen,
            modifier = Modifier.size(64.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Passport Read Successfully!",
            color = Color.White,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(24.dp))

        if (passportData != null) {
            // Photo
            if (passportData.photo != null) {
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .border(3.dp, AccentCyan, CircleShape)
                ) {
                    androidx.compose.foundation.Image(
                        bitmap = passportData.photo.asImageBitmap(),
                        contentDescription = "Passport Photo",
                        modifier = Modifier.fillMaxSize()
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .background(CardBackground)
                        .border(3.dp, AccentCyan, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.Person,
                        contentDescription = null,
                        tint = Color.Gray,
                        modifier = Modifier.size(50.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Personal info
            Text(
                text = passportData.fullName,
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = passportData.nationality,
                color = AccentCyan,
                fontSize = 14.sp
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Details cards
            PassportInfoCard("Document Number", passportData.documentNumber)
            Spacer(modifier = Modifier.height(8.dp))
            PassportInfoCard("Date of Birth", passportData.formattedDateOfBirth)
            Spacer(modifier = Modifier.height(8.dp))
            PassportInfoCard("Gender", passportData.gender)
            Spacer(modifier = Modifier.height(8.dp))
            PassportInfoCard("Expiry Date", passportData.formattedExpiryDate)

            // Verification status
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = AccentGreen,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (passportData.isAuthentic) "Authentic passport verified" else "Authenticity check pending",
                    color = AccentGreen,
                    fontSize = 14.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Privacy notice
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = AccentGreen.copy(alpha = 0.15f))
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Outlined.Lock,
                    contentDescription = null,
                    tint = AccentGreen,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Privacy Protected",
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Raw passport data is NOT stored. Only cryptographic credentials are saved locally.",
                        color = Color.Gray,
                        fontSize = 12.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onContinue,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(28.dp),
            colors = ButtonDefaults.buttonColors(containerColor = AccentCyan)
        ) {
            Text("Continue", color = DarkBackground, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
        }
    }
}

@Composable
private fun PassportInfoCard(label: String, value: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, color = Color.Gray, fontSize = 14.sp)
            Text(value, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ScanPassportScreenPreview() {
    ZKTheme {
        ScanPassportScreen()
    }
}
