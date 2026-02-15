package com.hcwebhook.app

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Webhook
import androidx.compose.ui.graphics.vector.ImageVector

sealed class NavigationScreen(val route: String, val title: String, val icon: ImageVector) {
    object Home : NavigationScreen("home", "Home", Icons.Filled.Home)
    object Webhooks : NavigationScreen("webhooks", "Webhooks", Icons.Filled.Webhook)
    object Logs : NavigationScreen("logs", "Logs", Icons.Filled.List)
    object About : NavigationScreen("about", "About", Icons.Filled.Info)
}

val bottomNavItems = listOf(
    NavigationScreen.Home,
    NavigationScreen.Webhooks,
    NavigationScreen.Logs,
    NavigationScreen.About
)
