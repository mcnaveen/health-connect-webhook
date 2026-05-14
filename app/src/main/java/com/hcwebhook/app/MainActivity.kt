package com.hcwebhook.app

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.BackHandler
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.health.connect.client.HealthConnectClient
import androidx.lifecycle.lifecycleScope
import com.hcwebhook.app.screens.AboutScreen
import com.hcwebhook.app.screens.ConfigurationScreen
import com.hcwebhook.app.screens.LocalHttpSettingsScreen
import com.hcwebhook.app.screens.LogsScreen
import com.hcwebhook.app.screens.NotificationsScreen
import com.hcwebhook.app.screens.OnboardingScreen
import com.hcwebhook.app.screens.SettingsBackupScreen
import com.hcwebhook.app.screens.WebhooksScreen
import com.hcwebhook.app.ui.theme.HCWebhookTheme
import kotlinx.coroutines.launch
import androidx.compose.ui.res.stringResource

class MainActivity : AppCompatActivity() {

    private lateinit var preferencesManager: PreferencesManager
    internal var pendingSyncCallback: (() -> Unit)? = null
    internal var permissionStatusCallback: ((Boolean) -> Unit)? = null
    private lateinit var permissionLauncher: androidx.activity.result.ActivityResultLauncher<Set<String>>
    internal val openLocalHttpRequest = mutableStateOf(false)

    private fun initializePermissionLauncher() {
        val requestPermissionActivityContract = androidx.health.connect.client.PermissionController.createRequestPermissionResultContract()

        permissionLauncher = registerForActivityResult(requestPermissionActivityContract) { _: Set<String> ->
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
        
        // This will only do something if built with 'playstore' flavor.
        // It does absolutely nothing in the 'foss' flavor.
        FlavorUtils.verifyPlayStoreInstallation(this)
        
        installSplashScreen()
        enableEdgeToEdge()
        preferencesManager = PreferencesManager(this)
        initializePermissionLauncher()
        if (intent?.getBooleanExtra(LocalHttpServerService.EXTRA_OPEN_LOCAL_HTTP, false) == true) {
            openLocalHttpRequest.value = true
        }

        setContent {
            HCWebhookTheme {
                var showOnboarding by remember { mutableStateOf(!preferencesManager.hasSeenOnboarding()) }
                if (showOnboarding) {
                    OnboardingScreen(onFinish = {
                        preferencesManager.setHasSeenOnboarding()
                        showOnboarding = false
                    })
                } else {
                    MainScreenWithNav(
                        activity = this@MainActivity,
                        permissionLauncher = permissionLauncher,
                        onRestartOnboarding = { showOnboarding = true }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.getBooleanExtra(LocalHttpServerService.EXTRA_OPEN_LOCAL_HTTP, false)) {
            openLocalHttpRequest.value = true
        }
    }

    @Composable
    fun MainScreenWithNav(
        activity: MainActivity,
        permissionLauncher: androidx.activity.result.ActivityResultLauncher<Set<String>>,
        onRestartOnboarding: () -> Unit = {}
    ) {
        var selectedScreen by remember { mutableStateOf<NavigationScreen>(NavigationScreen.Home) }
        var showLocalHttpSettings by remember { mutableStateOf(false) }
        var showNotificationsSettings by remember { mutableStateOf(false) }
        var showSettingsBackup by remember { mutableStateOf(false) }

        LaunchedEffect(activity.openLocalHttpRequest.value) {
            if (activity.openLocalHttpRequest.value) {
                showLocalHttpSettings = true
                activity.openLocalHttpRequest.value = false
            }
        }

        // Hoisted permission state — survives tab switches
        var hasPermissions by remember { mutableStateOf<Boolean?>(null) }
        var grantedPermissionsSet by remember { mutableStateOf<Set<String>>(emptySet()) }
        var sdkStatus by remember { mutableIntStateOf(HealthConnectClient.SDK_UNAVAILABLE) }

        // Initial permission check
        LaunchedEffect(Unit) {
            try {
                val status = HealthConnectClient.getSdkStatus(activity)
                sdkStatus = status
                if (status == HealthConnectClient.SDK_AVAILABLE) {
                    val mgr = HealthConnectManager(activity)
                    val granted = mgr.getGrantedPermissions()
                    hasPermissions = granted.isNotEmpty()
                    grantedPermissionsSet = granted
                } else {
                    hasPermissions = false
                }
            } catch (e: Exception) {
                hasPermissions = false
            }
        }

        // Keep state fresh after the permission launcher returns
        DisposableEffect(Unit) {
            activity.permissionStatusCallback = { granted ->
                hasPermissions = granted
                if (granted) {
                    lifecycleScope.launch {
                        try {
                            grantedPermissionsSet = HealthConnectManager(activity).getGrantedPermissions()
                        } catch (_: Exception) {}
                    }
                } else {
                    grantedPermissionsSet = emptySet()
                }
            }
            onDispose { activity.permissionStatusCallback = null }
        }

        Scaffold(
            bottomBar = {
                if (!showLocalHttpSettings && !showNotificationsSettings && !showSettingsBackup) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        bottomNavItems.forEach { screen ->
                            NavigationBarItem(
                                icon = { Icon(screen.icon, contentDescription = stringResource(screen.titleResId)) },
                                label = { Text(stringResource(screen.titleResId)) },
                                selected = selectedScreen == screen,
                                onClick = { selectedScreen = screen }
                            )
                        }
                    }
                }
            }
        ) { padding ->
            BackHandler {
                if (showLocalHttpSettings) {
                    showLocalHttpSettings = false
                } else if (showNotificationsSettings) {
                    showNotificationsSettings = false
                } else if (showSettingsBackup) {
                    showSettingsBackup = false
                } else if (selectedScreen != NavigationScreen.Home) {
                    selectedScreen = NavigationScreen.Home
                } else {
                    activity.finish()
                }
            }
            val saveableStateHolder = rememberSaveableStateHolder()
            Box(modifier = Modifier.padding(padding)) {
                if (showLocalHttpSettings) {
                    LocalHttpSettingsScreen(onBack = { showLocalHttpSettings = false })
                } else if (showNotificationsSettings) {
                    NotificationsScreen(onBack = { showNotificationsSettings = false })
                } else if (showSettingsBackup) {
                    SettingsBackupScreen(onBack = { showSettingsBackup = false })
                } else {
                    saveableStateHolder.SaveableStateProvider(selectedScreen.toString()) {
                        when (selectedScreen) {
                            is NavigationScreen.Home -> ConfigurationScreen(
                                activity = activity,
                                permissionLauncher = permissionLauncher,
                                hasPermissions = hasPermissions,
                                grantedPermissionsSet = grantedPermissionsSet,
                                sdkStatus = sdkStatus,
                                onOpenLocalHttpSettings = { showLocalHttpSettings = true }
                            )
                            is NavigationScreen.Webhooks -> WebhooksScreen(
                                onOpenNotificationsSettings = { showNotificationsSettings = true }
                            )
                            is NavigationScreen.Logs -> LogsScreen()
                            is NavigationScreen.About -> AboutScreen(
                                onRestartOnboarding = onRestartOnboarding,
                                onOpenLocalHttpSettings = { showLocalHttpSettings = true },
                                onOpenNotificationsSettings = { showNotificationsSettings = true },
                                onOpenSettingsBackup = { showSettingsBackup = true }
                            )
                        }
                    }
                }
            }
        }
    }
}