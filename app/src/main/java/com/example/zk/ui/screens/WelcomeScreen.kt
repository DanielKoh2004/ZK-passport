package com.example.zk.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
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

@Composable
fun WelcomeScreen(
    onCreateWallet: () -> Unit = {},
    onLogin: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(60.dp))

        // Shield icon
        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape)
                .background(CardBackground),
            contentAlignment = Alignment.Center
        ) {
            Text("🛡️", fontSize = 48.sp)
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Title
        Text(
            text = "Privacy-preserving\ndigital passport wallet",
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            textAlign = TextAlign.Center,
            lineHeight = 34.sp
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Subtitle
        Text(
            text = "Secure identity verification at your\nfingertips. No cloud account needed.\nYour data stays on your device.",
            fontSize = 14.sp,
            color = Color.Gray,
            textAlign = TextAlign.Center,
            lineHeight = 22.sp
        )

        Spacer(modifier = Modifier.height(40.dp))

        // Feature cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ID Card illustration
            Box(
                modifier = Modifier
                    .weight(1f)
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(CardBackground),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color.Gray.copy(alpha = 0.5f))
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    repeat(3) {
                        Box(
                            modifier = Modifier
                                .width(60.dp)
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(Color.Gray.copy(alpha = 0.5f))
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                }
            }

            // Network illustration
            Box(
                modifier = Modifier
                    .weight(1f)
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(CardBackground),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .clip(CircleShape)
                        .background(Color.Gray.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("🔗", fontSize = 24.sp)
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Create Wallet Button
        Button(
            onClick = onCreateWallet,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(28.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = AccentCyan
            )
        ) {
            Text(
                text = "Create Wallet",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = DarkBackground
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Log in Button
        OutlinedButton(
            onClick = onLogin,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(28.dp),
            border = BorderStroke(1.dp, Color.Gray.copy(alpha = 0.5f))
        ) {
            Text(
                text = "Log in",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Terms text
        Text(
            text = "By continuing, you agree to our Terms of Service and Privacy Policy.",
            fontSize = 12.sp,
            color = Color.Gray,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Preview(showBackground = true)
@Composable
fun WelcomeScreenPreview() {
    ZKTheme {
        WelcomeScreen()
    }
}
