package com.example.zk.ui.screens

import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.zk.data.WalletDataStore
import com.example.zk.util.ZKPEngine
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val TAG = "MyQrScreen"
private val DarkBackground = Color(0xFF0D1421)
private val CardBackground = Color(0xFF1A2332)
private val AccentCyan = Color(0xFF00D9FF)

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
    onBack: () -> Unit = {}
) {
    // Decode bitmask: bit0=photo, bit1=name, bit2=nationality, bit3=gender
    val discloseName = disclosureMask and 2 != 0
    val discloseNationality = disclosureMask and 4 != 0
    val discloseGender = disclosureMask and 8 != 0
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val walletDataStore = remember { WalletDataStore(context) }
    val zkpEngine = remember { ZKPEngine(context) }

    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var qrBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var proofSummary by remember { mutableStateOf<String?>(null) }

    // Clean up WebView when leaving the screen
    DisposableEffect(Unit) {
        onDispose { zkpEngine.destroy() }
    }

    // On first composition, kick off proof generation
    LaunchedEffect(Unit) {
        scope.launch {
            isLoading = true
            errorMessage = null
            try {
                // 1. Read the stored Verifiable Credential
                Log.d(TAG, "Reading stored credential …")
                val credentialJson = walletDataStore.credential.first()
                if (credentialJson.isNullOrBlank()) {
                    errorMessage = "No credential found. Scan your passport first."
                    isLoading = false
                    return@launch
                }

                Log.d(TAG, "Credential loaded (${credentialJson.length} chars)")

                // 2. Extract ZK‑friendly integers from the VC
                val vc = Gson().fromJson(credentialJson, JsonObject::class.java)
                val subject = vc.getAsJsonObject("credentialSubject")

                val dob = subject.get("dateOfBirth").asInt
                val passportNumber = subject.get("passportNumber").asInt
                val nationality = subject.get("nationality").asInt

                // Resolve the user's full name for optional disclosure
                val fullName = walletDataStore.passportFullName.first().ifBlank {
                    subject.get("holderDid")?.asString ?: "Unknown"
                }
                val userNationality = walletDataStore.passportNationality.first()
                val userGender = walletDataStore.passportGender.first()

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

                // 5. Build compact JSON for the QR code
                val qrObject = JsonObject().apply {
                    addProperty("type", proofTypeKey(proofType))
                    addProperty("label", proofTypeLabel(proofType))
                    if (discloseName) addProperty("name", fullName)
                    if (discloseNationality) addProperty("nationality", userNationality)
                    if (discloseGender) addProperty("gender", userGender)
                    add("proof", Gson().fromJson(result.proofJson, JsonObject::class.java))
                    add("publicSignals", Gson().fromJson(result.publicSignalsJson, com.google.gson.JsonArray::class.java))
                }
                val qrPayload = Gson().toJson(qrObject)
                Log.d(TAG, "QR payload length: ${qrPayload.length}")

                proofSummary = "Proof generated • ${qrPayload.length} bytes"

                // 6. Generate QR bitmap (off main thread)
                val bitmap = withContext(Dispatchers.Default) {
                    generateQrBitmap(qrPayload, size = 1024)
                }
                qrBitmap = bitmap
                Log.d(TAG, "QR bitmap generated: ${bitmap.width}x${bitmap.height}")

                // 7. Save to proof history
                walletDataStore.addProofHistoryEntry(
                    WalletDataStore.ProofHistoryEntry(
                        proofType = proofTypeKey(proofType),
                        label = proofTypeLabel(proofType),
                        timestamp = System.currentTimeMillis(),
                        disclosedName = discloseName,
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

    Scaffold(
        containerColor = DarkBackground,
        topBar = {
            TopAppBar(
                title = {
                    Text("ZK Proof QR", color = Color.White, fontWeight = FontWeight.Medium)
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
                        "This may take a few seconds",
                        color = Color.Gray,
                        fontSize = 13.sp
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

                // ── QR Code ─────────────────────────────────────────────
                qrBitmap != null -> {
                    Text(
                        "Present this QR to a verifier",
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

                    Spacer(modifier = Modifier.height(24.dp))

                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Image(
                            bitmap = qrBitmap!!.asImageBitmap(),
                            contentDescription = "ZK Proof QR Code",
                            modifier = Modifier
                                .size(280.dp)
                                .padding(12.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    if (proofSummary != null) {
                        Text(
                            proofSummary!!,
                            color = Color.Gray,
                            fontSize = 12.sp
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
