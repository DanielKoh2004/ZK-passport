package com.example.zk.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.AccountBox
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.zk.ui.theme.ZKTheme
import kotlinx.coroutines.delay

private val DarkBackground = Color(0xFF0D1421)
private val CardBackground = Color(0xFF1A2332)
private val AccentCyan = Color(0xFF00D9FF)
private val AccentGreen = Color(0xFF4CAF50)

/**
 * EnrollmentScreen - Step 2 of wallet creation
 * Simulates passport enrollment (NFC read / OCR / manual entry)
 *
 * IMPORTANT: Passport data is processed LOCALLY only.
 * Raw passport data is NOT stored in history or sent to blockchain.
 * Only derived credentials (commitments, hashes) are stored.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnrollmentScreen(
    onBack: () -> Unit = {},
    onEnrollmentComplete: () -> Unit = {},
    onSkip: () -> Unit = {},
    onScanPassport: (() -> Unit)? = null, // Navigate to actual scan passport screen
    isLoading: Boolean = false,
    enrollmentComplete: Boolean = false,
    onStartEnrollment: (() -> Unit)? = null
) {
    var internalLoading by remember { mutableStateOf(false) }
    var internalComplete by remember { mutableStateOf(false) }

    val loading = isLoading || internalLoading
    val complete = enrollmentComplete || internalComplete

    // Start enrollment - either navigate to scan or simulate
    fun startEnrollment() {
        if (onScanPassport != null) {
            // Navigate to actual passport scan screen
            onScanPassport()
        } else if (onStartEnrollment != null) {
            // Use external enrollment logic
            onStartEnrollment()
        } else {
            // Use internal simulation
            internalLoading = true
        }
    }

    // Auto-complete simulation
    LaunchedEffect(internalLoading) {
        if (internalLoading) {
            delay(2000)
            internalLoading = false
            internalComplete = true
        }
    }

    // Auto-navigate when complete
    LaunchedEffect(complete) {
        if (complete) {
            delay(500)
            onEnrollmentComplete()
        }
    }

    Scaffold(
        containerColor = DarkBackground,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Enroll Passport",
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
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
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Step indicator
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Step 2 of 3",
                    color = Color.White,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(8.dp))

                // Progress bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(AccentCyan)
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(AccentCyan)
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(Color.Gray.copy(alpha = 0.3f))
                    )
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

            // Passport illustration
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(CardBackground, AccentCyan.copy(alpha = 0.3f))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (loading) {
                    CircularProgressIndicator(
                        color = AccentCyan,
                        strokeWidth = 3.dp,
                        modifier = Modifier.size(60.dp)
                    )
                } else if (complete) {
                    Icon(
                        Icons.Outlined.CheckCircle,
                        contentDescription = "Complete",
                        tint = AccentGreen,
                        modifier = Modifier.size(60.dp)
                    )
                } else {
                    Icon(
                        Icons.Outlined.AccountBox,
                        contentDescription = "Passport",
                        tint = AccentCyan,
                        modifier = Modifier.size(60.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Title
            Text(
                text = when {
                    complete -> "Enrollment Complete!"
                    loading -> "Processing Passport..."
                    else -> "Enroll Your Passport"
                },
                color = Color.White,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Description
            Text(
                text = when {
                    complete -> "Your credential has been created locally.\nNo data was uploaded."
                    loading -> "Extracting data and creating credential.\nThis is processed locally on your device."
                    else -> "Scan your passport to create a local credential.\nYour data stays on your device."
                },
                color = Color.Gray,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                lineHeight = 22.sp
            )

            Spacer(modifier = Modifier.height(40.dp))

            // Privacy notice card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CardBackground)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Outlined.CheckCircle,
                            contentDescription = null,
                            tint = AccentGreen,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Privacy Protected",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "• Passport data processed locally only\n• Raw data is NOT stored or uploaded\n• Only cryptographic credentials saved\n• No blockchain storage of personal data",
                        color = Color.Gray,
                        fontSize = 13.sp,
                        lineHeight = 20.sp
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Scan button
            if (!loading && !complete) {
                Button(
                    onClick = { startEnrollment() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(28.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentCyan)
                ) {
                    Icon(
                        Icons.Outlined.AccountBox,
                        contentDescription = null,
                        tint = DarkBackground,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (onScanPassport != null) "Scan Passport" else "Scan Passport (Simulated)",
                        color = DarkBackground,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Skip for testing
                TextButton(onClick = onSkip) {
                    Text(
                        text = "Skip for Testing",
                        color = Color.Gray,
                        fontSize = 14.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Preview(showBackground = true)
@Composable
fun EnrollmentScreenPreview() {
    ZKTheme {
        EnrollmentScreen()
    }
}
