package com.example.antimarcipan.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/** Prosté podržení po [durationMs] → akce (ignoruje swipe/zrušení). */
fun Modifier.longHold(durationMs: Long, onHold: () -> Unit): Modifier =
    this.pointerInput(Unit) {
        awaitEachGesture {
            awaitFirstDown(requireUnconsumed = false)
            val completed = withTimeoutOrNull(durationMs) {
                waitForUpOrCancellation(); true
            }
            if (completed == null) onHold()
        }
    }

/**
 * Obalí obsah gestem: krátké ťuknutí → onTap, podržení [holdMs] → onHold.
 * Během držení hlásí průběh 0..1 přes onProgress (pro plnící kolečko).
 */
@Composable
fun Holdable(
    holdMs: Long,
    onTap: () -> Unit,
    onHold: () -> Unit,
    onProgress: (Float) -> Unit = {},
    content: @Composable () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val progress = remember { Animatable(0f) }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.pointerInput(Unit) {
            awaitEachGesture {
                awaitFirstDown(requireUnconsumed = false)
                var fired = false
                val job = scope.launch {
                    progress.snapTo(0f)
                    try {
                        progress.animateTo(
                            1f,
                            tween(holdMs.toInt(), easing = LinearEasing),
                        ) { onProgress(value) }
                        fired = true
                        onProgress(0f)
                        onHold()
                    } catch (_: CancellationException) {
                        onProgress(0f)
                    }
                }
                val up = waitForUpOrCancellation()
                if (!fired && progress.value < 1f) {
                    job.cancel()
                    onProgress(0f)
                    if (up != null) onTap()
                }
            }
        },
    ) { content() }
}
