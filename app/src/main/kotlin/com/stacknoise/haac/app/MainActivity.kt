package com.stacknoise.haac.app

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.fragment.app.FragmentActivity
import com.stacknoise.haac.app.navigation.HaacNavHost
import com.stacknoise.haac.app.shortcut.PendingInstanceSwitch
import com.stacknoise.haac.core.common.ui.theme.DarkHaacPalette
import com.stacknoise.haac.core.common.ui.theme.HaacTheme
import com.stacknoise.haac.core.common.ui.theme.LightHaacPalette
import com.stacknoise.haac.core.database.settings.AppearanceSettings
import com.stacknoise.haac.core.database.settings.ThemeMode
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

/**
 * The single activity of the app; hosts the Compose navigation graph (concept 3). A [FragmentActivity],
 * because AndroidX `BiometricPrompt` needs one (concept 5.4).
 */
@AndroidEntryPoint
class MainActivity : FragmentActivity() {
    @Inject
    lateinit var pendingSwitch: PendingInstanceSwitch

    @Inject
    lateinit var appearance: AppearanceSettings

    /** Enables edge-to-edge drawing and sets the themed Compose content. */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // A recreated activity gets the same intent again; only the first start counts as a request.
        if (savedInstanceState == null) requestSwitchOf(intent)
        // The stored mode is read once before the first frame, so a chosen design never flashes the other one.
        val firstMode = runBlocking { appearance.themeMode.first() }
        setContent {
            val mode by appearance.themeMode.collectAsState(initial = firstMode)
            val dark = mode == ThemeMode.DARK || (mode == ThemeMode.SYSTEM && isSystemInDarkTheme())
            // Transparent bars with icons that stay readable on the design in use.
            DisposableEffect(dark) {
                val bars = if (dark) {
                    SystemBarStyle.dark(Color.TRANSPARENT)
                } else {
                    SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
                }
                enableEdgeToEdge(statusBarStyle = bars, navigationBarStyle = bars)
                onDispose {}
            }
            HaacTheme(palette = if (dark) DarkHaacPalette else LightHaacPalette) {
                HaacNavHost()
            }
        }
    }

    /** An app shortcut that reaches the running app asks for its instance. */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        requestSwitchOf(intent)
    }

    /** Passes the instance of a shortcut [intent] on to the main area. */
    private fun requestSwitchOf(intent: Intent?) {
        pendingSwitch.serverIdOf(intent)?.let(pendingSwitch::request)
    }
}
