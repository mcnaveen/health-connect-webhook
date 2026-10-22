package com.hcwebhook.app

import java.io.File
import java.security.MessageDigest
import java.util.Locale

/**
 * Payloads that one webhook did not get while the sync still went through for another webhook.
 *
 * A sync moves its timestamps and Changes tokens when at least one webhook gets the data, so a
 * webhook that failed would never get that data. Its payloads wait here, one file each, and go
 * out in order before the next sync sends that webhook anything new.
 *
 * Plain files, so the logic runs in JVM unit tests.
 */
class WebhookOutbox(
    private val dir: File,
    private val maxBytesPerWebhook: Long = MAX_BYTES_PER_WEBHOOK,
) {
    /** One stored payload: JSON text or protobuf bytes, with the number of items it holds. */
    data class Item(val file: File, val format: WebhookDeliveryFormat, val itemCount: Int)

    private var sequence = 0

    /**
     * Adds a payload at the end of [url]'s queue. When the queue is larger than
     * [maxBytesPerWebhook], the oldest payloads are dropped; returns how many.
     */
    fun add(url: String, format: WebhookDeliveryFormat, payload: ByteArray, itemCount: Int): Int {
        val queue = File(dir, keyOf(url)).apply { mkdirs() }
        val name = String.format(
            Locale.ROOT, "%013d-%04d-%d.%s",
            System.currentTimeMillis(), sequence++ % 10_000, itemCount, extensionOf(format),
        )
        val temp = File(queue, "$name.tmp")
        temp.writeBytes(payload)
        check(temp.renameTo(File(queue, name))) { "Could not store payload for $url" }
        return trim(queue)
    }

    /** [url]'s payloads, oldest first. */
    fun items(url: String): List<Item> =
        File(dir, keyOf(url)).listFiles().orEmpty()
            .mapNotNull { file -> parse(file) }
            .sortedBy { it.file.name }

    fun hasItems(url: String): Boolean = items(url).isNotEmpty()

    fun read(item: Item): ByteArray = item.file.readBytes()

    fun remove(item: Item) {
        item.file.delete()
    }

    /** Drops the queues of webhooks that are not in [urls], e.g. after a webhook was removed. */
    fun keepOnly(urls: Collection<String>) {
        val keep = urls.map(::keyOf).toSet()
        dir.listFiles().orEmpty().filter { it.isDirectory && it.name !in keep }.forEach { it.deleteRecursively() }
    }

    private fun trim(queue: File): Int {
        val files = queue.listFiles().orEmpty().filter { parse(it) != null }.sortedBy { it.name }
        var total = files.sumOf { it.length() }
        var dropped = 0
        // Keep the newest payload even when it alone is over the limit.
        for (file in files.dropLast(1)) {
            if (total <= maxBytesPerWebhook) break
            total -= file.length()
            file.delete()
            dropped++
        }
        return dropped
    }

    private fun parse(file: File): Item? {
        val match = NAME.matchEntire(file.name) ?: return null
        val format = when (match.groupValues[2]) {
            "json" -> WebhookDeliveryFormat.JSON
            "pb" -> WebhookDeliveryFormat.GRPC
            else -> return null
        }
        return Item(file, format, match.groupValues[1].toInt())
    }

    private fun extensionOf(format: WebhookDeliveryFormat) = when (format) {
        WebhookDeliveryFormat.JSON -> "json"
        WebhookDeliveryFormat.GRPC -> "pb"
    }

    private fun keyOf(url: String): String =
        MessageDigest.getInstance("SHA-256").digest(url.toByteArray())
            .take(16).joinToString("") { String.format(Locale.ROOT, "%02x", it) }

    companion object {
        /** About two full 25,000-item payloads of heart rate per webhook. */
        const val MAX_BYTES_PER_WEBHOOK = 16L * 1024 * 1024

        private val NAME = Regex("""\d{13}-\d{4}-(\d+)\.(json|pb)""")
    }
}
