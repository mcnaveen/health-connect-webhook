package com.hcwebhook.app

import org.junit.Assert.assertEquals
import org.junit.Test

class LocalHttpAppVersionTest {

    @Test
    fun addsVersionToObjectsWithoutIt() {
        assertEquals("""{"app_version":"1.9.26","status":"ok"}""", withAppVersion("""{"status":"ok"}""", "1.9.26"))
        assertEquals("""{"app_version":"1.9.26"}""", withAppVersion("{}", "1.9.26"))
    }

    @Test
    fun keepsSyncPayloadThatHasVersion() {
        val payload = """{"timestamp":"2026-10-22T16:39:44Z","app_version":"1.9.26","steps":[]}"""

        assertEquals(payload, withAppVersion(payload, "1.9.26"))
    }

    @Test
    fun leavesNonObjectsAlone() {
        assertEquals("[1,2]", withAppVersion("[1,2]", "1.9.26"))
    }
}
