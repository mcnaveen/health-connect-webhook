package com.hcwebhook.app.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.hcwebhook.app.LocalFeedbackEntry
import com.hcwebhook.app.PreferencesManager
import com.hcwebhook.app.FeedbackSubmitter
import com.hcwebhook.app.R
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date
import java.util.UUID

private const val FEEDBACK_PORTAL_URL = "https://hc-webhook.feedbackjar.com/"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedbackSheet(
    visible: Boolean,
    onDismiss: () -> Unit,
    onSubmitted: () -> Unit = {},
) {
    if (!visible) return

    val context = LocalContext.current
    val prefs = remember { PreferencesManager(context) }
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var feedbackText by remember { mutableStateOf("") }
    var submitting by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var localFeedback by remember { mutableStateOf(prefs.getLocalFeedback()) }

    val genericError = stringResource(R.string.feedback_error)
    val rateLimitMessage = stringResource(R.string.feedback_rate_limited)
    val dateFormat = remember { DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT) }

    LaunchedEffect(visible) {
        if (visible) localFeedback = prefs.getLocalFeedback()
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        windowInsets = WindowInsets(0),
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                ) {
                    Text(
                        text = stringResource(R.string.feedback_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                ) {
                    Text(
                        text = stringResource(R.string.feedback_submit_title),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            item {
                OutlinedTextField(
                    value = feedbackText,
                    onValueChange = {
                        feedbackText = it
                        errorMessage = null
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    placeholder = { Text(stringResource(R.string.feedback_hint)) },
                    minLines = 4,
                    enabled = !submitting,
                    isError = errorMessage != null,
                    supportingText = errorMessage?.let { { Text(it) } },
                )
            }
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.End,
                ) {
                    Button(
                        onClick = {
                            val content = feedbackText.trim()
                            if (content.isEmpty()) return@Button
                            scope.launch {
                                submitting = true
                                errorMessage = null
                                FeedbackSubmitter.submit(context, content)
                                    .onSuccess { response ->
                                        prefs.addLocalFeedback(
                                            LocalFeedbackEntry(
                                                id = UUID.randomUUID().toString(),
                                                content = content,
                                                submittedAt = System.currentTimeMillis(),
                                                postId = response.postId,
                                                title = response.title,
                                                type = response.type,
                                            ),
                                        )
                                        localFeedback = prefs.getLocalFeedback()
                                        feedbackText = ""
                                        onSubmitted()
                                    }
                                    .onFailure { error ->
                                        errorMessage = if (error.message?.contains("429") == true) {
                                            rateLimitMessage
                                        } else {
                                            genericError
                                        }
                                    }
                                submitting = false
                            }
                        },
                        enabled = feedbackText.isNotBlank() && !submitting,
                    ) {
                        if (submitting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary,
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text(
                            if (submitting) {
                                stringResource(R.string.feedback_submitting)
                            } else {
                                stringResource(R.string.feedback_submit)
                            },
                        )
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            context.startActivity(
                                Intent(Intent.ACTION_VIEW, Uri.parse(FEEDBACK_PORTAL_URL)),
                            )
                        }
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = stringResource(R.string.feedback_view_all),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }

            if (localFeedback.isNotEmpty()) {
                item {
                    HorizontalDivider(modifier = Modifier.padding(top = 4.dp))
                }
                item {
                    Text(
                        text = stringResource(R.string.feedback_history_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                    )
                }
                items(localFeedback, key = { it.id }) { entry ->
                    LocalFeedbackCard(
                        entry = entry,
                        dateFormat = dateFormat,
                        modifier = Modifier.fillParentMaxWidth(),
                    )
                }
            }
        }
    }
}

@Composable
private fun LocalFeedbackCard(
    entry: LocalFeedbackEntry,
    dateFormat: DateFormat,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraSmall,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = entry.title?.takeIf { it.isNotBlank() } ?: entry.content,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth(),
            )
            if (entry.title != null && entry.content != entry.title) {
                Text(
                    text = entry.content,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Text(
                text = dateFormat.format(Date(entry.submittedAt)),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
