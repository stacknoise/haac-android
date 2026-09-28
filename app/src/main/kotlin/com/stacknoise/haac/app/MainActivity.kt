package com.stacknoise.haac.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.stacknoise.haac.app.navigation.HaacNavHost
import com.stacknoise.haac.core.common.ui.theme.HaacTheme
import dagger.hilt.android.AndroidEntryPoint

/** The single activity of the app; hosts the Compose navigation graph (concept 3). */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    /** Enables edge-to-edge drawing and sets the themed Compose content. */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HaacTheme {
                HaacNavHost()
            }
        }
    }
}
