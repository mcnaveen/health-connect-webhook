package com.hcwebhook.app

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.util.UUID
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference

data class ServerRequestLog(
    val id: String,
    val timestamp: Long,
    val method: String,
    val path: String,
    val statusCode: Int,
    val responseTimeMs: Long,
    val clientIp: String
)

object LocalHttpServerManager {
    private val serverScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val serverMutex = Mutex()
    private var serverSocket: ServerSocket? = null
    private var serverJob: Job? = null
    private var currentPort: Int? = null
    private var appContext: Context? = null
    private val latestPayload = AtomicReference<String?>()
    private val serverStartTime = AtomicLong(0L)

    private val requestLogs = mutableListOf<ServerRequestLog>()
    private const val MAX_REQUEST_LOGS = 200

    private fun recordRequestLog(log: ServerRequestLog) {
        synchronized(requestLogs) {
            requestLogs.add(0, log)
            if (requestLogs.size > MAX_REQUEST_LOGS) requestLogs.removeAt(requestLogs.size - 1)
        }
    }

    fun getRequestLogs(): List<ServerRequestLog> =
        synchronized(requestLogs) { requestLogs.toList() }

    fun clearRequestLogs() {
        synchronized(requestLogs) { requestLogs.clear() }
    }

    suspend fun syncWithPreferences(context: Context) {
        appContext = context.applicationContext
        val prefs = PreferencesManager(context)
        if (prefs.isLocalTcpEnabled()) {
            start(prefs.getLocalTcpPort())
        } else {
            stop()
        }
    }

    suspend fun start(port: Int) {
        serverMutex.withLock {
            val activeSocket = serverSocket
            if (activeSocket != null && !activeSocket.isClosed && currentPort == port) {
                return
            }
            stopLocked()

            val socket = ServerSocket(port, 50, InetAddress.getByName("0.0.0.0"))
            serverSocket = socket
            currentPort = port
            serverStartTime.set(System.currentTimeMillis())

            serverJob = serverScope.launch {
                while (isActive && !socket.isClosed) {
                    try {
                        val client = socket.accept()
                        launch { handleHttpClient(client) }
                    } catch (_: Exception) {
                        if (socket.isClosed) break
                    }
                }
            }
        }
    }

    suspend fun stop() {
        serverMutex.withLock {
            stopLocked()
        }
    }

    fun publishPayload(jsonPayload: String) {
        latestPayload.set(jsonPayload)
    }

    fun getCurrentPort(): Int? = currentPort

    private suspend fun stopLocked() {
        serverSocket?.close()
        serverSocket = null
        currentPort = null
        serverStartTime.set(0L)
        serverJob?.cancelAndJoin()
        serverJob = null
    }

    private suspend fun handleHttpClient(socket: Socket) {
        val startTime = System.currentTimeMillis()
        val clientIp = socket.inetAddress?.hostAddress ?: "unknown"
        var logMethod = ""
        var logPath = "/"
        var logStatus = 0

        socket.use { client ->
            try {
                client.soTimeout = 5_000
                val reader = BufferedReader(InputStreamReader(client.getInputStream()))
                val writer = BufferedWriter(OutputStreamWriter(client.getOutputStream()))

                // Local wrapper so every response also captures the status code.
                fun reply(code: Int, body: String) {
                    logStatus = code
                    writeHttpResponse(writer, code, body)
                }

                val requestLine = reader.readLine()?.trim().orEmpty()
                if (requestLine.isEmpty()) {
                    reply(400, """{"status":"error","message":"Empty request"}""")
                    return
                }

                // Parse method and path early so they are logged even for 401 responses.
                val parts = requestLine.split(" ")
                if (parts.size < 2) {
                    reply(400, """{"status":"error","message":"Invalid request line"}""")
                    return
                }
                val method = parts[0].uppercase()
                val rawPath = parts[1]
                logMethod = method
                logPath = rawPath.substringBefore("?")

                var authorizationHeader: String? = null
                var writeToken: String? = null
                var contentLength = 0
                while (true) {
                    val headerLine = reader.readLine() ?: break
                    if (headerLine.isBlank()) break
                    val lower = headerLine.lowercase()
                    when {
                        lower.startsWith("authorization:") ->
                            authorizationHeader = headerLine.substringAfter(":").trim()
                        lower.startsWith("x-write-token:") ->
                            writeToken = headerLine.substringAfter(":").trim()
                        lower.startsWith("content-length:") ->
                            contentLength = headerLine.substringAfter(":").trim().toIntOrNull() ?: 0
                    }
                }

                val ctx = appContext
                if (ctx != null) {
                    val prefs = PreferencesManager(ctx)
                    if (prefs.isLocalHttpAuthEnabled()) {
                        val expected = "Bearer ${prefs.getLocalHttpToken()}"
                        if (authorizationHeader != expected) {
                            reply(401, """{"status":"error","message":"Unauthorized"}""")
                            return
                        }
                    }
                }

                if (method == "OPTIONS") {
                    logStatus = 204
                    writeCorsPreflightResponse(writer)
                    return
                }

                val path = logPath
                val queryParams = parseQueryParams(rawPath.substringAfter("?", ""))

                when {
                    method == "GET" && path == "/ping" ->
                        reply(200, """{"status":"ok"}""")

                    method == "GET" && path == "/" -> {
                        val context = appContext ?: run {
                            reply(500, """{"status":"error","message":"Server context unavailable"}""")
                            return
                        }
                        val days = queryParams["days"]?.toIntOrNull()?.takeIf { it > 0 }
                        val syncManager = SyncManager(context)
                        val payloadResult = syncManager.getRealtimeJsonPayload(timeRangeDays = days)
                        if (payloadResult.isSuccess) {
                            reply(200, payloadResult.getOrThrow())
                        } else {
                            val msg = payloadResult.exceptionOrNull()?.message ?: "Failed to read realtime data"
                            reply(500, """{"status":"error","message":"${msg.escapeJson()}"}""")
                        }
                    }

                    method == "GET" && path == "/latest" ->
                        reply(200, latestPayload.get() ?: """{"status":"no_data"}""")

                    method == "GET" && path == "/logs" -> {
                        val context = appContext ?: run {
                            reply(500, """{"status":"error","message":"Server context unavailable"}""")
                            return
                        }
                        logStatus = handleLogsRequest(writer, context, queryParams)
                    }

                    method == "GET" && path == "/stats" -> {
                        val context = appContext ?: run {
                            reply(500, """{"status":"error","message":"Server context unavailable"}""")
                            return
                        }
                        logStatus = handleStatsRequest(writer, context)
                    }

                    method == "GET" && path == "/health" -> {
                        val context = appContext ?: run {
                            reply(500, """{"status":"error","message":"Server context unavailable"}""")
                            return
                        }
                        logStatus = handleHealthRequest(writer, context)
                    }

                    method == "GET" && path == "/server-logs" ->
                        logStatus = handleServerLogsRequest(writer, queryParams)

                    method == "POST" && path == "/sync" -> {
                        val context = appContext ?: run {
                            reply(500, """{"status":"error","message":"Server context unavailable"}""")
                            return
                        }
                        val days = queryParams["days"]?.toIntOrNull()?.takeIf { it > 0 }
                        logStatus = handleSyncRequest(writer, context, days)
                    }

                    method == "POST" && path == "/write" -> {
                        val context = appContext ?: run {
                            reply(500, """{"status":"error","message":"Server context unavailable"}""")
                            return
                        }
                        val body = readBody(reader, contentLength)
                        logStatus = handleWriteRequest(writer, context, writeToken, body)
                    }

                    else -> reply(
                        404,
                        """{"status":"error","message":"Unknown endpoint. Available: GET /, /ping, /latest, /logs, /stats, /health, /server-logs; POST /sync, /write"}"""
                    )
                }
            } catch (_: Exception) {
                // Ignore bad client connections.
            } finally {
                if (logMethod.isNotEmpty() && logStatus != 0) {
                    recordRequestLog(
                        ServerRequestLog(
                            id = UUID.randomUUID().toString(),
                            timestamp = startTime,
                            method = logMethod,
                            path = logPath,
                            statusCode = logStatus,
                            responseTimeMs = System.currentTimeMillis() - startTime,
                            clientIp = clientIp
                        )
                    )
                }
            }
        }
    }

    private fun readBody(reader: BufferedReader, contentLength: Int): String {
        if (contentLength <= 0) return ""
        val buffer = CharArray(contentLength)
        var read = 0
        while (read < contentLength) {
            val n = reader.read(buffer, read, contentLength - read)
            if (n == -1) break
            read += n
        }
        return String(buffer, 0, read)
    }

    // Direct receive: writes a posted record straight into Health Connect.
    private suspend fun handleWriteRequest(
        writer: BufferedWriter,
        context: Context,
        writeToken: String?,
        body: String
    ): Int {
        val expected = PreferencesManager(context).getWriteToken()
        if (writeToken.isNullOrBlank() || writeToken != expected) {
            writeHttpResponse(writer, 401, """{"status":"error","message":"Invalid or missing X-Write-Token"}""")
            return 401
        }

        val type: String
        val data: org.json.JSONObject
        try {
            val root = org.json.JSONObject(body)
            type = root.getString("type")
            data = root.getJSONObject("data")
        } catch (e: Exception) {
            writeHttpResponse(writer, 400, """{"status":"error","message":"Bad request: ${(e.message ?: "invalid body").escapeJson()}"}""")
            return 400
        }

        val result = try {
            WriteBackManager(context).write(type, data)
        } catch (e: Exception) {
            Result.failure(e)
        }

        return if (result.isSuccess) {
            writeHttpResponse(writer, 200, """{"status":"ok","written":"${type.escapeJson()}"}""")
            200
        } else {
            val msg = result.exceptionOrNull()?.message ?: "Write failed"
            // Bad field/parse problems are the caller's fault (400); anything else is a server/HC failure (500).
            val isClientError = result.exceptionOrNull().let {
                it is IllegalArgumentException || it is org.json.JSONException ||
                    it is java.time.format.DateTimeParseException
            }
            val code = if (isClientError) 400 else 500
            writeHttpResponse(writer, code, """{"status":"error","message":"${msg.escapeJson()}"}""")
            code
        }
    }

    private suspend fun handleSyncRequest(writer: BufferedWriter, context: Context, days: Int?): Int {
        val syncManager = SyncManager(context)
        val result = syncManager.performSync(timeRangeDays = days, syncType = "api")
        return if (result.isSuccess) {
            val syncResult = result.getOrThrow()
            val body = when (syncResult) {
                is SyncResult.NoData ->
                    """{"status":"ok","result":"no_data"}"""
                is SyncResult.NoMatchingData ->
                    """{"status":"ok","result":"no_matching_data"}"""
                is SyncResult.Success -> {
                    val counts = syncResult.syncCounts.entries.joinToString(",") { (type, count) ->
                        """"${type.name.lowercase()}":$count"""
                    }
                    """{"status":"ok","result":"success","counts":{$counts}}"""
                }
            }
            writeHttpResponse(writer, 200, body)
            200
        } else {
            val msg = result.exceptionOrNull()?.message ?: "Sync failed"
            writeHttpResponse(writer, 500, """{"status":"error","message":"${msg.escapeJson()}"}""")
            500
        }
    }

    private fun handleLogsRequest(writer: BufferedWriter, context: Context, params: Map<String, String>): Int {
        val prefs = PreferencesManager(context)
        var logs = prefs.getWebhookLogs()

        val successFilter = params["success"]
        if (successFilter != null) {
            val wantSuccess = successFilter.lowercase() == "true"
            logs = logs.filter { it.success == wantSuccess }
        }

        val dataTypeFilter = params["dataType"]
        if (dataTypeFilter != null) {
            logs = logs.filter { it.dataType?.lowercase() == dataTypeFilter.lowercase() }
        }

        val sinceFilter = params["since"]?.toLongOrNull()
        if (sinceFilter != null) {
            logs = logs.filter { it.timestamp >= sinceFilter }
        }

        val limit = params["limit"]?.toIntOrNull()?.coerceIn(1, 500) ?: 50
        logs = logs.take(limit)

        val logsJson = logs.joinToString(",") { log ->
            buildString {
                append("{")
                append(""""id":"${log.id}",""")
                append(""""timestamp":${log.timestamp},""")
                append(""""url":"${log.url.redactUrl().escapeJson()}",""")
                append(""""success":${log.success},""")
                append(""""statusCode":${log.statusCode ?: "null"},""")
                append(""""dataType":${log.dataType?.let { "\"$it\"" } ?: "null"},""")
                append(""""recordCount":${log.recordCount ?: "null"},""")
                append(""""responseTimeMs":${log.responseTimeMs ?: "null"},""")
                append(""""syncType":${log.syncType?.let { "\"$it\"" } ?: "null"},""")
                append(""""errorMessage":${log.errorMessage?.let { "\"${it.escapeJson()}\"" } ?: "null"}""")
                append("}")
            }
        }
        writeHttpResponse(writer, 200, """{"status":"ok","count":${logs.size},"logs":[$logsJson]}""")
        return 200
    }

    private fun handleStatsRequest(writer: BufferedWriter, context: Context): Int {
        val prefs = PreferencesManager(context)
        val logs = prefs.getWebhookLogs()
        val total = logs.size
        val successCount = logs.count { it.success }
        val failureCount = total - successCount
        val successRatePct = if (total > 0) (successCount.toDouble() / total * 100).toLong() else 0L
        val responseTimes = logs.mapNotNull { it.responseTimeMs }
        val avgResponseTimeMs = if (responseTimes.isEmpty()) null else responseTimes.average().toLong()
        val lastSyncTime = prefs.getLastSyncTime()

        val body = buildString {
            append("""{"status":"ok",""")
            append(""""total":$total,""")
            append(""""success":$successCount,""")
            append(""""failure":$failureCount,""")
            append(""""successRatePct":$successRatePct,""")
            append(""""avgResponseTimeMs":${avgResponseTimeMs ?: "null"},""")
            append(""""lastSyncTime":${lastSyncTime ?: "null"}""")
            append("}")
        }
        writeHttpResponse(writer, 200, body)
        return 200
    }

    private fun handleHealthRequest(writer: BufferedWriter, context: Context): Int {
        val prefs = PreferencesManager(context)
        val uptimeMs = System.currentTimeMillis() - serverStartTime.get()
        val lastSyncTime = prefs.getLastSyncTime()
        val lastSyncSummary = prefs.getLastSyncSummary()
        val logs = prefs.getWebhookLogs()
        val lastSuccessfulWebhookTime = logs.firstOrNull { it.success }?.timestamp

        val body = buildString {
            append("""{"status":"ok",""")
            append(""""serverUptimeMs":$uptimeMs,""")
            append(""""lastSyncTime":${lastSyncTime ?: "null"},""")
            append(""""lastSuccessfulWebhookTime":${lastSuccessfulWebhookTime ?: "null"},""")
            append(""""totalLogs":${logs.size},""")
            append(""""lastSyncSummary":${lastSyncSummary?.let { "\"${it.escapeJson()}\"" } ?: "null"}""")
            append("}")
        }
        writeHttpResponse(writer, 200, body)
        return 200
    }

    private fun handleServerLogsRequest(writer: BufferedWriter, params: Map<String, String>): Int {
        var logs = getRequestLogs()

        val methodFilter = params["method"]
        if (methodFilter != null) {
            logs = logs.filter { it.method.equals(methodFilter, ignoreCase = true) }
        }

        val pathFilter = params["path"]
        if (pathFilter != null) {
            logs = logs.filter { it.path == pathFilter }
        }

        val sinceFilter = params["since"]?.toLongOrNull()
        if (sinceFilter != null) {
            logs = logs.filter { it.timestamp >= sinceFilter }
        }

        val limit = params["limit"]?.toIntOrNull()?.coerceIn(1, 500) ?: 50
        logs = logs.take(limit)

        val logsJson = logs.joinToString(",") { log ->
            buildString {
                append("{")
                append(""""id":"${log.id}",""")
                append(""""timestamp":${log.timestamp},""")
                append(""""method":"${log.method}",""")
                append(""""path":"${log.path.escapeJson()}",""")
                append(""""statusCode":${log.statusCode},""")
                append(""""responseTimeMs":${log.responseTimeMs},""")
                append(""""clientIp":"${log.clientIp.escapeJson()}"""")
                append("}")
            }
        }
        writeHttpResponse(writer, 200, """{"status":"ok","count":${logs.size},"logs":[$logsJson]}""")
        return 200
    }

    private fun parseQueryParams(query: String): Map<String, String> {
        if (query.isBlank()) return emptyMap()
        return query.split("&").mapNotNull { param ->
            val eq = param.indexOf('=')
            if (eq < 0) null else param.substring(0, eq) to param.substring(eq + 1)
        }.toMap()
    }

    private fun String.escapeJson() = replace("\\", "\\\\").replace("\"", "\\\"")

    // Strips path and query so embedded API keys/tokens are never exposed over the network.
    private fun String.redactUrl(): String {
        return try {
            val uri = java.net.URI(this)
            val scheme = uri.scheme
            val host = uri.host
            if (scheme == null || host == null) "[redacted]"
            else {
                val port = uri.port
                if (port == -1) "$scheme://$host" else "$scheme://$host:$port"
            }
        } catch (_: Exception) {
            "[redacted]"
        }
    }

    private fun writeCorsPreflightResponse(writer: BufferedWriter) {
        writer.write("HTTP/1.1 204 No Content\r\n")
        writer.write("Access-Control-Allow-Origin: *\r\n")
        writer.write("Access-Control-Allow-Methods: GET, POST, OPTIONS\r\n")
        writer.write("Access-Control-Allow-Headers: Authorization, Content-Type\r\n")
        writer.write("Content-Length: 0\r\n")
        writer.write("Connection: close\r\n")
        writer.write("\r\n")
        writer.flush()
    }

    private fun writeHttpResponse(writer: BufferedWriter, statusCode: Int, body: String) {
        val statusText = when (statusCode) {
            200 -> "OK"
            204 -> "No Content"
            400 -> "Bad Request"
            401 -> "Unauthorized"
            404 -> "Not Found"
            405 -> "Method Not Allowed"
            else -> "Internal Server Error"
        }
        val versionedBody = if (body.startsWith("{") && body.endsWith("}")) {
            val inner = body.drop(1).dropLast(1)
            if (inner.isNotEmpty()) """{"app_version":"${BuildConfig.VERSION_NAME}",$inner}"""
            else """{"app_version":"${BuildConfig.VERSION_NAME}"}"""
        } else body
        val bodyBytes = versionedBody.toByteArray(Charsets.UTF_8)
        writer.write("HTTP/1.1 $statusCode $statusText\r\n")
        writer.write("Content-Type: application/json; charset=utf-8\r\n")
        writer.write("Cache-Control: no-store\r\n")
        writer.write("Access-Control-Allow-Origin: *\r\n")
        writer.write("Content-Length: ${bodyBytes.size}\r\n")
        writer.write("Connection: close\r\n")
        writer.write("\r\n")
        writer.write(versionedBody)
        writer.flush()
    }
}
