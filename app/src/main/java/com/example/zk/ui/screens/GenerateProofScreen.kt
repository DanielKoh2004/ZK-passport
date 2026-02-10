package com.example.zk.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.zk.ui.theme.ZKTheme

private val DarkBackground = Color(0xFF0D1421)
private val CardBackground = Color(0xFF1A2332)
private val AccentCyan = Color(0xFF00D9FF)
private val AccentGreen = Color(0xFF4CAF50)

data class ProofTemplate(
    val title: String,
    val description: String
)

data class DisclosureItem(
    val icon: ImageVector,
    val title: String,
    val subtitle: String? = null,
    val isSelectable: Boolean = true,
    val isLocked: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GenerateProofScreen(
    onBackClick: () -> Unit = {},
    onNavigateToHome: () -> Unit = {},
    onNavigateToActivity: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {}
) {
    var selectedTemplate by remember { mutableStateOf(0) }
    var passportPhotoSelected by remember { mutableStateOf(false) }
    var fullNameSelected by remember { mutableStateOf(true) }

    val templates = listOf(
        ProofTemplate("Prove Age ≥ 18", "Verifies over 18 without revealing DOB"),
        ProofTemplate("Prove Nationality", "Verifies citizenship without passport no."),
        ProofTemplate("Prove Credential Valid", "Checks signature validity only")
    )

    val disclosureItems = listOf(
        DisclosureItem(
            icon = Icons.Filled.Person,
            title = "Passport Photo",
            isSelectable = true
        ),
        DisclosureItem(
            icon = Icons.Outlined.Person,
            title = "Full Name",
            isSelectable = true
        ),
        DisclosureItem(
            icon = Icons.Filled.Lock,
            title = "Passport Number",
            subtitle = "Not shared",
            isSelectable = false,
            isLocked = true
        ),
        DisclosureItem(
            icon = Icons.Filled.Lock,
            title = "Date of Birth",
            subtitle = "Hidden by ZK Proof",
            isSelectable = false,
            isLocked = true
        )
    )

    Scaffold(
        containerColor = DarkBackground,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Proof Generation",
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
            GenerateProofBottomNavigationBar(
                onNavigateToHome = onNavigateToHome,
                onNavigateToActivity = onNavigateToActivity,
                onNavigateToSettings = onNavigateToSettings
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {

                // Select Proof Template Section
                item {
                    Text(
                        text = "Select Proof Template",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // Template Options
                items(templates.size) { index ->
                    TemplateOption(
                        template = templates[index],
                        isSelected = selectedTemplate == index,
                        onClick = { selectedTemplate = index }
                    )
                }

                // Selective Disclosure Section
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Selective Disclosure",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // Disclosure Items
                item {
                    DisclosureOption(
                        item = disclosureItems[0],
                        isChecked = passportPhotoSelected,
                        onCheckedChange = { passportPhotoSelected = it }
                    )
                }

                item {
                    DisclosureOption(
                        item = disclosureItems[1],
                        isChecked = fullNameSelected,
                        onCheckedChange = { fullNameSelected = it }
                    )
                }

                item {
                    LockedDisclosureOption(item = disclosureItems[2])
                }

                item {
                    LockedDisclosureOption(item = disclosureItems[3])
                }

                // Privacy Notice
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            Icons.Filled.CheckCircle,
                            contentDescription = null,
                            tint = AccentGreen,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Only proof is shared; no passport data revealed.",
                            color = AccentGreen,
                            fontSize = 12.sp
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            // Generate Proof Button
            Button(
                onClick = { /* TODO: Generate proof */ },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AccentCyan)
            ) {
                Text(
                    text = "Generate Proof",
                    color = DarkBackground,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
        }
    }
}


@Composable
private fun TemplateOption(
    template: ProofTemplate,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        border = if (isSelected) {
            androidx.compose.foundation.BorderStroke(1.dp, AccentCyan)
        } else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = isSelected,
                onClick = onClick,
                colors = RadioButtonDefaults.colors(
                    selectedColor = AccentCyan,
                    unselectedColor = Color.Gray
                )
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = template.title,
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = template.description,
                    color = Color.Gray,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
private fun DisclosureOption(
    item: DisclosureItem,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit
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
                item.icon,
                contentDescription = null,
                tint = Color.Gray,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = item.title,
                color = Color.White,
                fontSize = 15.sp,
                modifier = Modifier.weight(1f)
            )
            Checkbox(
                checked = isChecked,
                onCheckedChange = onCheckedChange,
                colors = CheckboxDefaults.colors(
                    checkedColor = AccentCyan,
                    uncheckedColor = Color.Gray,
                    checkmarkColor = DarkBackground
                )
            )
        }
    }
}

@Composable
private fun LockedDisclosureOption(item: DisclosureItem) {
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
                item.icon,
                contentDescription = null,
                tint = Color.Gray,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    color = Color.Gray,
                    fontSize = 15.sp,
                    textDecoration = TextDecoration.LineThrough
                )
                item.subtitle?.let {
                    Text(
                        text = it,
                        color = Color.Gray.copy(alpha = 0.7f),
                        fontSize = 11.sp
                    )
                }
            }
            Icon(
                Icons.Filled.Lock,
                contentDescription = "Locked",
                tint = Color.Gray,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun GenerateProofBottomNavigationBar(
    onNavigateToHome: () -> Unit = {},
    onNavigateToActivity: () -> Unit = {},
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
            selected = true,
            onClick = { },
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
fun GenerateProofScreenPreview() {
    ZKTheme {
        GenerateProofScreen()
    }
}
