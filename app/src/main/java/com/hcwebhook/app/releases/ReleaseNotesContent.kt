package com.hcwebhook.app.releases

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.withStyle
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
                            text = styledReleaseText(line.text),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
                is ReleaseNotesFormatter.ReleaseLine.Paragraph -> {
                    Text(
                        text = styledReleaseText(line.text),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                is ReleaseNotesFormatter.ReleaseLine.Heading -> {
                    Text(
                        text = styledReleaseText(line.text),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }
                is ReleaseNotesFormatter.ReleaseLine.Table -> ReleaseTable(line)
                is ReleaseNotesFormatter.ReleaseLine.Code -> {
                    Text(
                        text = line.text,
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        softWrap = false,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun ReleaseTable(table: ReleaseNotesFormatter.ReleaseLine.Table) {
    val columns = maxOf(table.header.size, table.rows.maxOfOrNull { it.size } ?: 0)
    val divider = MaterialTheme.colorScheme.outlineVariant
    val allRows = (if (table.header.isNotEmpty()) listOf(table.header) else emptyList()) + table.rows
    val textStyle = MaterialTheme.typography.bodyMedium
    val measurer = rememberTextMeasurer()
    val firstColumnTexts = allRows.map { styledReleaseText(it.getOrElse(0) { "" }) }
    val density = LocalDensity.current
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
    ) {
        // First column holds short keys: as wide as its longest cell, so names do not
        // break, but at most half the table.
        val firstColumnWidth = with(density) {
            val widest = firstColumnTexts.maxOfOrNull { measurer.measure(it, textStyle, softWrap = false).size.width } ?: 0
            minOf(widest.toDp() + 8.dp, maxWidth / 2)
        }
        Column {
            allRows.forEachIndexed { index, row ->
                val isHeader = index == 0 && table.header.isNotEmpty()
                if (index > 0) HorizontalDivider(color = divider, thickness = 0.5.dp)
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 6.dp)) {
                    for (column in 0 until columns) {
                        Text(
                            text = styledReleaseText(row.getOrElse(column) { "" }),
                            style = textStyle,
                            fontWeight = if (isHeader) FontWeight.SemiBold else null,
                            color = if (isHeader) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = if (column == 0) {
                                Modifier.width(firstColumnWidth).padding(end = 8.dp)
                            } else {
                                Modifier.weight(1f).padding(end = 8.dp)
                            },
                        )
                    }
                }
            }
        }
    }
}

/** Draws `code` in monospace and **bold** in bold; drops the markers. */
@Composable
private fun styledReleaseText(text: String): AnnotatedString {
    val codeStyle = SpanStyle(fontFamily = FontFamily.Monospace, background = MaterialTheme.colorScheme.surfaceVariant)
    return buildAnnotatedString { appendInlineMarkup(text, codeStyle) }
}

private fun AnnotatedString.Builder.appendInlineMarkup(text: String, codeStyle: SpanStyle) {
    var rest = text
    while (rest.isNotEmpty()) {
        val match = INLINE_MARKUP.find(rest)
        if (match == null) {
            append(rest)
            return
        }
        append(rest.substring(0, match.range.first))
        val (code, bold) = match.destructured
        if (code.isNotEmpty()) {
            withStyle(codeStyle) { append(code) }
        } else {
            // Bold text can hold `code`.
            withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) { appendInlineMarkup(bold, codeStyle) }
        }
        rest = rest.substring(match.range.last + 1)
    }
}

private val INLINE_MARKUP = Regex("""`([^`]+)`|\*\*(.+?)\*\*""")

private fun formatReleaseDate(iso: String): String {
    return try {
        val instant = Instant.parse(iso)
        val formatter = DateTimeFormatter.ofPattern("MMMM d, yyyy", Locale.getDefault())
        formatter.format(instant.atZone(ZoneId.systemDefault()))
    } catch (_: Exception) {
        iso.take(10)
    }
}
