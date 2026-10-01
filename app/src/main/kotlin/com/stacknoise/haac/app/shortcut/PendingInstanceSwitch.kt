package com.stacknoise.haac.app.shortcut

import android.content.Context
import android.content.Intent
import com.stacknoise.haac.app.MainActivity
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * The instance an app shortcut asked for (concept 4.4). The request waits here until the main area is visible,
 * so it also survives the unlock screen after a cold start.
 */
@Singleton
class PendingInstanceSwitch @Inject constructor() {
    private val requested = MutableStateFlow<String?>(null)

    /** The instance to switch to, or null when nothing is pending. */
    val serverId: StateFlow<String?> = requested

    /** Asks for a switch to instance [serverId]. */
    fun request(serverId: String) {
        requested.value = serverId
    }

    /** The pending switch was taken over. */
    fun clear() {
        requested.value = null
    }

    /** Intent action and extra of the shortcut intents. */
    companion object {
        private const val ACTION = "com.stacknoise.haac.action.SWITCH_INSTANCE"
        private const val EXTRA_SERVER_ID = "server_id"

        /** The intent of the shortcut for instance [serverId]. */
        fun intent(context: Context, serverId: String): Intent =
            Intent(context, MainActivity::class.java).setAction(ACTION).putExtra(EXTRA_SERVER_ID, serverId)

        /** The instance a launch [intent] asks for, or null for a normal start. */
        fun serverIdOf(intent: Intent?): String? =
            intent?.takeIf { it.action == ACTION }?.getStringExtra(EXTRA_SERVER_ID)
    }
}
