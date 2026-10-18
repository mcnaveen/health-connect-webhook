package com.hcwebhook.app.releases

/**
 * Mirrors [changelog.tsx](https://github.com/mcnaveen/hc-webhook-site) body parsing:
 * cutoff at "## New Contributors", drop Full Changelog / Closes lines, then per-line clean().
 * Also keeps headings, code blocks and tables, which hand-written notes use.
 * Inline `code` and **bold** stay in the text; [styledReleaseText] draws them.
 */
object ReleaseNotesFormatter {

    sealed interface ReleaseLine {
        data class Bullet(val text: String) : ReleaseLine
        data class Paragraph(val text: String) : ReleaseLine
        data class Heading(val text: String) : ReleaseLine
        data class Code(val text: String) : ReleaseLine
        /** [header] is empty when the table has no `|---|` line. */
        data class Table(val header: List<String>, val rows: List<List<String>>) : ReleaseLine
    }

    fun normalizeTag(tag: String): String =
        tag.removePrefix("v").removePrefix("V").trim()

    fun matchesVersion(tag: String, versionName: String): Boolean =
        normalizeTag(tag) == versionName.trim()

    fun parseBody(body: String?): List<ReleaseLine> {
        if (body.isNullOrBlank()) return emptyList()

        val normalized = body.replace("\r\n", "\n").replace("\r", "\n")
        val cutoff = normalized.indexOf("## New Contributors")
        val rawLines = (if (cutoff != -1) normalized.substring(0, cutoff) else normalized).lines()

        val lines = mutableListOf<ReleaseLine>()
        val seen = mutableSetOf<String>()
        fun add(entry: ReleaseLine?) {
            if (entry == null) return
            // Code blocks and tables can repeat on purpose; dedupe only text lines.
            if (entry !is ReleaseLine.Code && entry !is ReleaseLine.Table && !seen.add(dedupKey(entry))) return
            lines.add(entry)
        }

        var i = 0
        while (i < rawLines.size) {
            val line = rawLines[i].trim()
            when {
                line.startsWith("```") -> {
                    val code = mutableListOf<String>()
                    i++
                    while (i < rawLines.size && !rawLines[i].trim().startsWith("```")) {
                        code.add(rawLines[i].trimEnd())
                        i++
                    }
                    add(code.joinToString("\n").trimIndent().takeIf { it.isNotBlank() }?.let { ReleaseLine.Code(it) })
                }
                line.startsWith("|") -> {
                    val table = mutableListOf<String>()
                    while (i < rawLines.size && rawLines[i].trim().startsWith("|")) {
                        table.add(rawLines[i].trim())
                        i++
                    }
                    add(parseTable(table))
                    continue
                }
                line.isEmpty() || line.startsWith("**Full Changelog**") || isClosesReferenceLine(line) -> Unit
                line.startsWith("#") -> {
                    val text = clean(line.trimStart('#'))
                    if (text.isNotEmpty() && !text.equals("What's Changed", ignoreCase = true)) {
                        add(ReleaseLine.Heading(text))
                    }
                }
                line.startsWith("* ") || line.startsWith("- ") ->
                    add(clean(line.drop(2)).takeIf { it.isNotEmpty() }?.let { ReleaseLine.Bullet(it) })
                else ->
                    add(clean(line).takeIf { it.isNotEmpty() && !isFullChangelogLine(it) }?.let { ReleaseLine.Paragraph(it) })
            }
            i++
        }
        return lines
    }

    /** The row above the `|---|` line is the header. */
    private fun parseTable(rows: List<String>): ReleaseLine? {
        val cells = rows.map { row -> row.trim('|').split("|").map { clean(it) } }
        val separator = cells.indexOfFirst { row -> row.all { it.matches(TABLE_SEPARATOR) } }
        val header = if (separator > 0) cells[separator - 1] else emptyList()
        val body = (if (separator >= 0) cells.drop(separator + 1) else cells)
            .filter { row -> row.any { it.isNotEmpty() } }
        if (header.isEmpty() && body.isEmpty()) return null
        return ReleaseLine.Table(header, body)
    }

    private val TABLE_SEPARATOR = Regex("""^:?-+:?$""")

    /** Same rules as changelog.tsx `clean()`, except **bold** stays for [styledReleaseText]. */
    fun clean(text: String): String = text
        .replace("\r", "")
        .replace(Regex("\\[([^\\]]+)]\\([^)]+\\)")) { it.groupValues[1] }
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
        is ReleaseLine.Heading -> "# " + line.text
        is ReleaseLine.Code -> line.text
        is ReleaseLine.Table -> (listOf(line.header) + line.rows).joinToString("\n") { it.joinToString("|") }
    }.lowercase()
}
