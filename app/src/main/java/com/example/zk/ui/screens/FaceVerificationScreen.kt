package com.example.zk.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.zk.ui.theme.ZKTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FaceVerificationScreen(
    onBack: () -> Unit = {},
    onCapture: () -> Unit = {},
    onHelp: () -> Unit = {},
    onSwitch: () -> Unit = {}
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1A1F2E))
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Top Bar
            TopAppBar(
                title = {
                    Text(
                        text = "Verify Identity",
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

            // Step indicator with encryption badge
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Step 2 of 3",
                    color = Color.White,
                    fontSize = 14.sp
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text("🔒", fontSize = 14.sp)
                    Text(
                        text = "End-to-end encrypted",
                        color = Color(0xFF4ADE80),
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Progress bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color(0xFF4ADE80))
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color(0xFF4ADE80))
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color(0xFF4A5568))
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Title
            Text(
                text = "Face Verification",
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Subtitle
            Text(
                text = "Place your face within the frame and\nfollow the on-screen prompts to confirm\nyour identity.",
                color = Color(0xFF9CA3AF),
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp),
                lineHeight = 22.sp
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Camera area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                // Card background
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(0.75f)
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color(0xFF2D3748))
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Spacer(modifier = Modifier.height(16.dp))

                        // Camera Active badge
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0xFF374151))
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF4ADE80))
                                )
                                Text(
                                    text = "Camera Active",
                                    color = Color.White,
                                    fontSize = 12.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Oval face frame
                        Box(
                            modifier = Modifier
                                .width(200.dp)
                                .height(260.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            // Oval border with gradient
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val brush = Brush.linearGradient(
                                    colors = listOf(
                                        Color(0xFF3B82F6),
                                        Color(0xFF8B5CF6)
                                    ),
                                    start = Offset(0f, 0f),
                                    end = Offset(size.width, size.height)
                                )
                                drawOval(
                                    brush = brush,
                                    topLeft = Offset(0f, 0f),
                                    size = size,
                                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 4.dp.toPx())
                                )
                            }

                            // Placeholder silhouette
                            Box(
                                modifier = Modifier
                                    .width(180.dp)
                                    .height(240.dp)
                                    .clip(androidx.compose.foundation.shape.GenericShape { size, _ ->
                                        val width = size.width
                                        val height = size.height
                                        moveTo(width / 2, 0f)
                                        cubicTo(width, 0f, width, height * 0.6f, width / 2, height)
                                        cubicTo(0f, height * 0.6f, 0f, 0f, width / 2, 0f)
                                    })
                                    .background(Color(0x30FFFFFF)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("👤", fontSize = 64.sp, color = Color(0x60FFFFFF))
                            }

                            // Guide dots
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopCenter)
                                    .offset(y = (-8).dp)
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF3B82F6))
                            )
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .offset(y = 8.dp)
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF3B82F6))
                            )
                            Box(
                                modifier = Modifier
                                    .align(Alignment.CenterStart)
                                    .offset(x = (-8).dp)
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF3B82F6))
                            )
                            Box(
                                modifier = Modifier
                                    .align(Alignment.CenterEnd)
                                    .offset(x = 8.dp)
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF3B82F6))
                            )
                        }
                    }
                }
            }

            // Tip text
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF3B82F6))
                    .padding(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                    )
                    Text(
                        text = "Make sure your environment is well-lit",
                        color = Color.White,
                        fontSize = 14.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Bottom controls
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 48.dp, vertical = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Help button
                IconButton(
                    onClick = onHelp,
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF374151))
                ) {
                    Text("❓", fontSize = 24.sp)
                }

                // Capture button
                Button(
                    onClick = onCapture,
                    modifier = Modifier.size(80.dp),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF3B82F6)
                    ),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text("📷", fontSize = 32.sp)
                }

                // Switch camera button
                IconButton(
                    onClick = onSwitch,
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF374151))
                ) {
                    Text("🔄", fontSize = 24.sp)
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun FaceVerificationScreenPreview() {
    ZKTheme {
        FaceVerificationScreen()
    }
}
