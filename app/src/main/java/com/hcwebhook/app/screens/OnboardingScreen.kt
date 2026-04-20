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
import androidx.compose.ui.res.stringResource
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.compose.material.icons.filled.Translate
import com.hcwebhook.app.FlavorUtils
import com.hcwebhook.app.HealthDataType
import com.hcwebhook.app.R
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
            title = { Text(stringResource(R.string.onboarding_skip_dialog_title)) },
            text = { Text(stringResource(R.string.onboarding_skip_dialog_text)) },
            confirmButton = {
                TextButton(onClick = {
                    showSkipDialog = false
                    onFinish()
                }) { Text(stringResource(R.string.onboarding_skip)) }
            },
            dismissButton = {
                TextButton(onClick = { showSkipDialog = false }) { Text(stringResource(R.string.onboarding_cancel)) }
            }
        )
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0.dp),
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .statusBarsPadding(),
                horizontalArrangement = Arrangement.End
            ) {
                LanguageSelector()
            }
        },
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
                    Text(if (pagerState.currentPage < 2) stringResource(R.string.onboarding_next) else stringResource(R.string.onboarding_get_started))
                }

                if (pagerState.currentPage < 2) {
                    TextButton(onClick = { showSkipDialog = true }) {
                        Text(stringResource(R.string.onboarding_skip), color = MaterialTheme.colorScheme.onSurfaceVariant)
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
            text = stringResource(R.string.onboarding_welcome_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.onboarding_welcome_subtitle),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(32.dp))
        FeatureRow(
            icon = Icons.Filled.CheckCircle,
            title = stringResource(R.string.onboarding_feature1_title),
            description = stringResource(R.string.onboarding_feature1_desc)
        )
        Spacer(modifier = Modifier.height(16.dp))
        FeatureRow(
            icon = Icons.Filled.Webhook,
            title = stringResource(R.string.onboarding_feature2_title),
            description = stringResource(R.string.onboarding_feature2_desc)
        )
        Spacer(modifier = Modifier.height(16.dp))
        FeatureRow(
            icon = Icons.Filled.Lock,
            title = stringResource(R.string.onboarding_feature3_title),
            description = stringResource(R.string.onboarding_feature3_desc)
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
            text = stringResource(R.string.onboarding_datatypes_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.onboarding_datatypes_desc, HealthDataType.entries.size),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(20.dp))

        val groups = mapOf(
            stringResource(R.string.onboarding_group_activity) to listOf(
                HealthDataType.STEPS,
                HealthDataType.DISTANCE,
                HealthDataType.ACTIVE_CALORIES,
                HealthDataType.TOTAL_CALORIES,
                HealthDataType.EXERCISE
            ),
            stringResource(R.string.onboarding_group_heart_vitals) to listOf(
                HealthDataType.HEART_RATE,
                HealthDataType.HEART_RATE_VARIABILITY,
                HealthDataType.RESTING_HEART_RATE,
                HealthDataType.BLOOD_PRESSURE,
                HealthDataType.OXYGEN_SATURATION,
                HealthDataType.RESPIRATORY_RATE,
                HealthDataType.BODY_TEMPERATURE,
                HealthDataType.BLOOD_GLUCOSE
            ),
            stringResource(R.string.onboarding_group_sleep) to listOf(
                HealthDataType.SLEEP
            ),
            stringResource(R.string.onboarding_group_body_composition) to listOf(
                HealthDataType.WEIGHT,
                HealthDataType.HEIGHT,
                HealthDataType.BODY_FAT,
                HealthDataType.LEAN_BODY_MASS,
                HealthDataType.BONE_MASS,
                HealthDataType.BASAL_METABOLIC_RATE,
                HealthDataType.VO2_MAX
            ),
            stringResource(R.string.onboarding_group_nutrition) to listOf(
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
                            text = stringResource(id = type.nameResId),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = stringResource(id = type.rationaleResId),
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
            text = stringResource(R.string.onboarding_privacy_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.onboarding_privacy_subtitle),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(32.dp))

        val points = listOf(
            stringResource(R.string.onboarding_privacy_p1),
            stringResource(R.string.onboarding_privacy_p2),
            stringResource(R.string.onboarding_privacy_p3),
            stringResource(R.string.onboarding_privacy_p4),
            stringResource(R.string.onboarding_privacy_p5)
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
            text = stringResource(R.string.onboarding_privacy_footer),
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
                            text = stringResource(R.string.onboarding_privacy_warning_title),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Text(
                            text = stringResource(R.string.onboarding_privacy_warning_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
        }
    }
}

fun iconForDataType(type: HealthDataType): ImageVector = when (type) {
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

@Composable
fun LanguageSelector() {
    var expanded by remember { mutableStateOf(false) }

    val languages = listOf(
        "" to "System Default",
        "en" to "English",
        "ta" to "தமிழ்", // Tamil
        "fr" to "Français", // French
        "de" to "Deutsch", // German
        "es" to "Español", // Spanish
        "pt" to "Português", // Portuguese
        "zh" to "中文", // Chinese
        "ja" to "日本語", // Japanese
        "ko" to "한국어", // Korean
        "it" to "Italiano" // Italian
    )

    Box(modifier = Modifier.wrapContentSize(Alignment.TopEnd)) {
        IconButton(onClick = { expanded = true }) {
            Icon(Icons.Filled.Translate, contentDescription = "Select Language")
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            languages.forEach { (tag, name) ->
                DropdownMenuItem(
                    text = { Text(name) },
                    onClick = {
                        if (tag.isEmpty()) {
                            AppCompatDelegate.setApplicationLocales(LocaleListCompat.getEmptyLocaleList())
                        } else {
                            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(tag))
                        }
                        expanded = false
                    }
                )
            }
        }
    }
}
