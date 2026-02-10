package com.example.zk.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.example.zk.R
import com.example.zk.ui.theme.ZKTheme

private val DarkBackground = Color(0xFF0D1421)
private val CardBackground = Color(0xFF1A2332)
private val AccentCyan = Color(0xFF00D9FF)
private val AccentGreen = Color(0xFF4CAF50)

@Composable
fun HomeScreen(
    onNavigateToActivity: () -> Unit = {},
    onNavigateToGenerateProof: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onNavigateToProfile: () -> Unit = {},
    userName: String = "User",
    hasCredential: Boolean = false
) {
    // Get first name for greeting
    val firstName = userName.split(" ").firstOrNull() ?: "User"

    Scaffold(
        containerColor = DarkBackground,
        bottomBar = {
            BottomNavigationBar(
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
                .padding(horizontal = 20.dp)
        ) {
            // Top Bar
            TopBar()

            Spacer(modifier = Modifier.height(24.dp))

            // Greeting
            Text(
                text = stringResource(R.string.hello_user, firstName),
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Credential Status
            CredentialBadge(hasCredential = hasCredential)

            Spacer(modifier = Modifier.height(20.dp))

            // Hero Image Card
            HeroImageCard()

            Spacer(modifier = Modifier.height(20.dp))

            // Generate Proof Card
            GenerateProofCard(onStartGeneration = onNavigateToGenerateProof)

            Spacer(modifier = Modifier.height(20.dp))

            // Quick Actions
            QuickActionsRow(onProfileClick = onNavigateToProfile)
        }
    }
}

@Composable
private fun TopBar() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(AccentCyan, Color(0xFF0088AA))
                        )
                    )
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = stringResource(R.string.identity),
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium
            )
        }
        IconButton(onClick = { }) {
            Icon(
                Icons.Outlined.Notifications,
                contentDescription = stringResource(R.string.notifications),
                tint = Color.White
            )
        }
    }
}

@Composable
private fun CredentialBadge(hasCredential: Boolean = false) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = if (hasCredential) AccentGreen.copy(alpha = 0.2f) else Color.Gray.copy(alpha = 0.2f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                if (hasCredential) Icons.Filled.CheckCircle else Icons.Outlined.Info,
                contentDescription = null,
                tint = if (hasCredential) AccentGreen else Color.Gray,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = if (hasCredential) stringResource(R.string.credential_active) else stringResource(R.string.credential_not_added),
                color = if (hasCredential) AccentGreen else Color.Gray,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun HeroImageCard() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        listOf(Color(0xFF1A2332), AccentCyan.copy(alpha = 0.3f))
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            // Circuit board pattern placeholder
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .background(
                        Brush.radialGradient(
                            listOf(Color.White.copy(alpha = 0.8f), Color.Transparent)
                        )
                    )
            )
        }
    }
}

@Composable
private fun GenerateProofCard(onStartGeneration: () -> Unit = {}) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Outlined.CheckCircle,
                contentDescription = null,
                tint = AccentCyan,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.generate_proof),
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.generate_proof_desc),
                color = Color.Gray,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onStartGeneration,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AccentCyan)
            ) {
                Text(
                    text = stringResource(R.string.start_generation),
                    color = DarkBackground,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun QuickActionsRow(onProfileClick: () -> Unit = {}) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        QuickActionCard(
            icon = Icons.Outlined.Person,
            label = stringResource(R.string.profile),
            modifier = Modifier.weight(1f),
            onClick = onProfileClick
        )
        QuickActionCard(
            icon = Icons.Outlined.Info,
            label = stringResource(R.string.support),
            modifier = Modifier.weight(1f),
            onClick = { }
        )
    }
}

@Composable
private fun QuickActionCard(
    icon: ImageVector,
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, contentDescription = label, tint = Color.White)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = label, color = Color.White, fontSize = 14.sp)
        }
    }
}


@Composable
private fun BottomNavigationBar(
    onNavigateToActivity: () -> Unit = {},
    onNavigateToGenerateProof: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {}
) {
    NavigationBar(
        containerColor = CardBackground,
        contentColor = Color.White
    ) {
        NavigationBarItem(
            selected = true,
            onClick = { },
            icon = {
                Icon(
                    Icons.Filled.Home,
                    contentDescription = stringResource(R.string.home),
                    modifier = Modifier.size(24.dp)
                )
            },
            label = { Text(stringResource(R.string.home), fontSize = 12.sp) },
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
                    contentDescription = stringResource(R.string.activity),
                    modifier = Modifier.size(24.dp)
                )
            },
            label = { Text(stringResource(R.string.activity), fontSize = 12.sp) },
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
                    contentDescription = stringResource(R.string.generate_proof),
                    modifier = Modifier.size(24.dp)
                )
            },
            label = { Text(stringResource(R.string.generate_proof), fontSize = 11.sp) },
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
                    contentDescription = stringResource(R.string.settings),
                    modifier = Modifier.size(24.dp)
                )
            },
            label = { Text(stringResource(R.string.settings), fontSize = 12.sp) },
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
fun HomeScreenPreview() {
    ZKTheme {
        HomeScreen()
    }
}
