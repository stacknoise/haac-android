package com.stacknoise.haac.app.shortcut

import android.content.Context
import android.content.Intent
import androidx.core.content.edit
import com.stacknoise.haac.app.MainActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * The instance an app shortcut asked for (concept 4.4). The request waits here until the main area is visible,
 * so it also survives the unlock screen after a cold start. The shortcut intents carry a random key that only
 * this app knows, so another app cannot switch the instance through the exported activity (review S-07).
 */
@Singleton
class PendingInstanceSwitch @Inject constructor(@ApplicationContext private val context: Context) {
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

    /** The intent of the shortcut for instance [serverId]. */
    fun intent(serverId: String): Intent = Intent(context, MainActivity::class.java)
        .setAction(ACTION)
        .putExtra(EXTRA_SERVER_ID, serverId)
        .putExtra(EXTRA_KEY, key())

    /** The instance a launch [intent] asks for, or null for a normal start or a foreign intent. */
    fun serverIdOf(intent: Intent?): String? = intent
        ?.takeIf { it.action == ACTION && it.getStringExtra(EXTRA_KEY) == key() }
        ?.getStringExtra(EXTRA_SERVER_ID)

    /** The random key of this installation; created on first use. */
    @Synchronized
    private fun key(): String {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return prefs.getString(EXTRA_KEY, null) ?: UUID.randomUUID().toString().also {
            prefs.edit { putString(EXTRA_KEY, it) }
        }
    }

    /** Intent action and extras of the shortcut intents. */
    private companion object {
        const val ACTION = "com.stacknoise.haac.action.SWITCH_INSTANCE"
        const val EXTRA_SERVER_ID = "server_id"
        const val EXTRA_KEY = "shortcut_key"
        const val PREFS = "shortcut"
    }
}
