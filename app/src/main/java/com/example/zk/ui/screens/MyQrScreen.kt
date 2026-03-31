package com.example.zk.ui.screens

import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import android.media.AudioManager
import android.media.ToneGenerator
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import android.view.WindowManager
import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.example.zk.data.WalletDataStore
import com.example.zk.network.IssuerApiClient
import com.example.zk.util.CryptoManager
import com.example.zk.util.ZKPEngine
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val TAG = "MyQrScreen"
private val DarkBackground = Color(0xFF0D1421)
private val CardBackground = Color(0xFF1A2332)
private val AccentCyan = Color(0xFF00D9FF)
private val SuccessGreen = Color(0xFF4CAF50)

// Proof type constants matching GenerateProofScreen template indices
const val PROOF_TYPE_AGE_18 = 0
const val PROOF_TYPE_NATIONALITY = 1
const val PROOF_TYPE_CREDENTIAL_VALID = 2

private fun proofTypeKey(type: Int): String = when (type) {
    PROOF_TYPE_NATIONALITY -> "nationality"
    PROOF_TYPE_CREDENTIAL_VALID -> "credential_valid"
    else -> "age_18"
}

private fun proofTypeLabel(type: Int): String = when (type) {
    PROOF_TYPE_NATIONALITY -> "Nationality"
    PROOF_TYPE_CREDENTIAL_VALID -> "Credential Valid"
    else -> "Age ≥ 18"
}

private fun proofTypeSubtitle(type: Int): String = when (type) {
    PROOF_TYPE_NATIONALITY -> "Nationality — Zero\u2011Knowledge Proof"
    PROOF_TYPE_CREDENTIAL_VALID -> "Credential Valid — Zero\u2011Knowledge Proof"
    else -> "Age ≥ 18 — Zero\u2011Knowledge Proof"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyQrScreen(
    proofType: Int = PROOF_TYPE_AGE_18,
    disclosureMask: Int = 0,
    nonce: String = "",
    onBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val walletDataStore = remember { WalletDataStore(context) }
    val zkpEngine = remember { ZKPEngine(context) }

    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var qrBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var proofSummary by remember { mutableStateOf<String?>(null) }

    // Polling states
    var verifiedByOfficer by remember { mutableStateOf(false) }
    var pollingTimedOut by remember { mutableStateOf(false) }
    var countdownSeconds by remember { mutableIntStateOf(60) }

    // Biometric gate
    var biometricPassed by remember { mutableStateOf(false) }
    var biometricFailed by remember { mutableStateOf(false) }

    // Offline indicator
    val isOnline = remember {
        val cm = context.getSystemService(android.content.Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        val network = cm?.activeNetwork
        val caps = cm?.getNetworkCapabilities(network)
        caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
    }

    // Back navigation lock — show confirmation dialog
    var showExitDialog by remember { mutableStateOf(false) }

    // Intercept hardware back button
    BackHandler {
        showExitDialog = true
    }

    // Clean up WebView when leaving the screen
    DisposableEffect(Unit) {
        onDispose { zkpEngine.destroy() }
    }

    // Lock screen to portrait to prevent proof regeneration on rotation
    val activity = context as? Activity
    DisposableEffect(Unit) {
        val originalOrientation = activity?.requestedOrientation
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        onDispose {
            activity?.requestedOrientation = originalOrientation ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    // Auto-boost brightness when QR is displayed
    DisposableEffect(qrBitmap) {
        val window = activity?.window
        val originalBrightness = window?.attributes?.screenBrightness ?: -1f
        if (qrBitmap != null && window != null) {
            val params = window.attributes
            params.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_FULL
            window.attributes = params
        }
        onDispose {
            if (window != null) {
                val params = window.attributes
                params.screenBrightness = originalBrightness
                window.attributes = params
            }
        }
    }

    // Exit confirmation dialog
    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            containerColor = CardBackground,
            titleContentColor = Color.White,
            textContentColor = Color.Gray,
            title = { Text("Cancel Verification?") },
            text = { Text("This will end the current session. The Officer will need to generate a new challenge.") },
            confirmButton = {
                TextButton(onClick = {
                    showExitDialog = false
                    onBack()
                }) {
                    Text("Yes, Cancel", color = Color(0xFFFF5252))
                }
            },
            dismissButton = {
                TextButton(onClick = { showExitDialog = false }) {
                    Text("Stay", color = AccentCyan)
                }
            }
        )
    }

    // Biometric authentication gate
    LaunchedEffect(Unit) {
        val fragmentActivity = context as? FragmentActivity
        if (fragmentActivity == null) {
            biometricPassed = true // fallback if not a FragmentActivity
            return@LaunchedEffect
        }

        val biometricManager = BiometricManager.from(context)
        val canAuth = biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.BIOMETRIC_WEAK)

        if (canAuth != BiometricManager.BIOMETRIC_SUCCESS) {
            Log.d(TAG, "Biometric not available (code=$canAuth), skipping gate")
            biometricPassed = true
            return@LaunchedEffect
        }

        val executor = ContextCompat.getMainExecutor(context)
        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                Log.d(TAG, "Biometric authentication succeeded")
                biometricPassed = true
            }
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                Log.e(TAG, "Biometric error ($errorCode): $errString")
                biometricFailed = true
                errorMessage = "Authentication required: $errString"
            }
            override fun onAuthenticationFailed() {
                Log.w(TAG, "Biometric authentication failed")
            }
        }

        val prompt = BiometricPrompt(fragmentActivity, executor, callback)
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Identity Verification")
            .setSubtitle("Authenticate to generate your ZK proof")
            .setNegativeButtonText("Cancel")
            .build()
        prompt.authenticate(promptInfo)
    }

    // On biometric pass, kick off proof generation
    LaunchedEffect(biometricPassed) {
        if (!biometricPassed) return@LaunchedEffect
        scope.launch {
            isLoading = true
            errorMessage = null
            try {
                // 1. Read the stored Verifiable Credential (may be null if issuer API was unreachable)
                Log.d(TAG, "Reading stored credential …")
                val credentialJson = walletDataStore.credential.first()

                // 2. Extract ZK‑friendly integers — from VC if available, else from local passport fields
                val dob: Int
                val passportNumber: Int
                val nationality: Int

                if (!credentialJson.isNullOrBlank()) {
                    Log.d(TAG, "Credential loaded (${credentialJson.length} chars)")
                    val vc = Gson().fromJson(credentialJson, JsonObject::class.java)
                    val subject = vc.getAsJsonObject("credentialSubject")
                    dob = subject.get("dateOfBirth").asInt
                    passportNumber = subject.get("passportNumber").asInt
                    nationality = subject.get("nationality").asInt
                } else {
                    // Fallback: build ZK inputs from locally stored passport data
                    Log.d(TAG, "No VC JSON — using local passport fields")
                    val rawDob = walletDataStore.passportDateOfBirth.first() // "DD/MM/YYYY"
                    val rawDocNum = walletDataStore.passportDocNumber.first()
                    val rawNat = walletDataStore.passportNationality.first()

                    if (rawDob.isBlank() || rawDocNum.isBlank()) {
                        errorMessage = "No credential found. Scan your passport first."
                        isLoading = false
                        return@launch
                    }

                    // Convert DD/MM/YYYY → YYYYMMDD int
                    dob = try {
                        val parts = rawDob.split("/")
                        "${parts[2]}${parts[1]}${parts[0]}".toInt()
                    } catch (_: Exception) { 0 }

                    // Deterministic string → positive Int (mirrors PassportViewModel logic)
                    passportNumber = rawDocNum.toIntOrNull()
                        ?: (kotlin.math.abs(rawDocNum.hashCode()) % 1_000_000_000)
                    nationality = rawNat.toIntOrNull()
                        ?: (kotlin.math.abs(rawNat.hashCode()) % 1_000_000_000)
                }

                Log.d(TAG, "Extracted inputs – dob=$dob, passport#=$passportNumber, nat=$nationality, proofType=$proofType")

                // 3. Compute a dynamic age threshold (18 years before today)
                val cal = java.util.Calendar.getInstance()
                val ageThreshold = (cal.get(java.util.Calendar.YEAR) - 18) * 10000 +
                        (cal.get(java.util.Calendar.MONTH) + 1) * 100 +
                        cal.get(java.util.Calendar.DAY_OF_MONTH)

                // 4. Generate ZK proof via WebView engine
                Log.d(TAG, "Calling ZKPEngine.computeProof() …")
                val result = zkpEngine.computeProof(
                    dob = dob,
                    passportNumber = passportNumber,
                    nationality = nationality,
                    ageThreshold = ageThreshold
                )
                Log.d(TAG, "Proof received from ZKPEngine")

                // 5. Sign the proof + nonce with device key
                val publicSignals = Gson().fromJson(result.publicSignalsJson, com.google.gson.JsonArray::class.java)
                val firstSignal = if (publicSignals.size() > 0) publicSignals[0].asString else ""
                val payloadToSign = "${firstSignal}_${nonce}"

                Log.d(TAG, "Signing payload: ${payloadToSign.take(40)}…")
                val signature = CryptoManager.signPayload(payloadToSign)
                val publicKey = CryptoManager.getPublicKeyBase64()
                Log.d(TAG, "Signature generated (${signature.length} chars)")
                Log.d(TAG, "Public key: ${publicKey.take(20)}…")

                // 6. Build compact response JSON for the QR code (data minimization)
                val qrObject = JsonObject().apply {
                    add("proof", Gson().fromJson(result.proofJson, JsonObject::class.java))
                    add("publicSignals", publicSignals)
                    addProperty("nonce", nonce)
                    addProperty("signature", signature)
                    addProperty("publicKey", publicKey)

                    // Add label and type so Verifier can display what proof this is
                    addProperty("label", proofTypeLabel(proofType))
                    addProperty("type", proofTypeKey(proofType))

                    // Selectively disclose data based on user configuration bitmask
                    if ((disclosureMask and 2) != 0) {
                        addProperty("name", walletDataStore.passportFullName.first())
                    }
                    if ((disclosureMask and 4) != 0) {
                        addProperty("nationality", walletDataStore.passportNationality.first())
                    }
                    if ((disclosureMask and 8) != 0) {
                        addProperty("gender", walletDataStore.passportGender.first())
                    }
                }
                val qrPayload = Gson().toJson(qrObject)
                Log.d(TAG, "QR payload length: ${qrPayload.length}")

                proofSummary = "Signed proof • ${qrPayload.length} bytes"

                // 7. Generate QR bitmap (off main thread)
                val bitmap = withContext(Dispatchers.Default) {
                    generateQrBitmap(qrPayload, size = 1024)
                }
                qrBitmap = bitmap
                Log.d(TAG, "QR bitmap generated: ${bitmap.width}x${bitmap.height}")

                // 8. Save to proof history
                walletDataStore.addProofHistoryEntry(
                    WalletDataStore.ProofHistoryEntry(
                        proofType = proofTypeKey(proofType),
                        label = proofTypeLabel(proofType),
                        timestamp = System.currentTimeMillis(),
                        disclosedName = false,
                        success = true,
                        proofSizeBytes = qrPayload.length
                    )
                )

            } catch (e: Exception) {
                Log.e(TAG, "Proof generation failed", e)
                errorMessage = e.localizedMessage ?: "Unknown error"
            } finally {
                isLoading = false
            }
        }
    }

    // ── Polling loop: check if the officer has verified ──────────────────
    LaunchedEffect(nonce, qrBitmap) {
        // Only start polling once the QR is displayed and we have a valid nonce
        if (qrBitmap == null || nonce.isBlank()) return@LaunchedEffect

        val pollingIntervalMs = 2_000L
        val timeoutMs = 60_000L
        val startTime = System.currentTimeMillis()

        Log.d(TAG, "Starting polling for nonce=${nonce.take(8)}…")

        withContext(Dispatchers.IO) {
            while (true) {
                val elapsed = System.currentTimeMillis() - startTime
                countdownSeconds = ((timeoutMs - elapsed) / 1000).toInt().coerceAtLeast(0)

                if (elapsed >= timeoutMs) {
                    Log.d(TAG, "Polling timed out after ${elapsed}ms")
                    pollingTimedOut = true
                    return@withContext
                }

                try {
                    val response = IssuerApiClient.api.getSessionStatus(nonce)
                    Log.d(TAG, "Poll response: status=${response.status}")

                    if (response.status == "success") {
                        Log.d(TAG, "Officer confirmed verification!")
                        verifiedByOfficer = true

                        // Haptic feedback + sound on success
                        withContext(Dispatchers.Main) {
                            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                val vm = context.getSystemService(android.content.Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                                vm?.defaultVibrator
                            } else {
                                @Suppress("DEPRECATION")
                                context.getSystemService(android.content.Context.VIBRATOR_SERVICE) as? Vibrator
                            }
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                vibrator?.vibrate(VibrationEffect.createOneShot(400, VibrationEffect.DEFAULT_AMPLITUDE))
                            } else {
                                @Suppress("DEPRECATION")
                                vibrator?.vibrate(400)
                            }
                            try {
                                val toneGen = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100)
                                toneGen.startTone(ToneGenerator.TONE_PROP_ACK, 200)
                            } catch (_: Exception) {}
                        }
                        return@withContext
                    }
                } catch (e: Exception) {
                    // Network error — silently continue polling
                    Log.w(TAG, "Poll failed (will retry): ${e.message}")
                }

                delay(pollingIntervalMs)
            }
        }
    }

    Scaffold(
        containerColor = DarkBackground,
        topBar = {
            TopAppBar(
                title = {
                    Text("ZK Proof Response", color = Color.White, fontWeight = FontWeight.Medium)
                },
                // No back button — navigation is locked
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            when {
                // ── Loading ─────────────────────────────────────────────
                isLoading -> {
                    CircularProgressIndicator(color = AccentCyan, strokeWidth = 4.dp)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Generating ZK Proof …",
                        color = Color.White,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Computing proof and signing with your device key",
                        color = Color.Gray,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "Step 2 of 3 — Generating Proof",
                        color = Color.Gray.copy(alpha = 0.5f),
                        fontSize = 11.sp
                    )
                }

                // ── Error ───────────────────────────────────────────────
                errorMessage != null -> {
                    Text(
                        "Proof Generation Failed",
                        color = Color(0xFFFF5252),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        errorMessage!!,
                        color = Color.Gray,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = onBack,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentCyan)
                    ) {
                        Text("Go Back", color = DarkBackground, fontWeight = FontWeight.SemiBold)
                    }
                }

                // ── QR Code (waiting for officer to scan) ───────────────
                qrBitmap != null && !verifiedByOfficer && !pollingTimedOut -> {
                    Text(
                        "Present this response to the Officer",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        proofTypeSubtitle(proofType),
                        color = AccentCyan,
                        fontSize = 14.sp
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Image(
                            bitmap = qrBitmap!!.asImageBitmap(),
                            contentDescription = "ZK Proof Response QR Code",
                            modifier = Modifier
                                .size(280.dp)
                                .padding(12.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Status indicators
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text("🔒", fontSize = 14.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "Signed & nonce-bound",
                            color = SuccessGreen,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    if (proofSummary != null) {
                        Text(
                            proofSummary!!,
                            color = Color.Gray,
                            fontSize = 12.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Polling indicator with countdown
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = AccentCyan,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Waiting for Officer to scan… ${countdownSeconds}s",
                            color = Color.Gray,
                            fontSize = 13.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Step indicator
                    Text(
                        "Step 3 of 3 — Awaiting Confirmation",
                        color = Color.Gray.copy(alpha = 0.5f),
                        fontSize = 11.sp
                    )

                    // Offline indicator
                    if (!isOnline) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color(0xFFFF9800).copy(alpha = 0.15f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFFF9800))
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    "Offline • Verification still works",
                                    color = Color(0xFFFF9800),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Finish button — safely navigate home
                    Button(
                        onClick = onBack,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentCyan)
                    ) {
                        Text(
                            "Finish Verification",
                            color = DarkBackground,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // ── Verified by Officer ──────────────────────────────────
                verifiedByOfficer -> {
                    Text("✅", fontSize = 72.sp)
                    Spacer(modifier = Modifier.height(20.dp))
                    Text(
                        "Verification Successful!",
                        color = SuccessGreen,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "The Officer has confirmed your identity.",
                        color = Color.Gray,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(32.dp))
                    Button(
                        onClick = onBack,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen)
                    ) {
                        Text(
                            "Return Home",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // ── Polling timed out ────────────────────────────────────
                pollingTimedOut -> {
                    Text("⏱️", fontSize = 56.sp)
                    Spacer(modifier = Modifier.height(20.dp))
                    Text(
                        "Session Timed Out",
                        color = Color(0xFFFF9800),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "The Officer did not scan the response QR in time.",
                        color = Color.Gray,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = onBack,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentCyan)
                    ) {
                        Text(
                            "Try Again",
                            color = DarkBackground,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

// ── QR bitmap helper (runs on Default dispatcher) ───────────────────────────

private fun generateQrBitmap(content: String, size: Int = 512): Bitmap {
    val writer = QRCodeWriter()
    val bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, size, size)
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565)
    for (x in 0 until size) {
        for (y in 0 until size) {
            bitmap.setPixel(x, y, if (bitMatrix[x, y]) AndroidColor.BLACK else AndroidColor.WHITE)
        }
    }
    return bitmap
}
