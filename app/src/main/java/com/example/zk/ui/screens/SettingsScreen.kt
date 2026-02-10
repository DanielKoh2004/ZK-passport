package com.example.zk.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.example.zk.R
import com.example.zk.ui.theme.ZKTheme

private val DarkBackground = Color(0xFF0D1421)
private val CardBackground = Color(0xFF1A2332)
private val AccentCyan = Color(0xFF00D9FF)
private val ErrorRed = Color(0xFFFF5252)

// Language options
data class LanguageOption(
    val code: String,
    val displayName: String,
    val nativeName: String
)

val supportedLanguages = listOf(
    LanguageOption("en", "English", "English"),
    LanguageOption("zh", "Chinese", "中文"),
    LanguageOption("ms", "Malay", "Bahasa Melayu"),
    LanguageOption("ta", "Tamil", "தமிழ்"),
    LanguageOption("ja", "Japanese", "日本語"),
    LanguageOption("ko", "Korean", "한국어"),
    LanguageOption("es", "Spanish", "Español"),
    LanguageOption("fr", "French", "Français")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateToHome: () -> Unit = {},
    onNavigateToActivity: () -> Unit = {},
    onNavigateToGenerateProof: () -> Unit = {},
    onNavigateToProfile: () -> Unit = {},
    onNavigateToChangePassword: () -> Unit = {},
    onLogout: () -> Unit = {},
    userName: String = "User",
    passportExpiry: String = "",
    hasCredential: Boolean = false,
    isBiometricEnabled: Boolean = false,
    isBiometricAvailable: Boolean = false,
    biometricUnavailableReason: String = "",
    onBiometricToggle: (Boolean) -> Unit = {},
    currentLanguage: String = "en",
    onLanguageChange: (String) -> Unit = {}
) {
    var showBiometricDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }

    // Get current language display name
    val currentLanguageDisplay = supportedLanguages.find { it.code == currentLanguage }?.displayName ?: "English"

    // Language selection dialog
    if (showLanguageDialog) {
        AlertDialog(
            onDismissRequest = { showLanguageDialog = false },
            title = {
                Text(
                    text = stringResource(R.string.select_language),
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    supportedLanguages.forEach { language ->
                        val isSelected = language.code == currentLanguage
                        Surface(
                            onClick = {
                                onLanguageChange(language.code)
                                showLanguageDialog = false
                            },
                            color = if (isSelected) AccentCyan.copy(alpha = 0.2f) else Color.Transparent,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp, horizontal = 16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = language.displayName,
                                        color = if (isSelected) AccentCyan else Color.White,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                    Text(
                                        text = language.nativeName,
                                        color = Color.Gray,
                                        fontSize = 12.sp
                                    )
                                }
                                if (isSelected) {
                                    Icon(
                                        Icons.Outlined.Check,
                                        contentDescription = "Selected",
                                        tint = AccentCyan
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showLanguageDialog = false }) {
                    Text(stringResource(R.string.cancel), color = AccentCyan)
                }
            },
            containerColor = CardBackground
        )
    }

    // Dialog for when biometric is not available
    if (showBiometricDialog) {
        AlertDialog(
            onDismissRequest = { showBiometricDialog = false },
            title = {
                Text(
                    text = stringResource(R.string.biometric_not_available),
                    color = Color.White
                )
            },
            text = {
                Text(
                    text = biometricUnavailableReason.ifEmpty { stringResource(R.string.biometric_not_available_message) },
                    color = Color.Gray
                )
            },
            confirmButton = {
                TextButton(onClick = { showBiometricDialog = false }) {
                    Text(stringResource(R.string.ok), color = AccentCyan)
                }
            },
            containerColor = CardBackground
        )
    }

    Scaffold(
        containerColor = DarkBackground,
        topBar = {
            TopAppBar(
                title = {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.settings),
                            color = Color.White,
                            fontWeight = FontWeight.Medium
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkBackground
                )
            )
        },
        bottomBar = {
            SettingsBottomNavigationBar(
                onNavigateToHome = onNavigateToHome,
                onNavigateToActivity = onNavigateToActivity,
                onNavigateToGenerateProof = onNavigateToGenerateProof
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
            // Profile Avatar
            Box(
                contentAlignment = Alignment.BottomEnd
            ) {
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .background(Color.Gray)
                        .border(3.dp, AccentCyan, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    // Placeholder for profile image
                    Icon(
                        Icons.Filled.Person,
                        contentDescription = "Profile",
                        tint = Color.White,
                        modifier = Modifier.size(50.dp)
                    )
                }
                // Verified badge
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(AccentCyan),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Outlined.Check,
                        contentDescription = "Verified",
                        tint = DarkBackground,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // User Name
            Text(
                text = userName.ifEmpty { "User" },
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Passport validity
            if (hasCredential && passportExpiry.isNotEmpty()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Outlined.DateRange,
                        contentDescription = null,
                        tint = AccentCyan,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.passport_valid_until, passportExpiry),
                        color = AccentCyan,
                        fontSize = 13.sp
                    )
                }
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Outlined.Info,
                        contentDescription = null,
                        tint = Color.Gray,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.passport_not_connected),
                        color = Color.Gray,
                        fontSize = 13.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Account Section
            SectionHeader(title = stringResource(R.string.account))

            Spacer(modifier = Modifier.height(8.dp))

            SettingsMenuItem(
                icon = Icons.Outlined.Person,
                title = stringResource(R.string.profile),
                onClick = onNavigateToProfile
            )

            Spacer(modifier = Modifier.height(8.dp))

            SettingsMenuItem(
                icon = Icons.Filled.Lock,
                title = stringResource(R.string.change_password),
                onClick = onNavigateToChangePassword
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Security & Preferences Section
            SectionHeader(title = stringResource(R.string.security_preferences))

            Spacer(modifier = Modifier.height(8.dp))

            SettingsToggleItem(
                icon = Icons.Filled.Lock,
                title = stringResource(R.string.biometric_login),
                subtitle = if (!isBiometricAvailable) biometricUnavailableReason else null,
                isChecked = isBiometricEnabled,
                enabled = isBiometricAvailable,
                onCheckedChange = { enabled ->
                    if (isBiometricAvailable) {
                        onBiometricToggle(enabled)
                    } else {
                        showBiometricDialog = true
                    }
                }
            )

            Spacer(modifier = Modifier.height(8.dp))

            SettingsMenuItem(
                icon = Icons.Outlined.Place,
                title = stringResource(R.string.change_language),
                trailingText = currentLanguageDisplay,
                onClick = { showLanguageDialog = true }
            )

            Spacer(modifier = Modifier.height(8.dp))

            SettingsMenuItem(
                icon = Icons.Outlined.Info,
                title = stringResource(R.string.faq),
                onClick = { }
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Logout Button
            OutlinedButton(
                onClick = onLogout,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp),
                border = ButtonDefaults.outlinedButtonBorder.copy(
                    brush = androidx.compose.ui.graphics.SolidColor(Color.Gray.copy(alpha = 0.5f))
                ),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color.White
                )
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ExitToApp,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = stringResource(R.string.logout),
                    fontWeight = FontWeight.Medium,
                    fontSize = 16.sp
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Delete Account Button
            TextButton(
                onClick = { },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = stringResource(R.string.delete_account),
                    color = ErrorRed,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Box(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            color = Color.Gray,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun SettingsMenuItem(
    icon: ImageVector,
    title: String,
    trailingText: String? = null,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = title,
                color = Color.White,
                fontSize = 15.sp,
                modifier = Modifier.weight(1f)
            )
            if (trailingText != null) {
                Text(
                    text = trailingText,
                    color = Color.Gray,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Icon(
                Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                contentDescription = null,
                tint = Color.Gray,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
private fun SettingsToggleItem(
    icon: ImageVector,
    title: String,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    subtitle: String? = null,
    enabled: Boolean = true
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = if (enabled) Color.White else Color.Gray,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = if (enabled) Color.White else Color.Gray,
                    fontSize = 15.sp
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        color = Color.Gray,
                        fontSize = 12.sp
                    )
                }
            }
            Switch(
                checked = isChecked,
                onCheckedChange = onCheckedChange,
                enabled = enabled,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = AccentCyan,
                    uncheckedThumbColor = Color.Gray,
                    uncheckedTrackColor = CardBackground,
                    uncheckedBorderColor = Color.Gray,
                    disabledCheckedThumbColor = Color.Gray,
                    disabledCheckedTrackColor = Color.DarkGray,
                    disabledUncheckedThumbColor = Color.Gray,
                    disabledUncheckedTrackColor = Color.DarkGray
                )
            )
        }
    }
}

@Composable
private fun SettingsBottomNavigationBar(
    onNavigateToHome: () -> Unit = {},
    onNavigateToActivity: () -> Unit = {},
    onNavigateToGenerateProof: () -> Unit = {}
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
            selected = true,
            onClick = { },
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
fun SettingsScreenPreview() {
    ZKTheme {
        SettingsScreen()
    }
}
