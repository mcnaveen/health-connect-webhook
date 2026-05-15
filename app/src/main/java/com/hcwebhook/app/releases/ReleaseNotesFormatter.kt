package com.hcwebhook.app.releases

/**
 * Mirrors [changelog.tsx](https://github.com/mcnaveen/hc-webhook-site) body parsing:
 * cutoff at "## New Contributors", drop Full Changelog / Closes lines, then per-line clean().
 */
object ReleaseNotesFormatter {

    sealed interface ReleaseLine {
        data class Bullet(val text: String) : ReleaseLine
        data class Paragraph(val text: String) : ReleaseLine
    }

    fun normalizeTag(tag: String): String =
        tag.removePrefix("v").removePrefix("V").trim()

    fun matchesVersion(tag: String, versionName: String): Boolean =
        normalizeTag(tag) == versionName.trim()

    fun parseBody(body: String?): List<ReleaseLine> {
        if (body.isNullOrBlank()) return emptyList()

        val normalized = body.replace("\r\n", "\n").replace("\r", "\n")

        val cutoff = normalized.indexOf("## New Contributors")
        val cleaned = (if (cutoff != -1) normalized.substring(0, cutoff) else normalized)
            .lineSequence()
            .map { it.trim() }
            .filter { line ->
                line.isNotEmpty() &&
                    !line.startsWith("**Full Changelog**") &&
                    !isClosesReferenceLine(line)
            }
            .joinToString("\n")
            .trim()

        if (cleaned.isEmpty()) return emptyList()

        val lines = mutableListOf<ReleaseLine>()
        val seen = mutableSetOf<String>()

        for (line in cleaned.lines()) {
            val entry = when {
                line.startsWith("## ") -> null
                line.startsWith("* ") || line.startsWith("- ") -> {
                    clean(line.drop(2)).takeIf { it.isNotEmpty() }?.let { ReleaseLine.Bullet(it) }
                }
                else -> clean(line).takeIf { it.isNotEmpty() && !isFullChangelogLine(it) }
                    ?.let { ReleaseLine.Paragraph(it) }
            } ?: continue

            val key = dedupKey(entry)
            if (!seen.add(key)) continue
            lines.add(entry)
        }
        return lines
    }

    /** Same rules as changelog.tsx `clean()`. */
    fun clean(text: String): String = text
        .replace("\r", "")
        .replace(Regex("\\[([^\\]]+)]\\([^)]+\\)")) { it.groupValues[1] }
        .replace(Regex("\\*\\*(.*?)\\*\\*")) { it.groupValues[1] }
        .replace(Regex("\\s+by\\s+@\\S+.*", RegexOption.IGNORE_CASE), "")
        .replace(Regex("https?://\\S+"), "")
        .replace(CLOSES_INLINE, "")
        .replace(Regex("\\s+"), " ")
        .trim()

    private val CLOSES_INLINE = Regex("""\s*\(Closes\s+#\d+\)""", RegexOption.IGNORE_CASE)

    private fun isClosesReferenceLine(line: String): Boolean {
        val trimmed = line.trim()
        if (trimmed.startsWith("Closes ", ignoreCase = true)) return true
        return CLOSES_LINE_ONLY.matches(trimmed)
    }

    private val CLOSES_LINE_ONLY =
        Regex("""^\(Closes\s+#\d+\)\s*$""", RegexOption.IGNORE_CASE)

    private fun isFullChangelogLine(text: String): Boolean =
        text.startsWith("Full Changelog", ignoreCase = true)

    private fun dedupKey(line: ReleaseLine): String = when (line) {
        is ReleaseLine.Bullet -> line.text
        is ReleaseLine.Paragraph -> line.text
    }.lowercase()
}
