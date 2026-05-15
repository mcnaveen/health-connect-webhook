package com.hcwebhook.app.releases

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun ReleaseArticle(
    release: GithubRelease,
    modifier: Modifier = Modifier,
) {
    val lines = ReleaseNotesFormatter.parseBody(release.body)
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = release.tagName,
                style = MaterialTheme.typography.labelMedium,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = formatReleaseDate(release.publishedAt),
                style = MaterialTheme.typography.labelSmall,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (release.name.isNotBlank() && release.name != release.tagName) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = release.name,
                style = MaterialTheme.typography.titleSmall,
                fontFamily = FontFamily.Monospace,
            )
        }
        if (lines.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            ReleaseBodyContent(lines)
        }
    }
}

@Composable
fun ReleaseBodyContent(lines: List<ReleaseNotesFormatter.ReleaseLine>) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        lines.forEach { line ->
            when (line) {
                is ReleaseNotesFormatter.ReleaseLine.Bullet -> {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Box(
                            modifier = Modifier
                                .padding(top = 8.dp, end = 10.dp)
                                .size(4.dp)
                                .clip(CircleShape)
                                .background(Color(0x66E11D48)),
                        )
                        Text(
                            text = line.text,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
                is ReleaseNotesFormatter.ReleaseLine.Paragraph -> {
                    Text(
                        text = line.text,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

private fun formatReleaseDate(iso: String): String {
    return try {
        val instant = Instant.parse(iso)
        val formatter = DateTimeFormatter.ofPattern("MMMM d, yyyy", Locale.getDefault())
        formatter.format(instant.atZone(ZoneId.systemDefault()))
    } catch (_: Exception) {
        iso.take(10)
    }
}
