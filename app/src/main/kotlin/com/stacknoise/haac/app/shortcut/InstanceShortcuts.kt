package com.stacknoise.haac.app.shortcut

import android.content.Context
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import com.stacknoise.haac.R
import com.stacknoise.haac.core.database.server.ServerDao
import com.stacknoise.haac.core.database.server.ServerEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Publishes one app shortcut per instance (long press on the app icon) from two instances on, as the instance
 * switcher does (concept 4.4). A shortcut opens the app on that instance.
 */
@Singleton
class InstanceShortcuts @Inject constructor(
    @ApplicationContext private val context: Context,
    private val servers: ServerDao,
) {
    /** Keeps the shortcuts in step with the stored instances while [scope] lives. */
    fun start(scope: CoroutineScope) {
        scope.launch { servers.observeAll().collect { publish(it) } }
    }

    /** Replaces the dynamic shortcuts with the first instances that fit; none for a single instance. */
    private fun publish(rows: List<ServerEntity>) {
        if (rows.size < MIN_INSTANCES) {
            ShortcutManagerCompat.removeAllDynamicShortcuts(context)
            return
        }
        val shortcuts = rows.take(ShortcutManagerCompat.getMaxShortcutCountPerActivity(context)).map { row ->
            ShortcutInfoCompat.Builder(context, "instance-${row.id}")
                .setShortLabel(row.displayName)
                .setLongLabel(row.displayName)
                .setIcon(IconCompat.createWithResource(context, R.mipmap.ic_launcher))
                .setIntent(PendingInstanceSwitch.intent(context, row.id))
                .build()
        }
        ShortcutManagerCompat.setDynamicShortcuts(context, shortcuts)
    }

    /** Shortcuts only help when there is something to switch between. */
    private companion object {
        const val MIN_INSTANCES = 2
    }
}
