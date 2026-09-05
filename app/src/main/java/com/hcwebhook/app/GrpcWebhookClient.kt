package com.hcwebhook.app

import android.content.Context
import com.hcwebhook.app.proto.v1.HealthPayload
import com.hcwebhook.app.proto.v1.HealthWebhookGrpc
import io.grpc.ManagedChannel
import io.grpc.Metadata
import io.grpc.Status
import io.grpc.StatusRuntimeException
import io.grpc.android.AndroidChannelBuilder
import io.grpc.stub.MetadataUtils
import java.io.IOException
import java.net.URI
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.UUID
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLException
import kotlin.math.pow
/**
 * Delivers a HealthPayload to a user-configured gRPC endpoint via
 * HealthWebhook.Deliver.
 *
 * Accepted target forms in [WebhookConfig.url]:
 * - host:port
 * - https://host[:port]  (TLS, default port 443)
 * - http://host[:port]   (plaintext, default port 80)
 *
 * Server must return DeliverResponse.ok = true.
 */
object GrpcWebhookClient {

    private const val TIMEOUT_SECONDS = 60L
    private const val MAX_RETRIES = 3
    private const val INITIAL_RETRY_DELAY_MS = 1000L
    private const val MAX_PAYLOAD_CHARS = 8000

    /**
     * @param logPayload optional human-readable JSON of the same health data for the
     * webhook log UI. Wire delivery still uses [payload] (protobuf). When null, the log
     * stores a short count summary only.
     */
    suspend fun deliver(
        config: WebhookConfig,
        payload: HealthPayload,
        context: Context,
        dataType: String? = null,
        recordCount: Int? = null,
        syncType: String? = null,
        logPayload: String? = null
    ): Result<Unit> {
        val timestamp = System.currentTimeMillis()
        var lastException: Exception? = null
        var errorMessage: String? = null
        val loggedPayload = logPayload ?: payloadSummary(payload)

        return try {
            val target = parseTarget(config.url)
            for (attempt in 1..MAX_RETRIES) {
                var channel: ManagedChannel? = null
                try {
                    channel = buildChannel(target, context)
                    val stub = HealthWebhookGrpc.newBlockingStub(channel)
                        .withDeadlineAfter(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                        .withInterceptors(MetadataUtils.newAttachHeadersInterceptor(metadataFrom(config)))

                    val response = stub.deliver(payload)
                    if (response.ok) {
                        logCall(
                            context, config.url, timestamp, 200, true, null,
                            System.currentTimeMillis() - timestamp, dataType, recordCount, syncType,
                            loggedPayload
                        )
                        return Result.success(Unit)
                    }
                    val msg = response.message.ifBlank { "gRPC Deliver returned ok=false" }
                    lastException = IOException(msg)
                    errorMessage = msg
                } catch (e: StatusRuntimeException) {
                    lastException = e
                    errorMessage = "gRPC ${e.status.code}: ${e.status.description ?: e.message}"
                    if (!isRetryableGrpc(e.status)) break
                } catch (e: IOException) {
                    lastException = e
                    errorMessage = e.message
                    if (!isRetryableIo(e)) break
                } finally {
                    channel?.shutdownNow()
                }

                if (attempt < MAX_RETRIES) {
                    val delayMs = INITIAL_RETRY_DELAY_MS * (2.0.pow(attempt - 1).toLong())
                    kotlinx.coroutines.delay(delayMs)
                }
            }

            logCall(
                context, config.url, timestamp, null, false, errorMessage,
                System.currentTimeMillis() - timestamp, dataType, recordCount, syncType,
                loggedPayload
            )
            Result.failure(lastException ?: IOException("gRPC deliver failed"))
        } catch (e: Exception) {
            logCall(
                context, config.url, timestamp, null, false, e.message,
                System.currentTimeMillis() - timestamp, dataType, recordCount, syncType,
                loggedPayload
            )
            Result.failure(e)
        }
    }

    internal data class GrpcTarget(
        val host: String,
        val port: Int,
        val useTls: Boolean
    )

    internal fun parseTarget(raw: String): GrpcTarget {
        val trimmed = raw.trim()
        require(trimmed.isNotEmpty()) { "gRPC target is empty" }

        if (trimmed.startsWith("http://", ignoreCase = true) ||
            trimmed.startsWith("https://", ignoreCase = true)
        ) {
            val uri = URI(trimmed)
            val host = uri.host ?: throw IllegalArgumentException("Invalid gRPC URL: missing host")
            val useTls = trimmed.startsWith("https://", ignoreCase = true)
            val port = when {
                uri.port != -1 -> uri.port
                useTls -> 443
                else -> 80
            }
            return GrpcTarget(host, port, useTls)
        }

        val parts = trimmed.split(":")
        return when (parts.size) {
            1 -> GrpcTarget(parts[0], 443, useTls = true)
            2 -> {
                val port = parts[1].toIntOrNull()
                    ?: throw IllegalArgumentException("Invalid gRPC port in: $trimmed")
                val useTls = port == 443
                GrpcTarget(parts[0], port, useTls)
            }
            else -> throw IllegalArgumentException("Invalid gRPC target: $trimmed")
        }
    }

    internal fun isValidTarget(raw: String): Boolean = try {
        parseTarget(raw)
        true
    } catch (_: Exception) {
        false
    }

    private fun buildChannel(target: GrpcTarget, context: Context): ManagedChannel {
        val builder = AndroidChannelBuilder.forAddress(target.host, target.port)
            .context(context.applicationContext)
        if (!target.useTls) {
            builder.usePlaintext()
        }
        return builder.build()
    }

    private fun metadataFrom(config: WebhookConfig): Metadata {
        val metadata = Metadata()
        config.headers.forEach { (key, value) ->
            if (key.isBlank()) return@forEach
            val asciiKey = Metadata.Key.of(key.lowercase(), Metadata.ASCII_STRING_MARSHALLER)
            metadata.put(asciiKey, value)
        }
        return metadata
    }

    private fun payloadSummary(payload: HealthPayload): String =
        "grpc HealthPayload bytes=${payload.serializedSize} types=${payloadCountSummary(payload)}"

    private fun payloadCountSummary(payload: HealthPayload): String = buildString {
        fun add(name: String, count: Int) {
            if (count > 0) {
                if (isNotEmpty()) append(',')
                append(name).append('=').append(count)
            }
        }
        add("steps", payload.stepsCount)
        add("sleep", payload.sleepCount)
        add("heart_rate", payload.heartRateCount)
        add("hrv", payload.heartRateVariabilityCount)
        add("distance", payload.distanceCount)
        add("exercise", payload.exerciseCount)
    }.ifBlank { "empty" }

    private fun isRetryableGrpc(status: Status): Boolean = when (status.code) {
        Status.Code.UNAVAILABLE, Status.Code.DEADLINE_EXCEEDED, Status.Code.RESOURCE_EXHAUSTED -> true
        else -> false
    }

    private fun isRetryableIo(e: IOException): Boolean = when (e) {
        is SocketTimeoutException, is UnknownHostException -> true
        is SSLException -> false
        else -> true
    }

    private fun logCall(
        context: Context,
        url: String,
        timestamp: Long,
        statusCode: Int?,
        success: Boolean,
        errorMessage: String?,
        responseTimeMs: Long,
        dataType: String?,
        recordCount: Int?,
        syncType: String?,
        payload: String
    ) {
        PreferencesManager(context).addWebhookLog(
            WebhookLog(
                id = UUID.randomUUID().toString(),
                timestamp = timestamp,
                url = url,
                statusCode = statusCode,
                success = success,
                errorMessage = errorMessage,
                dataType = dataType,
                recordCount = recordCount,
                responseTimeMs = responseTimeMs,
                syncType = syncType,
                payload = payload.take(MAX_PAYLOAD_CHARS),
                deliveryFormat = WebhookLog.FORMAT_GRPC
            )
        )
    }
}
