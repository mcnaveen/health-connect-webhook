package com.hcwebhook.app

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Guards the R8 keep rule required by protobuf-javalite (#77).
 * Runtime failure only shows up on minified release builds.
 */
class ProguardProtobufRulesTest {

    @Test
    fun keepsGeneratedMessageLiteForProtobufLiteReflection() {
        val rules = File("proguard-rules.pro").takeIf { it.isFile }
            ?: File("app/proguard-rules.pro")
        assertTrue("missing ${rules.absolutePath}", rules.isFile)

        val text = rules.readText()
        assertTrue(
            "proguard-rules.pro must keep GeneratedMessageLite subclasses " +
                "(protobuf-lite reflects on field names like seconds_)",
            text.contains("-keep class * extends com.google.protobuf.GeneratedMessageLite")
        )
        assertTrue(
            "do not allowobfuscation on GeneratedMessageLite — it breaks MessageSchema",
            !text.contains(
                Regex("""-keep,\s*allowobfuscation\s+class\s+\*\s+extends\s+com\.google\.protobuf\.GeneratedMessageLite""")
            )
        )
    }
}
