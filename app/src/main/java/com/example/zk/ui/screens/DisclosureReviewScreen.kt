package com.example.zk.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val DarkBackground = Color(0xFF0D1421)
private val CardBackground = Color(0xFF1A2332)
private val AccentCyan = Color(0xFF00D9FF)
private val AccentGreen = Color(0xFF4CAF50)
private val AccentAmber = Color(0xFFFF9800)

// Total optional disclosure fields available (name, nationality, gender)
private const val TOTAL_DISCLOSURE_FIELDS = 3

/**
 * Disclosure review screen shown after scanning the officer's challenge QR.
 * Displays what the officer is requesting and lets the citizen toggle optional disclosures.
 *
 * @param proofType Proof type int (0 = age ≥ 18, 1 = nationality, 2 = credential valid)
 * @param requestedDisclosures Fields the officer requested (e.g. ["name", "nationality"])
 * @param nonce The challenge nonce from the officer's QR
 * @param onConfirm Called with (proofType, disclosureMask, nonce) when the citizen confirms
 * @param onBack Navigate back
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DisclosureReviewScreen(
    proofType: Int = 0,
    requestedDisclosures: List<String> = emptyList(),
    nonce: String = "",
    onConfirm: (proofType: Int, disclosureMask: Int, nonce: String) -> Unit = { _, _, _ -> },
    onBack: () -> Unit = {}
) {
    // Disclosure toggle states — officer-required ones start as true and locked
    val nameRequired = "name" in requestedDisclosures
    val nationalityRequired = "nationality" in requestedDisclosures
    val genderRequired = "gender" in requestedDisclosures

    var nameSelected by remember { mutableStateOf(nameRequired) }
    var nationalitySelected by remember { mutableStateOf(nationalityRequired) }
    var genderSelected by remember { mutableStateOf(genderRequired) }

    // Compute disclosure mask: bit 1 = name (0x2), bit 2 = nationality (0x4), bit 3 = gender (0x8)
    // Note: bit 0 (0x1) was previously passport photo — now removed
    val disclosureMask = (if (nameSelected) 2 else 0) or
            (if (nationalitySelected) 4 else 0) or
            (if (genderSelected) 8 else 0)

    // Count shared fields
    val sharedCount = listOf(nameSelected, nationalitySelected, genderSelected).count { it }

    // Proof type labels
    val proofTitle = when (proofType) {
        1 -> "Nationality Verification"
        2 -> "Credential Validation"
        else -> "Age Verification (≥ 18)"
    }

    val proofDescription = when (proofType) {
        1 -> "Prove your citizenship without revealing your passport number"
        2 -> "Verify your credential signature is valid"
        else -> "Prove you are over 18 without revealing your date of birth"
    }

    val proofEmoji = when (proofType) {
        1 -> "🌍"
        2 -> "✅"
        else -> "🎂"
    }

    Scaffold(
        containerColor = DarkBackground,
        topBar = {
            TopAppBar(
                title = {
                    Text("Review & Confirm", color = Color.White, fontWeight = FontWeight.Medium)
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
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
            ) {
                Spacer(modifier = Modifier.height(8.dp))

                // ── Request Summary Card (Enhancement #5) ────────────────
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = AccentCyan.copy(alpha = 0.08f)
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, AccentCyan.copy(alpha = 0.3f))
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("🛂", fontSize = 24.sp)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                "Verifier Request",
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // What the officer wants to verify
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(proofEmoji, fontSize = 16.sp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    proofTitle,
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    proofDescription,
                                    color = Color.Gray,
                                    fontSize = 12.sp,
                                    lineHeight = 16.sp
                                )
                            }
                        }

                        // Requested disclosures summary
                        if (requestedDisclosures.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                            Spacer(modifier = Modifier.height(12.dp))

                            requestedDisclosures.forEach { key ->
                                val label = when (key) {
                                    "name" -> "Your full name"
                                    "nationality" -> "Your nationality"
                                    "gender" -> "Your gender"
                                    else -> key
                                }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(vertical = 2.dp)
                                ) {
                                    Text("📋", fontSize = 14.sp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        "Share $label",
                                        color = AccentCyan,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // ── Disclosure Items with Visual Diff (Enhancement #6) ────
                Text(
                    "Selective Disclosure",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Choose what personal data to share alongside your proof",
                    color = Color.Gray,
                    fontSize = 12.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Full Name
                DisclosureItem(
                    icon = Icons.Outlined.Person,
                    title = "Full Name",
                    isRequired = nameRequired,
                    isChecked = nameSelected,
                    onCheckedChange = { if (!nameRequired) nameSelected = it }
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Nationality
                DisclosureItem(
                    icon = Icons.Outlined.Place,
                    title = "Nationality",
                    isRequired = nationalityRequired,
                    isChecked = nationalitySelected,
                    onCheckedChange = { if (!nationalityRequired) nationalitySelected = it }
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Gender
                DisclosureItem(
                    icon = Icons.Outlined.Person,
                    title = "Gender",
                    isRequired = genderRequired,
                    isChecked = genderSelected,
                    onCheckedChange = { if (!genderRequired) genderSelected = it }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // ── Minimal Disclosure Badge (Enhancement #8) ─────────────
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (sharedCount == 0) AccentGreen.copy(alpha = 0.15f)
                            else AccentAmber.copy(alpha = 0.15f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("🛡️", fontSize = 14.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (sharedCount == 0) "Maximum privacy: no personal data shared"
                                   else "Minimal disclosure: $sharedCount of $TOTAL_DISCLOSURE_FIELDS fields shared",
                            color = if (sharedCount == 0) AccentGreen else AccentAmber,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Privacy assurance
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        Icons.Filled.CheckCircle,
                        contentDescription = null,
                        tint = AccentGreen,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        "Only selected fields are shared alongside the ZK proof.",
                        color = AccentGreen,
                        fontSize = 11.sp
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Step indicator
                Text(
                    "Step 2 of 3 — Review Disclosures",
                    color = Color.Gray.copy(alpha = 0.5f),
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))
            }

            // ── Confirm Button ───────────────────────────────────────────
            Button(
                onClick = { onConfirm(proofType, disclosureMask, nonce) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp)
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AccentCyan)
            ) {
                Text(
                    "Confirm & Generate Proof",
                    color = DarkBackground,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

// ── Disclosure item composable ──────────────────────────────────────────────

/**
 * A single disclosure row with three visual states:
 * - 🔒 Required (locked ON, cyan highlight)
 * - ➕ Voluntarily added (green, toggleable)
 * - 🚫 Hidden (grey, toggleable)
 */
@Composable
private fun DisclosureItem(
    icon: ImageVector,
    title: String,
    isRequired: Boolean,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val borderColor = when {
        isRequired -> AccentCyan
        isChecked -> AccentGreen
        else -> Color.Transparent
    }

    val backgroundColor = when {
        isRequired -> AccentCyan.copy(alpha = 0.08f)
        isChecked -> AccentGreen.copy(alpha = 0.06f)
        else -> CardBackground
    }

    val statusLabel = when {
        isRequired -> "Required by verifier"
        isChecked -> "Voluntarily shared"
        else -> "Hidden"
    }

    val statusColor = when {
        isRequired -> AccentCyan
        isChecked -> AccentGreen
        else -> Color.Gray
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        border = if (borderColor != Color.Transparent) {
            androidx.compose.foundation.BorderStroke(1.dp, borderColor.copy(alpha = 0.5f))
        } else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            Icon(
                icon,
                contentDescription = null,
                tint = statusColor,
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            // Title + status
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 2.dp)
                ) {
                    if (isRequired) {
                        Icon(
                            Icons.Filled.Lock,
                            contentDescription = null,
                            tint = AccentCyan,
                            modifier = Modifier.size(11.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    Text(
                        statusLabel,
                        color = statusColor,
                        fontSize = 11.sp
                    )
                }
            }

            // Checkbox (disabled for required fields)
            if (isRequired) {
                // Locked-on indicator
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(AccentCyan.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.CheckCircle,
                        contentDescription = "Required",
                        tint = AccentCyan,
                        modifier = Modifier.size(20.dp)
                    )
                }
            } else {
                Checkbox(
                    checked = isChecked,
                    onCheckedChange = onCheckedChange,
                    colors = CheckboxDefaults.colors(
                        checkedColor = AccentGreen,
                        uncheckedColor = Color.Gray,
                        checkmarkColor = DarkBackground
                    )
                )
            }
        }
    }
}
