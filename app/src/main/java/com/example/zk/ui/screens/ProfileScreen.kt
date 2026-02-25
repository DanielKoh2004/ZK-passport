package com.example.zk.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
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
import androidx.compose.ui.res.stringResource
import com.example.zk.R
import com.example.zk.ui.theme.ZKTheme

private val DarkBackground = Color(0xFF0D1421)
private val CardBackground = Color(0xFF1A2332)
private val AccentCyan = Color(0xFF00D9FF)
private val ErrorRed = Color(0xFFFF5252)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onBackClick: () -> Unit = {},
    onSaveClick: () -> Unit = {},
    onNavigateToHome: () -> Unit = {},
    onNavigateToActivity: () -> Unit = {},
    onNavigateToGenerateProof: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onNavigateToChangePin: () -> Unit = {},
    onScanPassport: () -> Unit = {},
    // Profile data from passport scan
    hasCredential: Boolean = false,
    userName: String = "",
    passportFullName: String = "",
    passportNationality: String = "",
    passportDateOfBirth: String = "",
    passportGender: String = "",
    passportDocNumber: String = "",
    passportExpiry: String = "",
    passportIssuingCountry: String = ""
) {
    // Use passport data if available, otherwise use defaults
    var fullName by remember(passportFullName) {
        mutableStateOf(passportFullName.ifEmpty { userName.ifEmpty { "Not Set" } })
    }
    var icNumber by remember(passportDocNumber) {
        mutableStateOf(passportDocNumber.ifEmpty { "Not Set" })
    }
    var country by remember(passportNationality) {
        mutableStateOf(passportNationality.ifEmpty { passportIssuingCountry.ifEmpty { "Not Set" } })
    }
    var dateOfBirth by remember(passportDateOfBirth) {
        mutableStateOf(passportDateOfBirth.ifEmpty { "Not Set" })
    }
    var gender by remember(passportGender) {
        mutableStateOf(passportGender.ifEmpty { "Not Set" })
    }
    var contactNumber by remember { mutableStateOf("") }

    Scaffold(
        containerColor = DarkBackground,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Edit Profile",
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
                    TextButton(onClick = onSaveClick) {
                        Text(
                            text = "Save",
                            color = AccentCyan,
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
            ProfileBottomNavigationBar(
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
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Profile Avatar with Edit
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
                    Icon(
                        Icons.Filled.Person,
                        contentDescription = "Profile",
                        tint = Color.White,
                        modifier = Modifier.size(50.dp)
                    )
                }
                // Edit badge
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(AccentCyan)
                        .clickable { /* Edit photo */ },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.Edit,
                        contentDescription = "Edit Photo",
                        tint = DarkBackground,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Credential Status Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (hasCredential)
                        Color(0xFF4CAF50).copy(alpha = 0.15f)
                    else
                        CardBackground
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        if (hasCredential) Icons.Outlined.CheckCircle else Icons.Outlined.Warning,
                        contentDescription = null,
                        tint = if (hasCredential) Color(0xFF4CAF50) else Color.Gray,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Credential Status",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = if (hasCredential) "Active - Stored Locally" else "Not Added",
                            color = if (hasCredential) Color(0xFF4CAF50) else Color.Gray,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Change PIN Button
            OutlinedButton(
                onClick = onNavigateToChangePin,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, AccentCyan)
            ) {
                Icon(
                    Icons.Outlined.Lock,
                    contentDescription = null,
                    tint = AccentCyan,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Change PIN",
                    color = AccentCyan,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Full Name Field - Read-only from passport
            ProfileTextField(
                label = "Full Name",
                value = fullName,
                onValueChange = { },
                readOnly = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Passport Number Field - Read-only from passport
            ProfileTextField(
                label = "Passport Number",
                value = icNumber,
                onValueChange = { },
                readOnly = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Nationality Field - Read-only from passport
            ProfileTextField(
                label = "Nationality",
                value = country,
                onValueChange = { },
                readOnly = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Date of Birth and Gender Row - Read-only from passport
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Date of Birth - Read-only
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Date of Birth",
                            color = Color.Gray,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            Icons.Outlined.CheckCircle,
                            contentDescription = "Verified from passport",
                            tint = Color(0xFF4CAF50),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    OutlinedTextField(
                        value = dateOfBirth,
                        onValueChange = { },
                        modifier = Modifier.fillMaxWidth(),
                        readOnly = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF4CAF50),
                            unfocusedBorderColor = Color(0xFF4CAF50).copy(alpha = 0.5f),
                            focusedContainerColor = CardBackground,
                            unfocusedContainerColor = CardBackground,
                            cursorColor = AccentCyan
                        ),
                        trailingIcon = {
                            Icon(
                                Icons.Outlined.Lock,
                                contentDescription = "From passport",
                                tint = Color.Gray,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        singleLine = true
                    )
                }

                // Gender - Read-only
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Gender",
                            color = Color.Gray,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            Icons.Outlined.CheckCircle,
                            contentDescription = "Verified from passport",
                            tint = Color(0xFF4CAF50),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    OutlinedTextField(
                        value = gender,
                        onValueChange = { },
                        modifier = Modifier.fillMaxWidth(),
                        readOnly = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF4CAF50),
                            unfocusedBorderColor = Color(0xFF4CAF50).copy(alpha = 0.5f),
                            focusedContainerColor = CardBackground,
                            unfocusedContainerColor = CardBackground,
                            cursorColor = AccentCyan
                        ),
                        trailingIcon = {
                            Icon(
                                Icons.Outlined.Lock,
                                contentDescription = "From passport",
                                tint = Color.Gray,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        singleLine = true
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Contact Number Field - Editable
            ProfileTextField(
                label = "Contact Number",
                value = contactNumber,
                onValueChange = { contactNumber = it },
                readOnly = false,
                placeholder = "Enter contact number"
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Passport Status Section
            if (hasCredential) {
                // Show passport verified status
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF4CAF50).copy(alpha = 0.15f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Outlined.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF4CAF50),
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Passport Verified",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Your passport data has been securely stored locally",
                                color = Color.Gray,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            } else {
                // Show scan passport button
                Button(
                    onClick = onScanPassport,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
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
                        text = "Scan Passport to Connect",
                        color = DarkBackground,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun ProfileTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    readOnly: Boolean = false,
    placeholder: String = ""
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                color = Color.Gray,
                fontSize = 13.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            if (readOnly) {
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    Icons.Outlined.CheckCircle,
                    contentDescription = "Verified from passport",
                    tint = Color(0xFF4CAF50),
                    modifier = Modifier.size(14.dp)
                )
            }
        }
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            readOnly = readOnly,
            placeholder = if (placeholder.isNotEmpty()) {
                { Text(placeholder, color = Color.Gray.copy(alpha = 0.6f)) }
            } else null,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                disabledTextColor = Color.White,
                focusedBorderColor = if (readOnly) Color(0xFF4CAF50) else AccentCyan,
                unfocusedBorderColor = if (readOnly) Color(0xFF4CAF50).copy(alpha = 0.5f) else Color.Gray.copy(alpha = 0.5f),
                disabledBorderColor = Color(0xFF4CAF50).copy(alpha = 0.5f),
                focusedContainerColor = CardBackground,
                unfocusedContainerColor = CardBackground,
                disabledContainerColor = CardBackground,
                cursorColor = AccentCyan
            ),
            trailingIcon = if (readOnly) {
                {
                    Icon(
                        Icons.Outlined.Lock,
                        contentDescription = "From passport",
                        tint = Color.Gray,
                        modifier = Modifier.size(18.dp)
                    )
                }
            } else null,
            singleLine = true
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProfileDropdown(
    label: String,
    value: String,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    options: List<String>,
    onOptionSelected: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            color = Color.Gray,
            fontSize = 13.sp,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = onExpandedChange
        ) {
            OutlinedTextField(
                value = value,
                onValueChange = {},
                readOnly = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = AccentCyan,
                    unfocusedBorderColor = Color.Gray.copy(alpha = 0.5f),
                    focusedContainerColor = CardBackground,
                    unfocusedContainerColor = CardBackground
                ),
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                },
                singleLine = true
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { onExpandedChange(false) },
                modifier = Modifier.background(CardBackground)
            ) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option, color = Color.White) },
                        onClick = { onOptionSelected(option) },
                        colors = MenuDefaults.itemColors(
                            textColor = Color.White
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfileBottomNavigationBar(
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
fun ProfileScreenPreview() {
    ZKTheme {
        ProfileScreen()
    }
}
