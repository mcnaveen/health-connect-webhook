package com.hcwebhook.app.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.Accessibility
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Bloodtype
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeviceThermostat
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Height
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.Webhook
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.hcwebhook.app.FlavorUtils
import com.hcwebhook.app.HealthDataType
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(onFinish: () -> Unit) {
    val pagerState = rememberPagerState(pageCount = { 3 })
    val scope = rememberCoroutineScope()
    var showSkipDialog by remember { mutableStateOf(false) }

    if (showSkipDialog) {
        AlertDialog(
            onDismissRequest = { showSkipDialog = false },
            title = { Text("Skip Introduction?") },
            text = { Text("You can view it again anytime from the About screen.") },
            confirmButton = {
                TextButton(onClick = {
                    showSkipDialog = false
                    onFinish()
                }) { Text("Skip") }
            },
            dismissButton = {
                TextButton(onClick = { showSkipDialog = false }) { Text("Cancel") }
            }
        )
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0.dp),
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 32.dp, top = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Page dots
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    repeat(3) { index ->
                        val color by animateColorAsState(
                            targetValue = if (pagerState.currentPage == index)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.outlineVariant,
                            label = "dot_color"
                        )
                        Box(
                            modifier = Modifier
                                .size(if (pagerState.currentPage == index) 10.dp else 8.dp)
                                .background(color, CircleShape)
                        )
                    }
                }

                Button(
                    onClick = {
                        if (pagerState.currentPage < 2) {
                            scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                        } else {
                            onFinish()
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (pagerState.currentPage < 2) "Next" else "Get Started")
                }

                if (pagerState.currentPage < 2) {
                    TextButton(onClick = { showSkipDialog = true }) {
                        Text("Skip", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    ) { padding ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .statusBarsPadding()
        ) { page ->
            when (page) {
                0 -> WelcomePage()
                1 -> DataTypesPage()
                2 -> PrivacyPage()
            }
        }
    }
}

@Composable
private fun WelcomePage() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Filled.Webhook,
            contentDescription = null,
            modifier = Modifier.size(72.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Health Connect to Webhook",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Forward your health data to any webhook — automatically.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(32.dp))
        FeatureRow(
            icon = Icons.Filled.CheckCircle,
            title = "You choose what to sync",
            description = "Select only the health data types you want to forward."
        )
        Spacer(modifier = Modifier.height(16.dp))
        FeatureRow(
            icon = Icons.Filled.Webhook,
            title = "Send to any webhook URL",
            description = "Your data goes directly to your configured URLs — nothing else."
        )
        Spacer(modifier = Modifier.height(16.dp))
        FeatureRow(
            icon = Icons.Filled.Lock,
            title = "Permissions on demand",
            description = "Health Connect permissions are requested only for the types you enable."
        )
    }
}

@Composable
private fun DataTypesPage() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
            .padding(bottom = 24.dp)
    ) {
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Available Data Types",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Choose from ${HealthDataType.entries.size} health data types to forward to your webhook. You're in full control — only what you turn on gets synced.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(20.dp))

        val groups = mapOf(
            "Activity" to listOf(
                HealthDataType.STEPS,
                HealthDataType.DISTANCE,
                HealthDataType.ACTIVE_CALORIES,
                HealthDataType.TOTAL_CALORIES,
                HealthDataType.EXERCISE
            ),
            "Heart & Vitals" to listOf(
                HealthDataType.HEART_RATE,
                HealthDataType.HEART_RATE_VARIABILITY,
                HealthDataType.RESTING_HEART_RATE,
                HealthDataType.BLOOD_PRESSURE,
                HealthDataType.OXYGEN_SATURATION,
                HealthDataType.RESPIRATORY_RATE,
                HealthDataType.BODY_TEMPERATURE,
                HealthDataType.BLOOD_GLUCOSE
            ),
            "Sleep" to listOf(
                HealthDataType.SLEEP
            ),
            "Body Composition" to listOf(
                HealthDataType.WEIGHT,
                HealthDataType.HEIGHT,
                HealthDataType.BODY_FAT,
                HealthDataType.LEAN_BODY_MASS,
                HealthDataType.BONE_MASS,
                HealthDataType.BASAL_METABOLIC_RATE,
                HealthDataType.VO2_MAX
            ),
            "Nutrition & Hydration" to listOf(
                HealthDataType.NUTRITION,
                HealthDataType.HYDRATION
            )
        )

        groups.forEach { (groupName, types) ->
            Text(
                text = groupName,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))
            types.forEach { type ->
                Row(
                    modifier = Modifier.padding(bottom = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = iconForDataType(type),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp).padding(top = 2.dp)
                    )
                    Column {
                        Text(
                            text = type.displayName,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = type.rationale,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 8.dp),
                color = MaterialTheme.colorScheme.outlineVariant
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun PrivacyPage() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Filled.Lock,
            contentDescription = null,
            modifier = Modifier.size(72.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Your Data, Your Control",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "This app is a transparent forwarder. Here's exactly how your data is handled:",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(32.dp))

        val points = listOf(
            "You decide which health data types to enable — nothing is read without your choice",
            "Permissions are only asked for the types you turn on. If you never enable a type, its permission is never requested",
            "Your data is sent directly to your own webhook URLs — nowhere else",
            "Nothing is stored on any external server",
            "Your data is never sold or shared with anyone"
        )

        points.forEach { point ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .size(20.dp)
                        .padding(top = 2.dp)
                )
                Text(
                    text = point,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Have feedback or suggestions? Head to the About tab anytime — we'd love to hear from you.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        if (FlavorUtils.isPlayStore) {
            Spacer(modifier = Modifier.height(24.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Filled.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.size(20.dp).padding(top = 2.dp)
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "Use only the official version",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Text(
                            text = "Cracked or modified versions of this app may steal your webhook URLs or send your health data to unknown servers. Always download from Google Play.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
        }
    }
}

private fun iconForDataType(type: HealthDataType): ImageVector = when (type) {
    HealthDataType.STEPS               -> Icons.Filled.DirectionsWalk
    HealthDataType.DISTANCE            -> Icons.Filled.Straighten
    HealthDataType.ACTIVE_CALORIES     -> Icons.Filled.LocalFireDepartment
    HealthDataType.TOTAL_CALORIES      -> Icons.Filled.Whatshot
    HealthDataType.EXERCISE            -> Icons.Filled.FitnessCenter
    HealthDataType.HEART_RATE          -> Icons.Filled.MonitorHeart
    HealthDataType.HEART_RATE_VARIABILITY -> Icons.Filled.ShowChart
    HealthDataType.RESTING_HEART_RATE  -> Icons.Filled.Favorite
    HealthDataType.BLOOD_PRESSURE      -> Icons.Filled.Bloodtype
    HealthDataType.BLOOD_GLUCOSE       -> Icons.Filled.Bloodtype
    HealthDataType.OXYGEN_SATURATION   -> Icons.Filled.Air
    HealthDataType.RESPIRATORY_RATE    -> Icons.Filled.Air
    HealthDataType.BODY_TEMPERATURE    -> Icons.Filled.DeviceThermostat
    HealthDataType.SLEEP               -> Icons.Filled.Bedtime
    HealthDataType.WEIGHT              -> Icons.Filled.MonitorWeight
    HealthDataType.HEIGHT              -> Icons.Filled.Height
    HealthDataType.BODY_FAT            -> Icons.Filled.MonitorWeight
    HealthDataType.LEAN_BODY_MASS      -> Icons.Filled.FitnessCenter
    HealthDataType.BONE_MASS           -> Icons.Filled.Accessibility
    HealthDataType.BASAL_METABOLIC_RATE -> Icons.Filled.LocalFireDepartment
    HealthDataType.VO2_MAX             -> Icons.Filled.Speed
    HealthDataType.NUTRITION           -> Icons.Filled.Restaurant
    HealthDataType.HYDRATION           -> Icons.Filled.WaterDrop
}

@Composable
private fun FeatureRow(icon: ImageVector, title: String, description: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .size(24.dp)
                .padding(top = 2.dp)
        )
        Column {
            Text(text = title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            Text(text = description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
