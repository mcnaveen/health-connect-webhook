package com.hcwebhook.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.hcwebhook.app.screens.AboutScreen
import com.hcwebhook.app.screens.ConfigurationScreen
import com.hcwebhook.app.screens.LogsScreen
import com.hcwebhook.app.ui.theme.HCWebhookTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var preferencesManager: PreferencesManager
    internal var pendingSyncCallback: (() -> Unit)? = null
    internal var permissionStatusCallback: ((Boolean) -> Unit)? = null
    private lateinit var permissionLauncher: androidx.activity.result.ActivityResultLauncher<Set<String>>

    private fun initializePermissionLauncher() {
        val requestPermissionActivityContract = androidx.health.connect.client.PermissionController.createRequestPermissionResultContract()

        permissionLauncher = registerForActivityResult(requestPermissionActivityContract) { granted: Set<String> ->
            lifecycleScope.launch {
                val healthConnectManager = HealthConnectManager(this@MainActivity)
                val grantedPermissions = healthConnectManager.getGrantedPermissions()
                val hasAnyPerms = grantedPermissions.isNotEmpty()

                permissionStatusCallback?.invoke(hasAnyPerms)

                if (hasAnyPerms && pendingSyncCallback != null) {
                    pendingSyncCallback?.invoke()
                    pendingSyncCallback = null
                } else if (!hasAnyPerms && pendingSyncCallback != null) {
                    android.widget.Toast.makeText(this@MainActivity, "No permissions granted", android.widget.Toast.LENGTH_LONG).show()
                    pendingSyncCallback = null
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        installSplashScreen()
        enableEdgeToEdge()
        preferencesManager = PreferencesManager(this)
        initializePermissionLauncher()

        setContent {
            HCWebhookTheme {
                MainScreenWithNav(
                    activity = this@MainActivity,
                    permissionLauncher = permissionLauncher
                )
            }
        }
    }

    @Composable
    fun MainScreenWithNav(
        activity: MainActivity,
        permissionLauncher: androidx.activity.result.ActivityResultLauncher<Set<String>>
    ) {
        var selectedScreen by remember { mutableStateOf<NavigationScreen>(NavigationScreen.Home) }

        Scaffold(
            bottomBar = {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    bottomNavItems.forEach { screen ->
                        NavigationBarItem(
                            icon = { Icon(screen.icon, contentDescription = screen.title) },
                            label = { Text(screen.title) },
                            selected = selectedScreen == screen,
                            onClick = { selectedScreen = screen }
                        )
                    }
                }
            }
        ) { padding ->
            Box(modifier = Modifier.padding(padding)) {
                when (selectedScreen) {
                    is NavigationScreen.Home -> ConfigurationScreen(
                        activity = activity,
                        permissionLauncher = permissionLauncher
                    )
                    is NavigationScreen.Webhooks -> com.hcwebhook.app.screens.WebhooksScreen(activity = activity)
                    is NavigationScreen.Logs -> LogsScreen()
                    is NavigationScreen.About -> AboutScreen()
                }
            }
        }
    }
}