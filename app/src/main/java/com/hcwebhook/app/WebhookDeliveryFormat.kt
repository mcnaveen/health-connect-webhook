package com.hcwebhook.app

import kotlinx.serialization.Serializable

@Serializable
enum class WebhookDeliveryFormat {
    /** HTTP POST with application/json (default). */
    JSON,
    /** gRPC unary Deliver(HealthPayload) using the published .proto schema. */
    GRPC
}
