package com.hcwebhook.app.dashboard

import com.hcwebhook.app.HealthDataType
import java.time.Instant

data class DashboardMetric(
    val type: HealthDataType,
    val value: String,
    val subtitleResId: Int,
)

data class DashboardSnapshot(
    val metrics: List<DashboardMetric>,
    val fetchedAt: Instant = Instant.now(),
)
