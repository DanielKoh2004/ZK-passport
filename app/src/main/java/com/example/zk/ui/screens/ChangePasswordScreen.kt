package com.example.zk.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.zk.ui.theme.ZKTheme

private val DarkBackground = Color(0xFF0D1421)
private val CardBackground = Color(0xFF1A2332)
private val AccentCyan = Color(0xFF00D9FF)

enum class PinStep {
    CURRENT,
    NEW,
    CONFIRM
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangePasswordScreen(
    onBackClick: () -> Unit = {},
    onUpdatePassword: (String, String) -> Unit = { _, _ -> },
    onNavigateToHome: () -> Unit = {},
    onNavigateToActivity: () -> Unit = {},
    onNavigateToGenerateProof: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {}
) {
    var currentPin by remember { mutableStateOf("") }
    var newPin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var currentStep by remember { mutableStateOf(PinStep.CURRENT) }
    var showError by remember { mutableStateOf(false) }

    val currentPinEntry = when (currentStep) {
        PinStep.CURRENT -> currentPin
        PinStep.NEW -> newPin
        PinStep.CONFIRM -> confirmPin
    }

    fun onNumberClick(number: String) {
        showError = false
        when (currentStep) {
            PinStep.CURRENT -> {
                if (currentPin.length < 6) {
                    currentPin += number
                    if (currentPin.length == 6) {
                        currentStep = PinStep.NEW
                    }
                }
            }
            PinStep.NEW -> {
                if (newPin.length < 6) {
                    newPin += number
                    if (newPin.length == 6) {
                        currentStep = PinStep.CONFIRM
                    }
                }
            }
            PinStep.CONFIRM -> {
                if (confirmPin.length < 6) {
                    confirmPin += number
                    if (confirmPin.length == 6) {
                        if (confirmPin == newPin) {
                            onUpdatePassword(currentPin, newPin)
                        } else {
                            showError = true
                            confirmPin = ""
                        }
                    }
                }
            }
        }
    }

    fun onDeleteClick() {
        showError = false
        when (currentStep) {
            PinStep.CURRENT -> {
                if (currentPin.isNotEmpty()) {
                    currentPin = currentPin.dropLast(1)
                }
            }
            PinStep.NEW -> {
                if (newPin.isNotEmpty()) {
                    newPin = newPin.dropLast(1)
                } else {
                    currentStep = PinStep.CURRENT
                }
            }
            PinStep.CONFIRM -> {
                if (confirmPin.isNotEmpty()) {
                    confirmPin = confirmPin.dropLast(1)
                } else {
                    currentStep = PinStep.NEW
                }
            }
        }
    }

    Scaffold(
        containerColor = DarkBackground,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Change Password",
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
        },
        bottomBar = {
            ChangePasswordBottomNavigationBar(
                onNavigateToHome = onNavigateToHome,
                onNavigateToActivity = onNavigateToActivity,
                onNavigateToGenerateProof = onNavigateToGenerateProof,
                onNavigateToSettings = onNavigateToSettings
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(40.dp))

            // Step Title
            Text(
                text = when (currentStep) {
                    PinStep.CURRENT -> "Enter Current PIN"
                    PinStep.NEW -> "Enter New PIN"
                    PinStep.CONFIRM -> "Confirm New PIN"
                },
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Step Subtitle
            Text(
                text = when (currentStep) {
                    PinStep.CURRENT -> "Enter your current 6-digit PIN"
                    PinStep.NEW -> "Create a new 6-digit PIN"
                    PinStep.CONFIRM -> "Re-enter your new PIN to confirm"
                },
                color = Color.Gray,
                fontSize = 14.sp
            )

            Spacer(modifier = Modifier.height(40.dp))

            // PIN Dots
            PinDots(filledCount = currentPinEntry.length)

            Spacer(modifier = Modifier.height(24.dp))

            // Error Message
            if (showError) {
                Text(
                    text = "PINs don't match. Please try again.",
                    color = Color(0xFFFF5252),
                    fontSize = 14.sp
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // Number Pad
            NumberPad(
                onNumberClick = { onNumberClick(it) },
                onDeleteClick = { onDeleteClick() }
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun PinDots(filledCount: Int) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        repeat(6) { index ->
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(
                        if (index < filledCount) Color.White
                        else Color.White.copy(alpha = 0.3f)
                    )
            )
        }
    }
}

@Composable
private fun NumberPad(
    onNumberClick: (String) -> Unit,
    onDeleteClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Row 1: 1, 2, 3
        Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            NumberButton("1", onClick = { onNumberClick("1") })
            NumberButton("2", onClick = { onNumberClick("2") })
            NumberButton("3", onClick = { onNumberClick("3") })
        }
        // Row 2: 4, 5, 6
        Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            NumberButton("4", onClick = { onNumberClick("4") })
            NumberButton("5", onClick = { onNumberClick("5") })
            NumberButton("6", onClick = { onNumberClick("6") })
        }
        // Row 3: 7, 8, 9
        Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            NumberButton("7", onClick = { onNumberClick("7") })
            NumberButton("8", onClick = { onNumberClick("8") })
            NumberButton("9", onClick = { onNumberClick("9") })
        }
        // Row 4: empty, 0, delete
        Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            Box(modifier = Modifier.size(72.dp))
            NumberButton("0", onClick = { onNumberClick("0") })
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .clickable { onDeleteClick() },
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
}

@Composable
private fun NumberButton(
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

@Composable
private fun ChangePasswordBottomNavigationBar(
    onNavigateToHome: () -> Unit = {},
    onNavigateToActivity: () -> Unit = {},
    onNavigateToGenerateProof: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {}
) {
    NavigationBar(
        containerColor = CardBackground,
        contentColor = Color.White
    ) {
        NavigationBarItem(
            selected = false,
            onClick = onNavigateToHome,
            icon = {
                Icon(
                    Icons.Filled.Home,
                    contentDescription = "Home",
                    modifier = Modifier.size(24.dp)
                )
            },
            label = { Text("Home", fontSize = 12.sp) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = AccentCyan,
                selectedTextColor = AccentCyan,
                unselectedIconColor = Color.Gray,
                unselectedTextColor = Color.Gray,
                indicatorColor = Color.Transparent
            )
        )
        NavigationBarItem(
            selected = false,
            onClick = onNavigateToActivity,
            icon = {
                Icon(
                    Icons.Outlined.Refresh,
                    contentDescription = "Activity",
                    modifier = Modifier.size(24.dp)
                )
            },
            label = { Text("Activity", fontSize = 12.sp) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = AccentCyan,
                selectedTextColor = AccentCyan,
                unselectedIconColor = Color.Gray,
                unselectedTextColor = Color.Gray,
                indicatorColor = Color.Transparent
            )
        )
        NavigationBarItem(
            selected = false,
            onClick = onNavigateToGenerateProof,
            icon = {
                Icon(
                    Icons.Outlined.CheckCircle,
                    contentDescription = "Generate Proof",
                    modifier = Modifier.size(24.dp)
                )
            },
            label = { Text("Generate Proof", fontSize = 11.sp) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = AccentCyan,
                selectedTextColor = AccentCyan,
                unselectedIconColor = Color.Gray,
                unselectedTextColor = Color.Gray,
                indicatorColor = Color.Transparent
            )
        )
        NavigationBarItem(
            selected = false,
            onClick = onNavigateToSettings,
            icon = {
                Icon(
                    Icons.Outlined.Settings,
                    contentDescription = "Settings",
                    modifier = Modifier.size(24.dp)
                )
            },
            label = { Text("Settings", fontSize = 12.sp) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = AccentCyan,
                selectedTextColor = AccentCyan,
                unselectedIconColor = Color.Gray,
                unselectedTextColor = Color.Gray,
                indicatorColor = Color.Transparent
            )
        )
    }
}

@Preview(showBackground = true)
@Composable
fun ChangePasswordScreenPreview() {
    ZKTheme {
        ChangePasswordScreen()
    }
}
