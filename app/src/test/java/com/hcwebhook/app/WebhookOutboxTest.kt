package com.hcwebhook.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class WebhookOutboxTest {

    @get:Rule
    val folder = TemporaryFolder()

    private val a = "https://a.example/hook"
    private val b = "https://b.example/hook"

    @Test
    fun keepsPayloadsInOrderPerWebhook() {
        val outbox = WebhookOutbox(folder.root)
        outbox.add(a, WebhookDeliveryFormat.JSON, "first".toByteArray(), 3)
        outbox.add(a, WebhookDeliveryFormat.JSON, "second".toByteArray(), 2)
        outbox.add(b, WebhookDeliveryFormat.GRPC, byteArrayOf(1, 2), 1)

        val items = outbox.items(a)
        assertEquals(listOf("first", "second"), items.map { String(outbox.read(it)) })
        assertEquals(listOf(3, 2), items.map { it.itemCount })
        assertEquals(WebhookDeliveryFormat.GRPC, outbox.items(b).single().format)
    }

    @Test
    fun removeAndKeepOnly() {
        val outbox = WebhookOutbox(folder.root)
        outbox.add(a, WebhookDeliveryFormat.JSON, "x".toByteArray(), 1)
        outbox.add(b, WebhookDeliveryFormat.JSON, "y".toByteArray(), 1)

        outbox.remove(outbox.items(a).single())
        assertFalse(outbox.hasItems(a))

        outbox.keepOnly(listOf(a))
        assertFalse(outbox.hasItems(b))
    }

    @Test
    fun dropsOldestWhenFullButKeepsNewest() {
        val outbox = WebhookOutbox(folder.root, maxBytesPerWebhook = 10)
        outbox.add(a, WebhookDeliveryFormat.JSON, ByteArray(6), 1)
        val dropped = outbox.add(a, WebhookDeliveryFormat.JSON, ByteArray(6), 2)

        assertEquals(1, dropped)
        assertEquals(listOf(2), outbox.items(a).map { it.itemCount })

        // A payload over the limit on its own still stays.
        outbox.add(a, WebhookDeliveryFormat.JSON, ByteArray(20), 3)
        assertEquals(listOf(3), outbox.items(a).map { it.itemCount })
        assertTrue(outbox.hasItems(a))
    }
}
