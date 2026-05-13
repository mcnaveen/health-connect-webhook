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
import java.util.concurrent.atomic.AtomicReference

object LocalHttpServerManager {
    private val serverScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val serverMutex = Mutex()
    private var serverSocket: ServerSocket? = null
    private var serverJob: Job? = null
    private var currentPort: Int? = null
    private var appContext: Context? = null
    private val latestPayload = AtomicReference<String?>()

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

            serverJob = serverScope.launch {
                while (isActive && !socket.isClosed) {
                    try {
                        val client = socket.accept()
                        launch {
                            handleHttpClient(client)
                        }
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
        serverJob?.cancelAndJoin()
        serverJob = null
    }

    private suspend fun handleHttpClient(socket: Socket) {
        socket.use { client ->
            try {
                client.soTimeout = 5_000
                val reader = BufferedReader(InputStreamReader(client.getInputStream()))
                val writer = BufferedWriter(OutputStreamWriter(client.getOutputStream()))

                val requestLine = reader.readLine()?.trim().orEmpty()
                if (requestLine.isEmpty()) {
                    writeHttpResponse(writer, 400, """{"status":"error","message":"Empty request"}""")
                    return
                }

                var authorizationHeader: String? = null
                while (true) {
                    val headerLine = reader.readLine() ?: break
                    if (headerLine.isBlank()) break
                    val lower = headerLine.lowercase()
                    if (lower.startsWith("authorization:")) {
                        authorizationHeader = headerLine.substringAfter(":").trim()
                    }
                }

                val ctx = appContext
                if (ctx != null) {
                    val prefs = PreferencesManager(ctx)
                    if (prefs.isLocalHttpAuthEnabled()) {
                        val expected = "Bearer ${prefs.getLocalHttpToken()}"
                        if (authorizationHeader != expected) {
                            writeHttpResponse(writer, 401, """{"status":"error","message":"Unauthorized"}""")
                            return
                        }
                    }
                }

                val parts = requestLine.split(" ")
                if (parts.size < 2) {
                    writeHttpResponse(writer, 400, """{"status":"error","message":"Invalid request line"}""")
                    return
                }

                val method = parts[0].uppercase()
                val rawPath = parts[1]
                if (method != "GET") {
                    writeHttpResponse(writer, 405, """{"status":"error","message":"Only GET is supported"}""")
                    return
                }

                val path = rawPath.substringBefore("?")
                val query = rawPath.substringAfter("?", "")

                when (path) {
                    "/ping" -> writeHttpResponse(writer, 200, """{"status":"ok"}""")
                    "/" -> {
                        val days = parseDays(query)
                        val context = appContext
                        if (context == null) {
                            writeHttpResponse(writer, 500, """{"status":"error","message":"Server context unavailable"}""")
                            return
                        }
                        val syncManager = SyncManager(context)
                        val payloadResult = syncManager.getRealtimeJsonPayload(timeRangeDays = days)
                        if (payloadResult.isSuccess) {
                            writeHttpResponse(writer, 200, payloadResult.getOrThrow())
                        } else {
                            val msg = payloadResult.exceptionOrNull()?.message ?: "Failed to read realtime data"
                            writeHttpResponse(writer, 500, """{"status":"error","message":"$msg"}""")
                        }
                    }
                    "/latest" -> {
                        val response = latestPayload.get() ?: """{"status":"no_data"}"""
                        writeHttpResponse(writer, 200, response)
                    }
                    else -> writeHttpResponse(
                        writer,
                        404,
                        """{"status":"error","message":"Use / for data or /ping"}"""
                    )
                }
            } catch (_: Exception) {
                // Ignore bad client connections
            }
        }
    }

    private fun parseDays(query: String): Int? {
        if (query.isBlank()) return null
        val param = query.split("&").firstOrNull { it.startsWith("days=") } ?: return null
        val value = param.substringAfter("days=", "")
        return value.toIntOrNull()?.takeIf { it > 0 }
    }

    private fun writeHttpResponse(writer: BufferedWriter, statusCode: Int, body: String) {
        val statusText = when (statusCode) {
            200 -> "OK"
            400 -> "Bad Request"
            401 -> "Unauthorized"
            404 -> "Not Found"
            405 -> "Method Not Allowed"
            else -> "Internal Server Error"
        }
        val bodyBytes = body.toByteArray(Charsets.UTF_8)
        writer.write("HTTP/1.1 $statusCode $statusText\r\n")
        writer.write("Content-Type: application/json; charset=utf-8\r\n")
        writer.write("Cache-Control: no-store\r\n")
        writer.write("Content-Length: ${bodyBytes.size}\r\n")
        writer.write("Connection: close\r\n")
        writer.write("\r\n")
        writer.write(body)
        writer.flush()
    }
}
