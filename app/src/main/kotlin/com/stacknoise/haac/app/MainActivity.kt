package com.stacknoise.haac.app

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.fragment.app.FragmentActivity
import com.stacknoise.haac.app.navigation.HaacNavHost
import com.stacknoise.haac.app.shortcut.PendingInstanceSwitch
import com.stacknoise.haac.core.common.ui.theme.HaacTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * The single activity of the app; hosts the Compose navigation graph (concept 3). A [FragmentActivity],
 * because AndroidX `BiometricPrompt` needs one (concept 5.4).
 */
@AndroidEntryPoint
class MainActivity : FragmentActivity() {
    @Inject
    lateinit var pendingSwitch: PendingInstanceSwitch

    /** Enables edge-to-edge drawing and sets the themed Compose content. */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Light theme: dark status and navigation bar icons on the transparent bars.
        val bars = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
        enableEdgeToEdge(statusBarStyle = bars, navigationBarStyle = bars)
        // A recreated activity gets the same intent again; only the first start counts as a request.
        if (savedInstanceState == null) requestSwitchOf(intent)
        setContent {
            HaacTheme {
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
        PendingInstanceSwitch.serverIdOf(intent)?.let(pendingSwitch::request)
    }
}
