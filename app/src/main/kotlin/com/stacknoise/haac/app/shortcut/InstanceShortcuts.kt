package com.stacknoise.haac.app.shortcut

import android.content.Context
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import com.stacknoise.haac.R
import com.stacknoise.haac.core.common.dispatch.IoDispatcher
import com.stacknoise.haac.core.database.server.ServerDao
import com.stacknoise.haac.core.database.server.ServerEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Publishes one app shortcut per instance (long press on the app icon) from two instances on, as the instance
 * switcher does (concept 4.4). A shortcut opens the app on that instance.
 */
@Singleton
class InstanceShortcuts @Inject constructor(
    @ApplicationContext private val context: Context,
    private val servers: ServerDao,
    private val switches: PendingInstanceSwitch,
    @param:IoDispatcher private val io: CoroutineDispatcher,
) {
    /** Keeps the shortcuts in step with the stored instances while [scope] lives. */
    fun start(scope: CoroutineScope) {
        scope.launch {
            // A sync touches the `server` row without changing a name; only a new, renamed or removed instance counts.
            servers.observeAll().instanceLabels().collect { withContext(io) { publish(it) } }
        }
    }

    /** Replaces the dynamic shortcuts with the first instances that fit; none for a single instance. */
    private suspend fun publish(rows: List<InstanceLabel>) {
        if (rows.size < MIN_INSTANCES) {
            ShortcutManagerCompat.removeAllDynamicShortcuts(context)
            return
        }
        val shortcuts = rows.take(ShortcutManagerCompat.getMaxShortcutCountPerActivity(context)).map { row ->
            ShortcutInfoCompat.Builder(context, "instance-${row.id}")
                .setShortLabel(row.name)
                .setLongLabel(row.name)
                .setIcon(IconCompat.createWithResource(context, R.mipmap.ic_launcher))
                .setIntent(switches.intent(row.id))
                .build()
        }
        ShortcutManagerCompat.setDynamicShortcuts(context, shortcuts)
    }

    /** Shortcuts only help when there is something to switch between. */
    private companion object {
        const val MIN_INSTANCES = 2
    }
}

/** What a shortcut shows of an instance. */
internal data class InstanceLabel(val id: String, val name: String)

/** The labels of the instances, emitted only when an instance was added, renamed or removed. */
internal fun Flow<List<ServerEntity>>.instanceLabels(): Flow<List<InstanceLabel>> =
    map { rows -> rows.map { InstanceLabel(it.id, it.displayName) } }.distinctUntilChanged()
