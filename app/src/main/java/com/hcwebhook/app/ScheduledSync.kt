package com.hcwebhook.app

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class ScheduledSync(
    val id: String = UUID.randomUUID().toString(),
    val hour: Int,
    val minute: Int,
    val label: String = "",
    val enabled: Boolean = true
) {
    fun getDisplayTime(): String {
        return String.format("%02d:%02d", hour, minute)
    }
    
    fun getDisplayLabel(): String {
        return if (label.isNotBlank()) label else getDisplayTime()
    }
    
    companion object {
        fun create(hour: Int, minute: Int, label: String = "", enabled: Boolean = true): ScheduledSync {
            return ScheduledSync(
                id = UUID.randomUUID().toString(),
                hour = hour,
                minute = minute,
                label = label,
                enabled = enabled
            )
        }
    }
}
