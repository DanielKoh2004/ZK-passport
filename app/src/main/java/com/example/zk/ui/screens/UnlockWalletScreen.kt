package com.example.zk.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
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

private val DarkBackground = Color(0xFF0D1421)
private val CardBackground = Color(0xFF1A2332)
private val AccentCyan = Color(0xFF00D9FF)

/**
 * UnlockWalletScreen - Login screen for existing wallet
 * Enter 6-digit PIN to unlock local wallet
 *
 * This is LOCAL wallet unlock - no web password, no cloud auth.
 * PIN is verified against stored salted hash.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnlockWalletScreen(
    onBackClick: () -> Unit = {},
    onUnlockSuccess: () -> Unit = {},
    externalPin: String? = null,
    errorMessage: String? = null,
    isLoading: Boolean = false,
    onDigitClick: ((String) -> Unit)? = null,
    onDeleteClick: (() -> Unit)? = null,
    isBiometricEnabled: Boolean = false,
    isBiometricAvailable: Boolean = false,
    onBiometricClick: (() -> Unit)? = null
) {
    // Internal state for standalone usage
    var internalPin by remember { mutableStateOf("") }

    // Determine which state to use
    val useExternalState = onDigitClick != null
    val currentPin = if (useExternalState) externalPin ?: "" else internalPin

    fun handleDigitClick(digit: String) {
        if (useExternalState) {
            onDigitClick?.invoke(digit)
        } else {
            if (internalPin.length < 6) {
                internalPin += digit
                if (internalPin.length == 6) {
                    // Auto-unlock when using internal state
                    onUnlockSuccess()
                }
            }
        }
    }

    fun handleDeleteClick() {
        if (useExternalState) {
            onDeleteClick?.invoke()
        } else {
            if (internalPin.isNotEmpty()) {
                internalPin = internalPin.dropLast(1)
            }
        }
    }

    Scaffold(
        containerColor = DarkBackground,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Unlock Wallet",
                        color = Color.White,
                        fontWeight = FontWeight.Medium
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    Icon(
                        Icons.Filled.Lock,
                        contentDescription = "Secure",
                        tint = AccentCyan,
                        modifier = Modifier.padding(end = 16.dp)
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkBackground
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(40.dp))

            // Lock icon
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(CardBackground, AccentCyan.copy(alpha = 0.2f))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.Lock,
                    contentDescription = null,
                    tint = AccentCyan,
                    modifier = Modifier.size(36.dp)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Title
            Text(
                text = "Enter Your PIN",
                color = Color.White,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Enter your 6-digit PIN to unlock",
                color = Color.Gray,
                fontSize = 14.sp
            )

            Spacer(modifier = Modifier.height(40.dp))

            // PIN Dots
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                repeat(6) { index ->
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(
                                if (index < currentPin.length) AccentCyan
                                else Color.White.copy(alpha = 0.3f)
                            )
                    )
                }
            }

            // Error message
            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = errorMessage,
                    color = Color(0xFFFF5252),
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center
                )
            }

            // Loading indicator
            if (isLoading) {
                Spacer(modifier = Modifier.height(16.dp))
                CircularProgressIndicator(
                    color = AccentCyan,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // Number Pad
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Row 1: 1, 2, 3
                Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    UnlockNumberButton("1") { handleDigitClick("1") }
                    UnlockNumberButton("2") { handleDigitClick("2") }
                    UnlockNumberButton("3") { handleDigitClick("3") }
                }
                // Row 2: 4, 5, 6
                Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    UnlockNumberButton("4") { handleDigitClick("4") }
                    UnlockNumberButton("5") { handleDigitClick("5") }
                    UnlockNumberButton("6") { handleDigitClick("6") }
                }
                // Row 3: 7, 8, 9
                Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    UnlockNumberButton("7") { handleDigitClick("7") }
                    UnlockNumberButton("8") { handleDigitClick("8") }
                    UnlockNumberButton("9") { handleDigitClick("9") }
                }
                // Row 4: biometric/empty, 0, delete
                Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    // Biometric button (only show if enabled and available)
                    if (isBiometricEnabled && isBiometricAvailable && onBiometricClick != null) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(AccentCyan.copy(alpha = 0.2f))
                                .clickable { onBiometricClick() },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "👆",
                                fontSize = 28.sp
                            )
                        }
                    } else {
                        Box(modifier = Modifier.size(72.dp))
                    }
                    UnlockNumberButton("0") { handleDigitClick("0") }
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .clickable { handleDeleteClick() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "⌫",
                            color = Color.White,
                            fontSize = 24.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

            // Footer Text
            Text(
                text = "No personal data stored centrally",
                color = Color.Gray,
                fontSize = 12.sp
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun UnlockNumberButton(
    number: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(72.dp)
            .clip(CircleShape)
            .background(CardBackground)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = number,
            color = Color.White,
            fontSize = 28.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Preview(showBackground = true)
@Composable
fun UnlockWalletScreenPreview() {
    ZKTheme {
        UnlockWalletScreen()
    }
}
