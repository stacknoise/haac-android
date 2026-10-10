package com.stacknoise.haac.core.common.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.Window
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import java.util.WeakHashMap

/**
 * Keeps the current screen out of screenshots and the recent-apps preview while it is shown (concept 5.5).
 * The flag stays until the last secure screen of the window is gone: during a transition the new screen is
 * composed before the old one is disposed, and the old one must not clear the flag of the new one (review S-09).
 */
@Composable
fun SecureWindow() {
    val window = LocalContext.current.findActivity()?.window
    DisposableEffect(window) {
        val screens = window?.let(SecureScreens::of)
        if (screens?.enter() == true) window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        onDispose {
            if (screens?.leave() == true) window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }
}

/** Counts the secure screens shown in one window; used on the main thread only. */
internal class SecureScreens {
    private var count = 0

    /** Registers a screen; true if it is the first one, so the flag has to be set. */
    fun enter(): Boolean = ++count == 1

    /** Unregisters a screen; true if it was the last one, so the flag has to be cleared. */
    fun leave(): Boolean = count > 0 && --count == 0

    /** The counters of the windows. */
    companion object {
        private val byWindow = WeakHashMap<Window, SecureScreens>()

        /** The counter of [window]. */
        fun of(window: Window): SecureScreens = byWindow.getOrPut(window) { SecureScreens() }
    }
}

/** The activity behind this context, unwrapping context wrappers; e.g. for the fingerprint prompt. */
tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
