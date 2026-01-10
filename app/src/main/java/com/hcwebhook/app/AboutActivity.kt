package com.hcwebhook.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hcwebhook.app.ui.theme.HCWebhookTheme

class AboutActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            HCWebhookTheme {
                AboutScreen()
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun AboutScreen() {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("About") },
                    navigationIcon = {
                        IconButton(onClick = { finish() }) {
                            Text("←")
                        }
                    }
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Card {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "HC Webhook",
                            style = MaterialTheme.typography.headlineMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "A Health Connect webhook integration app",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Card {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Description",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "HC Webhook allows you to sync your Health Connect data to custom webhooks. " +
                            "Configure your webhook URLs, select data types, and set sync intervals to " +
                            "automatically send your health data to your endpoints.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                Card {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Features",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "• Sync multiple health data types\n" +
                            "• Multiple webhook URL support\n" +
                            "• Configurable sync intervals\n" +
                            "• Manual sync option\n" +
                            "• Webhook logging and monitoring",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                Card {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Data Types",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Steps, Sleep, Heart Rate, Distance, Calories, Weight, Height, " +
                            "Blood Pressure, Blood Glucose, Oxygen Saturation, Body Temperature, " +
                            "Respiratory Rate, Resting Heart Rate, Exercise, Hydration, Nutrition",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}

