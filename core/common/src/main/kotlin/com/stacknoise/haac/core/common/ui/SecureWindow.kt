package com.stacknoise.haac.core.common.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext

/** Keeps the current screen out of screenshots and the recent-apps preview while it is shown (concept 5.5). */
@Composable
fun SecureWindow() {
    val window = LocalContext.current.findActivity()?.window
    DisposableEffect(window) {
        window?.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        onDispose { window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE) }
    }
}

/** The activity behind this context, unwrapping context wrappers; e.g. for the fingerprint prompt. */
tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
