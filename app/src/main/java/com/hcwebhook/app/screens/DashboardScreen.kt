package com.hcwebhook.app.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.health.connect.client.HealthConnectClient
import com.hcwebhook.app.HealthConnectManager
import com.hcwebhook.app.R
import com.hcwebhook.app.dashboard.DashboardMetric
import com.hcwebhook.app.dashboard.DashboardMetricPresentation
import com.hcwebhook.app.dashboard.DashboardSnapshot
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Instant

private data class DashboardMetricCard(
    val label: String,
    val value: String,
    val subtitle: String,
    val presentation: com.hcwebhook.app.dashboard.DashboardMetricStyle,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    hasPermissions: Boolean?,
    grantedPermissionsSet: Set<String>,
    sdkStatus: Int,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    var snapshot by remember { mutableStateOf<DashboardSnapshot?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var loadError by remember { mutableStateOf(false) }
    var tick by remember { mutableLongStateOf(0L) }

    val grantedTypes = remember(grantedPermissionsSet) {
        HealthConnectManager.grantedDataTypes(grantedPermissionsSet)
    }
    val canLoad = sdkStatus == HealthConnectClient.SDK_AVAILABLE && grantedTypes.isNotEmpty()

    suspend fun loadStats() {
        isLoading = true
        loadError = false
        val result = HealthConnectManager(context).readTodayDashboardStats(grantedPermissionsSet)
        isLoading = false
        result.fold(
            onSuccess = { snapshot = it },
            onFailure = { loadError = true },
        )
    }

    LaunchedEffect(canLoad, grantedPermissionsSet) {
        if (!canLoad) {
            snapshot = null
            return@LaunchedEffect
        }
        loadStats()
    }

    LaunchedEffect(snapshot?.fetchedAt) {
        while (snapshot != null) {
            delay(1_000)
            tick = System.currentTimeMillis()
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.dashboard_title)) },
                windowInsets = WindowInsets(0),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back),
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            if (!canLoad || isLoading) return@IconButton
                            scope.launch { loadStats() }
                        },
                        enabled = canLoad && !isLoading,
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(
                                imageVector = Icons.Filled.Refresh,
                                contentDescription = stringResource(R.string.dashboard_refresh),
                            )
                        }
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp),
        ) {
            when {
            hasPermissions == null -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }
            !canLoad -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 48.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(R.string.dashboard_no_permissions),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
            }
            loadError -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 48.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(R.string.dashboard_load_error),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center,
                    )
                }
            }
            else -> {
                val currentSnapshot = snapshot
                val cards = currentSnapshot?.metrics?.map { it.toCard() }.orEmpty()

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(scrollState),
                ) {
                    if (currentSnapshot == null && isLoading) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 48.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularProgressIndicator()
                        }
                    } else if (cards.isEmpty()) {
                        Text(
                            text = stringResource(R.string.dashboard_no_permissions),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 48.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                    } else {
                        DashboardMetricGrid(cards = cards)

                        if (currentSnapshot != null) {
                            @Suppress("UNUSED_VARIABLE")
                            val unusedTick = tick
                            Text(
                                text = dashboardUpdatedText(currentSnapshot.fetchedAt),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 24.dp, bottom = 16.dp),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }
            }
        }
        }
    }
}

@Composable
private fun DashboardMetric.toCard(): DashboardMetricCard {
    val style = DashboardMetricPresentation.styleFor(type)
    return DashboardMetricCard(
        label = stringResource(type.nameResId),
        value = value,
        subtitle = stringResource(subtitleResId),
        presentation = style,
    )
}

@Composable
private fun DashboardMetricGrid(cards: List<DashboardMetricCard>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        cards.chunked(2).forEach { rowCards ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                rowCards.forEach { card ->
                    DashboardMetricCardView(
                        card = card,
                        modifier = Modifier.weight(1f),
                    )
                }
                if (rowCards.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun DashboardMetricCardView(
    card: DashboardMetricCard,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(card.presentation.iconBackground, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = card.presentation.icon,
                    contentDescription = null,
                    tint = card.presentation.iconTint,
                    modifier = Modifier.size(22.dp),
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = card.value,
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = card.label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
            )
            Text(
                text = card.subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = card.presentation.iconTint,
            )
        }
    }
}

@Composable
private fun dashboardUpdatedText(fetchedAt: Instant): String {
    val elapsedSeconds = ((System.currentTimeMillis() - fetchedAt.toEpochMilli()) / 1000)
        .coerceAtLeast(0)
    return when {
        elapsedSeconds < 5 -> stringResource(R.string.dashboard_updated_just_now)
        elapsedSeconds < 60 -> stringResource(R.string.dashboard_updated_seconds, elapsedSeconds)
        elapsedSeconds < 3600 -> stringResource(R.string.dashboard_updated_minutes, elapsedSeconds / 60)
        else -> stringResource(R.string.dashboard_updated_hours, elapsedSeconds / 3600)
    }
}
