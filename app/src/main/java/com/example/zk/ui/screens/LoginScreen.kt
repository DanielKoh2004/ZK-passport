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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.zk.ui.theme.ZKTheme

private val DarkBackground = Color(0xFF0D1421)
private val CardBackground = Color(0xFF1A2332)
private val AccentCyan = Color(0xFF00D9FF)

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit = {},
    onBackClick: () -> Unit = {}
) {
    var passcode by remember { mutableStateOf("") }

    fun onNumberClick(number: String) {
        if (passcode.length < 6) {
            passcode += number

            // Auto-login when 6 digits entered
            if (passcode.length == 6) {
                onLoginSuccess()
            }
        }
    }

    fun onDeleteClick() {
        if (passcode.isNotEmpty()) {
            passcode = passcode.dropLast(1)
        }
    }

    Scaffold(
        containerColor = DarkBackground,
        topBar = {
            LoginTopBar(onBackClick = onBackClick)
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(60.dp))

            // Title
            Text(
                text = "Enter Passcode",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(40.dp))

            // Passcode Dots
            PasscodeDots(filledCount = passcode.length)


            Spacer(modifier = Modifier.weight(1f))

            // Number Pad
            NumberPad(
                onNumberClick = { onNumberClick(it) },
                onDeleteClick = { onDeleteClick() }
            )

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LoginTopBar(onBackClick: () -> Unit) {
    TopAppBar(
        title = {
            Text(
                text = "Secure Login",
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
            IconButton(onClick = { }) {
                Icon(
                    Icons.Filled.Lock,
                    contentDescription = "Secure",
                    tint = Color.White
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = DarkBackground
        )
    )
}

@Composable
private fun PasscodeDots(filledCount: Int) {
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
            // Empty space
            Box(modifier = Modifier.size(72.dp))
            NumberButton("0", onClick = { onNumberClick("0") })
            // Delete button
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

@Preview(showBackground = true)
@Composable
fun LoginScreenPreview() {
    ZKTheme {
        LoginScreen()
    }
}
