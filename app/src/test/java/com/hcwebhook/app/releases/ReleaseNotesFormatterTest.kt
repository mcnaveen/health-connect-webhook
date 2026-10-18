package com.hcwebhook.app.releases

import org.junit.Assert.assertEquals
import org.junit.Test

class ReleaseNotesFormatterTest {

    @Test
    fun parseBody_dedupesRepeatedBullets_withWindowsLineEndings() {
        val body = """
            ## What's Changed
            * Enhance LocalHttpServerManager with request logging and log filtering by @mcnaveen in https://github.com/mcnaveen/health-connect-webhook/pull/37


            **Full Changelog**: https://github.com/mcnaveen/health-connect-webhook/compare/v1.9.5...v1.9.6

            ## What's Changed
            * Enhance LocalHttpServerManager with request logging and log filtering by @mcnaveen in https://github.com/mcnaveen/health-connect-webhook/pull/37


            **Full Changelog**: https://github.com/mcnaveen/health-connect-webhook/compare/v1.9.5...v1.9.6
        """.trimIndent().replace("\n", "\r\n")

        val lines = ReleaseNotesFormatter.parseBody(body)
        assertEquals(1, lines.size)
        assertEquals(
            "Enhance LocalHttpServerManager with request logging and log filtering",
            (lines[0] as ReleaseNotesFormatter.ReleaseLine.Bullet).text,
        )
    }

    @Test
    fun parseBody_stripsInlineClosesReference() {
        val body = """
            ## What's Changed
            * Fix webhook retry (Closes #10)
        """.trimIndent()

        val lines = ReleaseNotesFormatter.parseBody(body)
        assertEquals(1, lines.size)
        assertEquals(
            "Fix webhook retry",
            (lines[0] as ReleaseNotesFormatter.ReleaseLine.Bullet).text,
        )
    }

    @Test
    fun parseBody_keepsHeadingsCodeAndTables() {
        val body = """
            ## Sync: edited records

            **New `metadata` fields (JSON):**

            | Field | Description |
            |-------|-------------|
            | `id` | Health Connect record id |
            | `last_modified_time` | When Health Connect last changed the record |

            ```json
            "deleted": [
              { "type": "nutrition", "id": "a1" }
            ]
            ```

            ## What's Changed
            * Implement Changes API support by @mcnaveen in https://github.com/mcnaveen/health-connect-webhook/pull/90
        """.trimIndent()

        val lines = ReleaseNotesFormatter.parseBody(body)

        assertEquals(
            listOf(
                ReleaseNotesFormatter.ReleaseLine.Heading("Sync: edited records"),
                ReleaseNotesFormatter.ReleaseLine.Paragraph("**New `metadata` fields (JSON):**"),
                ReleaseNotesFormatter.ReleaseLine.Table(
                    header = listOf("Field", "Description"),
                    rows = listOf(
                        listOf("`id`", "Health Connect record id"),
                        listOf("`last_modified_time`", "When Health Connect last changed the record"),
                    ),
                ),
                ReleaseNotesFormatter.ReleaseLine.Code("\"deleted\": [\n  { \"type\": \"nutrition\", \"id\": \"a1\" }\n]"),
                ReleaseNotesFormatter.ReleaseLine.Bullet("Implement Changes API support"),
            ),
            lines,
        )
    }

    @Test
    fun parseBody_skipsClosesOnlyLine() {
        val body = """
            ## What's Changed
            * Fix webhook retry
            (Closes #10)
        """.trimIndent()

        val lines = ReleaseNotesFormatter.parseBody(body)
        assertEquals(1, lines.size)
    }
}
