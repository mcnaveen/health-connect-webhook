package com.hcwebhook.app

import kotlinx.serialization.Serializable

@Serializable
data class LocalFeedbackEntry(
    val id: String,
    val content: String,
    val submittedAt: Long,
    val postId: String? = null,
    val title: String? = null,
    val type: String? = null,
)
