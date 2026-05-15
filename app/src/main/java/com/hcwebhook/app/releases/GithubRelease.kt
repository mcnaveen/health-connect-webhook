package com.hcwebhook.app.releases

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GithubRelease(
    @SerialName("tag_name") val tagName: String,
    val name: String,
    @SerialName("published_at") val publishedAt: String,
    val body: String? = null,
)
