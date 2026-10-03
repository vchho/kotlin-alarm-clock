package com.example.clockalarmapp

import android.os.SystemClock
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel

/** One recorded lap. [lapMillis] is the time since the previous lap; [totalMillis] is the overall time. */
data class Lap(
    val number: Int,
    val lapMillis: Long,
    val totalMillis: Long
)

/**
 * Holds the timer (stopwatch) state so it survives tab switches and rotation.
 * Uses elapsedRealtime, which is monotonic and keeps counting while the screen is off.
 */
class TimerViewModel : ViewModel() {

    var isRunning by mutableStateOf(false)
        private set

    /** Total elapsed time, refreshed by [tick] while running. */
    var elapsedMillis by mutableLongStateOf(0L)
        private set

    /** Newest lap first. */
    var laps by mutableStateOf<List<Lap>>(emptyList())
        private set

    private var accumulatedMillis = 0L
    private var startedAt = 0L

    fun start() {
        if (isRunning) return
        startedAt = SystemClock.elapsedRealtime()
        isRunning = true
    }

    fun stop() {
        if (!isRunning) return
        accumulatedMillis += SystemClock.elapsedRealtime() - startedAt
        elapsedMillis = accumulatedMillis
        isRunning = false
    }

    fun toggle() = if (isRunning) stop() else start()

    /** Call regularly (e.g. every frame) while running to refresh [elapsedMillis]. */
    fun tick() {
        if (isRunning) {
            elapsedMillis = accumulatedMillis + (SystemClock.elapsedRealtime() - startedAt)
        }
    }

    fun lap() {
        if (!isRunning) return
        tick()
        val total = elapsedMillis
        val previousTotal = laps.firstOrNull()?.totalMillis ?: 0L
        laps = listOf(Lap(laps.size + 1, total - previousTotal, total)) + laps
    }

    /** Clears the time and laps. Only allowed while stopped. */
    fun reset() {
        if (isRunning) return
        accumulatedMillis = 0L
        elapsedMillis = 0L
        laps = emptyList()
    }
}
