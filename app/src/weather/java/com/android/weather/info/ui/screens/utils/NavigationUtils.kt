package com.android.weather.info.ui.screens.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay

/**
 * Returns `true` for the duration of the NavHost slide transition (350ms),
 * then flips to `false` so screens can defer rendering heavy composables
 * until the animation has settled — exactly what professional apps do.
 *
 * Usage:
 *   val isNavigating = rememberIsNavigating()
 *   if (isNavigating) { GhostLoader() } else { RealContent() }
 */
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.runtime.DisposableEffect

@Composable
fun rememberIsNavigating(transitionDurationMs: Long = 350L): Boolean {
    var isNavigating by remember { mutableStateOf(true) }
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_START) {
                isNavigating = true
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(isNavigating) {
        if (isNavigating) {
            delay(transitionDurationMs)
            isNavigating = false
        }
    }
    
    return isNavigating
}
