package com.spg.nakmakanapa.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spg.nakmakanapa.model.DebugState
import com.spg.nakmakanapa.model.UserProfile
import com.spg.nakmakanapa.ui.theme.DarkGreenHeader
import com.spg.nakmakanapa.ui.theme.LightGrayBackground
import com.spg.nakmakanapa.ui.theme.TextPrimary
import com.spg.nakmakanapa.ui.theme.TextSecondary

@Composable
fun ProfileScreen(
    userProfile: UserProfile,
    debugState: DebugState = DebugState(),
    onUpdateProfile: (String, Int, Double, String, String) -> Unit,
    onResetSurpriseLimit: () -> Unit = {},
    onClearPickHistory: () -> Unit = {}
) {
    val scrollState = rememberScrollState()

    var showProfileSettingsDialog by remember { mutableStateOf(false) }
    var showAppSettingsDialog by remember { mutableStateOf(false) }
    var showHelpSupportDialog by remember { mutableStateOf(false) }
    var showAboutUsDialog by remember { mutableStateOf(false) }
    var showDebugDialog by remember { mutableStateOf(false) }
    var taps by remember { mutableStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LightGrayBackground)
            .verticalScroll(scrollState)
    ) {
        // Dark Green Curved Header Box with Status Bar Padding
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
                .background(DarkGreenHeader)
                .statusBarsPadding()
                .padding(vertical = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Profile",
                color = Color.White,
                fontFamily = FontFamily.Cursive,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // User Account Box
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = DarkGreenHeader),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Circle Avatar Icon
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .clip(CircleShape)
                        .background(Color.White),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Avatar",
                        tint = DarkGreenHeader,
                        modifier = Modifier.size(48.dp)
                    )
                }

                Spacer(modifier = Modifier.width(20.dp))

                Column {
                    Text(
                        text = "Hi! ${userProfile.username}",
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Normal
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "ID: ${userProfile.userId}",
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Normal
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 4 Profile Action Buttons
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ProfileActionButton(
                title = "Profile Settings",
                icon = Icons.Default.Person,
                onClick = { showProfileSettingsDialog = true }
            )

            ProfileActionButton(
                title = "Settings",
                icon = Icons.Default.Settings,
                onClick = { showAppSettingsDialog = true }
            )

            ProfileActionButton(
                title = "Help & Support",
                icon = Icons.AutoMirrored.Filled.HelpOutline,
                onClick = { showHelpSupportDialog = true }
            )

            ProfileActionButton(
                title = "About Us",
                icon = Icons.Default.Info,
                onClick = { showAboutUsDialog = true }
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }

    // Profile Settings Dialog
    if (showProfileSettingsDialog) {
        var usernameInput by remember { mutableStateOf(userProfile.username) }
        var maxBudgetInput by remember { mutableStateOf(userProfile.maxBudget.toString()) }
        var maxRadiusInput by remember { mutableStateOf(userProfile.maxRadiusKm.toInt().toString()) }
        var moodInput by remember { mutableStateOf(userProfile.customMoodPrompt) }

        AlertDialog(
            onDismissRequest = { showProfileSettingsDialog = false },
            title = { Text("Profile Settings", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = usernameInput,
                        onValueChange = { usernameInput = it },
                        label = { Text("Username") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = maxBudgetInput,
                        onValueChange = { maxBudgetInput = it },
                        label = { Text("Max Budget per Person (RM)") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = maxRadiusInput,
                        onValueChange = { maxRadiusInput = it },
                        label = { Text("Max Surprise Distance (KM)") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = moodInput,
                        onValueChange = { moodInput = it },
                        label = { Text("Custom Mood / Craving Note") }
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val budget = maxBudgetInput.toIntOrNull() ?: userProfile.maxBudget
                        val radius = maxRadiusInput.toDoubleOrNull() ?: userProfile.maxRadiusKm
                        onUpdateProfile(usernameInput, budget, radius, userProfile.dietaryRestrictions, moodInput)
                        showProfileSettingsDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DarkGreenHeader)
                ) {
                    Text("Save Changes")
                }
            },
            dismissButton = {
                TextButton(onClick = { showProfileSettingsDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }

    // App Settings Dialog
    if (showAppSettingsDialog) {
        AlertDialog(
            onDismissRequest = { showAppSettingsDialog = false },
            title = { Text("App Settings & API Keys", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "• Google Maps / Places API Key:\nLoaded from local.properties",
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "• DeepSeek AI Decision Engine:\nActive key loaded from local.properties",
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "• Token Usage Protection:\nStrict limit max_tokens: 150 per query (fractions of a cent per request).",
                        fontSize = 13.sp,
                        color = DarkGreenHeader,
                        fontWeight = FontWeight.Medium
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { showAppSettingsDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = DarkGreenHeader)
                ) {
                    Text("OK")
                }
            }
        )
    }

    // Help & Support Dialog
    if (showHelpSupportDialog) {
        AlertDialog(
            onDismissRequest = { showHelpSupportDialog = false },
            title = { Text("Help & Support", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "How to use Nak Makan Apa?",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Text(
                        text = "1. Click 'Jom Makan!' to open the Main Discovery Menu.\n2. Filter food by Budget, Distance, or Fancy dining.\n3. Click 'Adventure' for scenic riding/driving route recommendations.\n4. Click 'Surprise Me!' for AI-powered random restaurant selection!\n5. Bookmark your favorite spots to view them in 'Saved'.",
                        fontSize = 13.sp
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { showHelpSupportDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = DarkGreenHeader)
                ) {
                    Text("Got It!")
                }
            }
        )
    }

    // Debug Dialog (hidden — unlock by tapping version 7x in About Us)
    if (showDebugDialog) {
        AlertDialog(
            onDismissRequest = { showDebugDialog = false },
            title = { Text("Developer Debug", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Hidden tools for testing. These do not affect other users.",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                    HorizontalDivider()
                    DebugRow("Surprise Me — daily picks used") {
                        Text(
                            "${debugState.surpriseUsedToday} / ${debugState.surpriseDailyLimit}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = DarkGreenHeader
                        )
                    }
                    DebugRow("Pick cooldown active") {
                        Text(
                            if (debugState.cooldownActive) "yes" else "no",
                            fontSize = 13.sp,
                            color = if (debugState.cooldownActive) Color(0xFFB71C1C) else TextSecondary
                        )
                    }
                    DebugRow("Pick history entries") {
                        Text(
                            "${debugState.pickHistoryCount}",
                            fontSize = 13.sp,
                            color = TextPrimary
                        )
                    }
                    HorizontalDivider()
                    TextButton(
                        onClick = { onResetSurpriseLimit() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Reset Surprise Me limit & cooldown", color = DarkGreenHeader)
                    }
                    TextButton(
                        onClick = { onClearPickHistory() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Clear pick history", color = DarkGreenHeader)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDebugDialog = false }) { Text("Close") }
            }
        )
    }

    // About Us Dialog
    if (showAboutUsDialog) {
        AlertDialog(
            onDismissRequest = { showAboutUsDialog = false },
            title = { Text("About Us", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Nak Makan Apa? v1.0",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = DarkGreenHeader,
                        // Hidden debug unlock: tap the version 7 times.
                        modifier = Modifier.clickable {
                            taps++
                            if (taps >= 7) {
                                showAboutUsDialog = false
                                showDebugDialog = true
                                taps = 0
                            }
                        }
                    )
                    Text(
                        text = "Malaysian Food Discovery & Decision Application.\nDesigned to help hungry Malaysians decide what to eat effortlessly with AI recommendations and scenic route discoveries.",
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Powered by Google Places & DeepSeek AI",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { showAboutUsDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = DarkGreenHeader)
                ) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
private fun DebugRow(label: String, value: @Composable () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 13.sp, color = TextPrimary)
        value()
    }
}

@Composable
private fun ProfileActionButton(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = DarkGreenHeader,
                modifier = Modifier.size(36.dp)
            )

            Spacer(modifier = Modifier.width(24.dp))

            Text(
                text = title,
                fontSize = 24.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
        }
    }
}