package com.hcwebhook.app

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.protobuf.Duration as ProtoDuration
import com.google.protobuf.InvalidProtocolBufferException
import com.google.protobuf.Timestamp
import com.hcwebhook.app.proto.v1.HealthPayload
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Smoke test for protobuf-lite under R8 (#77).
 *
 * JVM unit tests do not apply release shrinking. Run this against a minified
 * variant so reflection over well-known type fields (`seconds_`, `nanos_`) is
 * exercised after obfuscation:
 *
 * ```
 * ./gradlew :app:connectedFossMinifyAndroidTest
 * ```
 */
@RunWith(AndroidJUnit4::class)
class ProtobufMinifiedSmokeTest {

    @Test
    fun wellKnownTypes_roundTripAfterPossibleR8() {
        val timestamp = Timestamp.newBuilder().setSeconds(1_714_000_000L).setNanos(123).build()
        val duration = ProtoDuration.newBuilder().setSeconds(3600).setNanos(0).build()

        val parsedTs = Timestamp.parseFrom(timestamp.toByteArray())
        val parsedDur = ProtoDuration.parseFrom(duration.toByteArray())

        assertEquals(1_714_000_000L, parsedTs.seconds)
        assertEquals(123, parsedTs.nanos)
        assertEquals(3600L, parsedDur.seconds)
    }

    @Test
    fun buildTestPayload_serializesAndParses() {
        val payload = ProtobufPayloadBuilder.buildTestPayload(appVersion = "r8-smoke")
        assertTrue("expected non-empty test payload", payload.serializedSize > 0)
        assertTrue(payload.stepsCount > 0)
        assertTrue(payload.getSteps(0).hasStartTime())
        assertTrue(payload.getSteps(0).hasEndTime())

        val bytes = payload.toByteArray()
        val parsed = try {
            HealthPayload.parseFrom(bytes)
        } catch (e: InvalidProtocolBufferException) {
            throw AssertionError(
                "HealthPayload.parseFrom failed after serialize — often R8 renamed " +
                    "GeneratedMessageLite fields (see proguard-rules.pro / issue #77)",
                e
            )
        }

        assertEquals(payload.appVersion, parsed.appVersion)
        assertEquals(payload.stepsCount, parsed.stepsCount)
        assertEquals(payload.getSteps(0).startTime.seconds, parsed.getSteps(0).startTime.seconds)
        assertEquals(payload.getSteps(0).endTime.nanos, parsed.getSteps(0).endTime.nanos)
    }
}
