package com.stacknoise.haac.app

import android.graphics.Color
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.fragment.app.FragmentActivity
import com.stacknoise.haac.app.navigation.HaacNavHost
import com.stacknoise.haac.core.common.ui.theme.HaacTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * The single activity of the app; hosts the Compose navigation graph (concept 3). A [FragmentActivity],
 * because AndroidX `BiometricPrompt` needs one (concept 5.4).
 */
@AndroidEntryPoint
class MainActivity : FragmentActivity() {
    /** Enables edge-to-edge drawing and sets the themed Compose content. */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Light theme: dark status and navigation bar icons on the transparent bars.
        val bars = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
        enableEdgeToEdge(statusBarStyle = bars, navigationBarStyle = bars)
        setContent {
            HaacTheme {
                HaacNavHost()
            }
        }
    }
}
