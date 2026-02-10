package com.example.zk.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.Lock
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
private val AccentGreen = Color(0xFF4CAF50)

enum class ChangePinStepUI {
    CURRENT,
    NEW,
    CONFIRM
}

/**
 * ChangePinScreen - Change the 6-digit wallet unlock PIN
 * Flow: Verify current PIN -> Enter new PIN -> Confirm new PIN
 *
 * This changes the LOCAL wallet PIN - no cloud password change.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangePinScreen(
    onBackClick: () -> Unit = {},
    onPinChanged: () -> Unit = {},
    // ViewModel integration - set to non-null to use external state
    externalCurrentStep: ChangePinStepUI? = null,
    externalPinEntry: String? = null,
    errorMessage: String? = null,
    isLoading: Boolean = false,
    pinChanged: Boolean = false,
    onDigitClick: ((String) -> Unit)? = null,
    onDeleteClick: (() -> Unit)? = null
) {
    // Internal state for standalone usage
    var internalStep by remember { mutableStateOf(ChangePinStepUI.CURRENT) }
    var internalCurrentPin by remember { mutableStateOf("") }
    var internalNewPin by remember { mutableStateOf("") }
    var internalConfirmPin by remember { mutableStateOf("") }
    var internalError by remember { mutableStateOf<String?>(null) }
    var internalPinChanged by remember { mutableStateOf(false) }

    // Determine which state to use
    val useExternalState = onDigitClick != null

    val step = if (useExternalState) externalCurrentStep ?: ChangePinStepUI.CURRENT else internalStep
    val pinEntry = if (useExternalState) {
        externalPinEntry ?: ""
    } else {
        when (internalStep) {
            ChangePinStepUI.CURRENT -> internalCurrentPin
            ChangePinStepUI.NEW -> internalNewPin
            ChangePinStepUI.CONFIRM -> internalConfirmPin
        }
    }
    val error = errorMessage ?: internalError
    val changed = pinChanged || internalPinChanged

    fun handleDigitClick(digit: String) {
        if (useExternalState) {
            onDigitClick?.invoke(digit)
        } else {
            internalError = null
            when (internalStep) {
                ChangePinStepUI.CURRENT -> {
                    if (internalCurrentPin.length < 6) {
                        internalCurrentPin += digit
                        if (internalCurrentPin.length == 6) {
                            // Mock verification - in real app, verify against stored hash
                            internalStep = ChangePinStepUI.NEW
                        }
                    }
                }
                ChangePinStepUI.NEW -> {
                    if (internalNewPin.length < 6) {
                        internalNewPin += digit
                        if (internalNewPin.length == 6) {
                            internalStep = ChangePinStepUI.CONFIRM
                        }
                    }
                }
                ChangePinStepUI.CONFIRM -> {
                    if (internalConfirmPin.length < 6) {
                        internalConfirmPin += digit
                        if (internalConfirmPin.length == 6) {
                            if (internalConfirmPin == internalNewPin) {
                                internalPinChanged = true
                            } else {
                                internalError = "PINs don't match. Try again."
                                internalConfirmPin = ""
                            }
                        }
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
            when (internalStep) {
                ChangePinStepUI.CURRENT -> {
                    if (internalCurrentPin.isNotEmpty()) {
                        internalCurrentPin = internalCurrentPin.dropLast(1)
                    }
                }
                ChangePinStepUI.NEW -> {
                    if (internalNewPin.isNotEmpty()) {
                        internalNewPin = internalNewPin.dropLast(1)
                    } else {
                        internalStep = ChangePinStepUI.CURRENT
                    }
                }
                ChangePinStepUI.CONFIRM -> {
                    if (internalConfirmPin.isNotEmpty()) {
                        internalConfirmPin = internalConfirmPin.dropLast(1)
                    } else {
                        internalStep = ChangePinStepUI.NEW
                    }
                }
            }
        }
    }

    // Auto-navigate when PIN changed
    LaunchedEffect(changed) {
        if (changed) {
            kotlinx.coroutines.delay(1500)
            onPinChanged()
        }
    }

    Scaffold(
        containerColor = DarkBackground,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Change PIN",
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
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkBackground
                )
            )
        }
    ) { paddingValues ->
        if (changed) {
            // Success state
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    Icons.Filled.CheckCircle,
                    contentDescription = "Success",
                    tint = AccentGreen,
                    modifier = Modifier.size(80.dp)
                )
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "PIN Changed Successfully",
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Your wallet PIN has been updated.",
                    color = Color.Gray,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(20.dp))

                // Step indicator
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StepIndicator(
                        stepNumber = 1,
                        label = "Current",
                        isActive = step == ChangePinStepUI.CURRENT,
                        isComplete = step != ChangePinStepUI.CURRENT
                    )
                    StepIndicator(
                        stepNumber = 2,
                        label = "New",
                        isActive = step == ChangePinStepUI.NEW,
                        isComplete = step == ChangePinStepUI.CONFIRM
                    )
                    StepIndicator(
                        stepNumber = 3,
                        label = "Confirm",
                        isActive = step == ChangePinStepUI.CONFIRM,
                        isComplete = false
                    )
                }

                Spacer(modifier = Modifier.height(40.dp))

                // Lock icon
                Icon(
                    Icons.Outlined.Lock,
                    contentDescription = null,
                    tint = AccentCyan,
                    modifier = Modifier.size(48.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Title
                Text(
                    text = when (step) {
                        ChangePinStepUI.CURRENT -> "Enter Current PIN"
                        ChangePinStepUI.NEW -> "Enter New PIN"
                        ChangePinStepUI.CONFIRM -> "Confirm New PIN"
                    },
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Subtitle
                Text(
                    text = when (step) {
                        ChangePinStepUI.CURRENT -> "Enter your current 6-digit PIN"
                        ChangePinStepUI.NEW -> "Choose a new 6-digit PIN"
                        ChangePinStepUI.CONFIRM -> "Re-enter your new PIN"
                    },
                    color = Color.Gray,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center
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
                                    if (index < pinEntry.length) AccentCyan
                                    else Color.White.copy(alpha = 0.3f)
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
                    Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                        ChangePinNumberButton("1") { handleDigitClick("1") }
                        ChangePinNumberButton("2") { handleDigitClick("2") }
                        ChangePinNumberButton("3") { handleDigitClick("3") }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                        ChangePinNumberButton("4") { handleDigitClick("4") }
                        ChangePinNumberButton("5") { handleDigitClick("5") }
                        ChangePinNumberButton("6") { handleDigitClick("6") }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                        ChangePinNumberButton("7") { handleDigitClick("7") }
                        ChangePinNumberButton("8") { handleDigitClick("8") }
                        ChangePinNumberButton("9") { handleDigitClick("9") }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                        Box(modifier = Modifier.size(72.dp))
                        ChangePinNumberButton("0") { handleDigitClick("0") }
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

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun RowScope.StepIndicator(
    stepNumber: Int,
    label: String,
    isActive: Boolean,
    isComplete: Boolean
) {
    Column(
        modifier = Modifier.weight(1f),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(
                    when {
                        isComplete -> AccentGreen
                        isActive -> AccentCyan
                        else -> CardBackground
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isComplete) {
                Icon(
                    Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            } else {
                Text(
                    text = stepNumber.toString(),
                    color = if (isActive) DarkBackground else Color.Gray,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            color = if (isActive || isComplete) Color.White else Color.Gray,
            fontSize = 12.sp
        )
    }
}

@Composable
private fun ChangePinNumberButton(
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
fun ChangePinScreenPreview() {
    ZKTheme {
        ChangePinScreen()
    }
}
