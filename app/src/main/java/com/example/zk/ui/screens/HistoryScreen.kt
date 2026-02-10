package com.example.zk.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.zk.ui.theme.ZKTheme

private val DarkBackground = Color(0xFF0D1421)
private val CardBackground = Color(0xFF1A2332)
private val AccentCyan = Color(0xFF00D9FF)
private val AccentGreen = Color(0xFF4CAF50)

data class ActivityItem(
    val location: String,
    val dateTime: String,
    val conditions: List<String>,
    val isValid: Boolean = true
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    onNavigateToHome: () -> Unit = {},
    onNavigateToGenerateProof: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {}
) {
    var selectedFilter by remember { mutableStateOf("All") }
    val filters = listOf("All", "Success", "Failed", "This week")

    val activities = listOf(
        ActivityItem(
            location = "JFK Airport, New York",
            dateTime = "2024-12-14 14:30",
            conditions = listOf("Age ≥ 18", "Valid Passport")
        ),
        ActivityItem(
            location = "LAX Airport, Los Angeles",
            dateTime = "2024-12-10 09:15",
            conditions = listOf("Valid Passport", "Citizenship")
        ),
        ActivityItem(
            location = "Border Checkpoint, TX",
            dateTime = "2024-12-05 16:45",
            conditions = listOf("Age ≥ 21", "Valid Passport")
        )
    )

    Scaffold(
        containerColor = DarkBackground,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Activity",
                        color = Color.White,
                        fontWeight = FontWeight.Medium
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkBackground
                )
            )
        },
        bottomBar = {
            ActivityBottomNavigationBar(
                onNavigateToHome = onNavigateToHome,
                onNavigateToGenerateProof = onNavigateToGenerateProof,
                onNavigateToSettings = onNavigateToSettings
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            // Filter Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                filters.forEach { filter ->
                    FilterChipItem(
                        text = filter,
                        selected = selectedFilter == filter,
                        onClick = { selectedFilter = filter }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Activity List
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(activities) { activity ->
                    ActivityCard(activity = activity)
                }
            }
        }
    }
}

@Composable
private fun FilterChipItem(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = {
            Text(
                text = text,
                fontSize = 12.sp
            )
        },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = AccentCyan,
            selectedLabelColor = DarkBackground,
            containerColor = CardBackground,
            labelColor = Color.White
        ),
        border = FilterChipDefaults.filterChipBorder(
            borderColor = Color.Gray.copy(alpha = 0.3f),
            selectedBorderColor = AccentCyan,
            enabled = true,
            selected = selected
        )
    )
}

@Composable
private fun ActivityCard(activity: ActivityItem) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = activity.location,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
                // Valid Badge
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = AccentGreen.copy(alpha = 0.2f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Filled.CheckCircle,
                            contentDescription = null,
                            tint = AccentGreen,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Valid",
                            color = AccentGreen,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Date Time
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Outlined.DateRange,
                    contentDescription = null,
                    tint = Color.Gray,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = activity.dateTime,
                    color = Color.Gray,
                    fontSize = 13.sp
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Conditions Section
            Text(
                text = "Conditions Verified:",
                color = Color.Gray,
                fontSize = 12.sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Conditions List
            activity.conditions.forEach { condition ->
                Row(
                    modifier = Modifier.padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(4.dp)
                            .background(AccentCyan, RoundedCornerShape(2.dp))
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = condition,
                        color = Color.White,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun ActivityBottomNavigationBar(
    onNavigateToHome: () -> Unit = {},
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
            selected = true,
            onClick = { },
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
fun HistoryScreenPreview() {
    ZKTheme {
        HistoryScreen()
    }
}
