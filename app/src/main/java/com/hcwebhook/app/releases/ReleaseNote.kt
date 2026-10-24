package com.hcwebhook.app.releases

import android.content.Context

/** One version's notes. [date] is `yyyy-MM-dd`; [body] is the markdown that [ReleaseNotesFormatter] reads. */
data class ReleaseNote(
    val version: String,
    val date: String,
    val body: String,
)

/**
 * Release notes bundled in the app (`assets/release_notes.txt`), newest first.
 * Each version starts with a `=== <version> <yyyy-MM-dd>` line. Nothing is fetched.
 */
object ReleaseNotes {

    fun load(context: Context): List<ReleaseNote> = try {
        context.assets.open(ASSET).bufferedReader().use { parse(it.readText()) }
    } catch (_: Exception) {
        emptyList()
    }

    fun findForVersion(context: Context, versionName: String): ReleaseNote? =
        load(context).firstOrNull { it.version == versionName.trim() }

    fun parse(text: String): List<ReleaseNote> {
        val notes = mutableListOf<ReleaseNote>()
        var version: String? = null
        var date = ""
        val body = StringBuilder()
        fun flush() {
            version?.let { notes.add(ReleaseNote(it, date, body.toString().trim())) }
            body.clear()
        }
        for (line in text.replace("\r\n", "\n").lines()) {
            if (line.startsWith(HEADER)) {
                flush()
                val parts = line.removePrefix(HEADER).trim().split(" ", limit = 2)
                version = parts[0]
                date = parts.getOrElse(1) { "" }.trim()
            } else {
                body.append(line).append('\n')
            }
        }
        flush()
        return notes
    }

    private const val ASSET = "release_notes.txt"
    private const val HEADER = "=== "
}
