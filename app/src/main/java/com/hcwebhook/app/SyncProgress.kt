package com.hcwebhook.app

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Progress of the running manual sync, for the progress bar on the home screen.
 *
 * A manual sync of several days runs one day at a time ([window] of [windows]). Inside a day it
 * first reads each enabled data type, then sends the payload parts.
 */
object SyncProgress {
    enum class Phase { READING, SENDING }

    data class State(
        val window: Int = 1,
        val windows: Int = 1,
        val phase: Phase = Phase.READING,
        val type: HealthDataType? = null,
        val typeIndex: Int = 0,
        val typeCount: Int = 0,
        val part: Int = 0,
        val parts: Int = 0,
    ) {
        /** 0..1 over the whole sync. Reading takes the first 80% of a day, sending the rest. */
        val fraction: Float
            get() {
                val inWindow = when (phase) {
                    Phase.READING -> if (typeCount > 0) 0.8f * typeIndex / typeCount else 0f
                    Phase.SENDING -> 0.8f + if (parts > 0) 0.2f * (part - 1).coerceAtLeast(0) / parts else 0f
                }
                return ((window - 1) + inWindow) / windows.coerceAtLeast(1)
            }
    }

    private val _state = MutableStateFlow<State?>(null)
    val state: StateFlow<State?> = _state

    fun start(windows: Int) {
        _state.value = State(window = 1, windows = windows)
    }

    fun window(window: Int) {
        _state.value = _state.value?.copy(window = window, phase = Phase.READING, type = null, typeIndex = 0, part = 0, parts = 0)
    }

    fun reading(type: HealthDataType, index: Int, count: Int) {
        _state.value = _state.value?.copy(phase = Phase.READING, type = type, typeIndex = index, typeCount = count)
    }

    fun sending(part: Int, parts: Int) {
        _state.value = _state.value?.copy(phase = Phase.SENDING, part = part, parts = parts)
    }

    fun clear() {
        _state.value = null
    }
}
