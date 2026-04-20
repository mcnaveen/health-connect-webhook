package com.hcwebhook.app

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Webhook
import androidx.compose.ui.graphics.vector.ImageVector

sealed class NavigationScreen(val route: String, val titleResId: Int, val icon: ImageVector) {
    object Home : NavigationScreen("home", R.string.nav_home, Icons.Filled.Home)
    object Webhooks : NavigationScreen("webhooks", R.string.nav_webhooks, Icons.Filled.Webhook)
    object Logs : NavigationScreen("logs", R.string.nav_logs, Icons.Filled.List)
    object About : NavigationScreen("about", R.string.nav_about, Icons.Filled.Info)
}

val bottomNavItems = listOf(
    NavigationScreen.Home,
    NavigationScreen.Webhooks,
    NavigationScreen.Logs,
    NavigationScreen.About
)
