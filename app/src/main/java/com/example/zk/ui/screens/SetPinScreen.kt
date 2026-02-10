package com.example.zk.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
 * SetPinScreen - Step 1 of wallet creation
 * User sets a 6-digit numeric PIN (local wallet unlock PIN, NOT a web password)
 * PIN is stored as salted hash locally
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetPinScreen(
    onBack: () -> Unit = {},
    onPinSet: (String) -> Unit = {},
    // ViewModel integration params - set to null to use internal state
    externalPin: String? = null,
    externalConfirmPin: String? = null,
    externalIsConfirming: Boolean? = null,
    errorMessage: String? = null,
    onDigitClick: ((String) -> Unit)? = null,
    onDeleteClick: (() -> Unit)? = null
) {
    // Use internal state if no ViewModel integration
    var internalPin by remember { mutableStateOf("") }
    var internalConfirmPin by remember { mutableStateOf("") }
    var internalIsConfirming by remember { mutableStateOf(false) }
    var internalError by remember { mutableStateOf<String?>(null) }

    // Determine which state to use
    val useExternalState = onDigitClick != null

    val currentPin = if (useExternalState) {
        if (externalIsConfirming == true) externalConfirmPin ?: "" else externalPin ?: ""
    } else {
        if (internalIsConfirming) internalConfirmPin else internalPin
    }

    val isConfirmingState = if (useExternalState) externalIsConfirming ?: false else internalIsConfirming
    val error = errorMessage ?: internalError

    val maxPinLength = 6

    fun handleDigitClick(digit: String) {
        if (useExternalState) {
            onDigitClick?.invoke(digit)
        } else {
            // Internal state handling
            internalError = null
            if (internalIsConfirming) {
                if (internalConfirmPin.length < 6) {
                    internalConfirmPin += digit
                    if (internalConfirmPin.length == 6) {
                        if (internalConfirmPin == internalPin) {
                            onPinSet(internalPin)
                        } else {
                            internalError = "PINs don't match. Try again."
                            internalConfirmPin = ""
                        }
                    }
                }
            } else {
                if (internalPin.length < 6) {
                    internalPin += digit
                    if (internalPin.length == 6) {
                        internalIsConfirming = true
                    }
                }
            }
        }
    }

    fun handleDeleteClick() {
        if (useExternalState) {
            onDeleteClick?.invoke()
        } else {
            internalError = null
            if (internalIsConfirming) {
                if (internalConfirmPin.isNotEmpty()) {
                    internalConfirmPin = internalConfirmPin.dropLast(1)
                } else {
                    internalIsConfirming = false
                }
            } else {
                if (internalPin.isNotEmpty()) {
                    internalPin = internalPin.dropLast(1)
                }
            }
            internalError = null
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Top Bar
            TopAppBar(
                title = {
                    Text(
                        text = "Create Wallet",
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

            // Step indicator
            Column(
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                Text(
                    text = "Step 1 of 3",
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
                            .background(Color.Gray.copy(alpha = 0.3f))
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

            // Title
            Text(
                text = if (isConfirmingState) "Confirm Your PIN" else "Set a 6-digit Wallet PIN",
                color = Color.White,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Subtitle
            Text(
                text = if (isConfirmingState)
                    "Re-enter your PIN to confirm"
                else
                    "This PIN unlocks your local wallet.\nNo account or cloud storage.",
                color = Color(0xFF9CA3AF),
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
                lineHeight = 22.sp
            )

            Spacer(modifier = Modifier.height(40.dp))

            // PIN dots
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(maxPinLength) { index ->
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 8.dp)
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(
                                if (index < currentPin.length) AccentCyan
                                else Color(0xFF4A5568)
                            )
                    )
                }
            }

            // Error message
            if (error != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = error,
                    color = Color(0xFFFF5252),
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // Number pad
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 48.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Row 1: 1 2 3
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    NumberKey("1") { handleDigitClick("1") }
                    NumberKey("2") { handleDigitClick("2") }
                    NumberKey("3") { handleDigitClick("3") }
                }

                // Row 2: 4 5 6
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    NumberKey("4") { handleDigitClick("4") }
                    NumberKey("5") { handleDigitClick("5") }
                    NumberKey("6") { handleDigitClick("6") }
                }

                // Row 3: 7 8 9
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    NumberKey("7") { handleDigitClick("7") }
                    NumberKey("8") { handleDigitClick("8") }
                    NumberKey("9") { handleDigitClick("9") }
                }

                // Row 4: empty 0 backspace
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Empty space
                    Box(modifier = Modifier.size(72.dp))

                    NumberKey("0") { handleDigitClick("0") }

                    // Backspace
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .clickable { handleDeleteClick() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "⌫",
                            fontSize = 28.sp,
                            color = Color.White
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Privacy notice
            Text(
                text = "No personal data stored centrally",
                color = Color.Gray,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun NumberKey(
    number: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(72.dp)
            .clip(CircleShape)
            .background(CardBackground)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = number,
            fontSize = 32.sp,
            fontWeight = FontWeight.Medium,
            color = Color.White
        )
    }
}

@Preview(showBackground = true)
@Composable
fun SetPinScreenPreview() {
    ZKTheme {
        SetPinScreen()
    }
}



