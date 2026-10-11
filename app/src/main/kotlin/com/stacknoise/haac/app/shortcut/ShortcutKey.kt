package com.stacknoise.haac.app.shortcut

import android.content.Context
import androidx.core.content.edit
import androidx.datastore.core.DataStore
import com.stacknoise.haac.core.database.settings.AppSettings
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first

/**
 * The random key of this installation that app shortcut intents carry (review S-07), kept in the app settings
 * DataStore. A key from the earlier `SharedPreferences` file is taken over once, so shortcuts that are already
 * pinned keep working, and then removed from there.
 */
@Singleton
class ShortcutKey internal constructor(
    private val settings: DataStore<AppSettings>,
    private val legacy: () -> String?,
    private val removeLegacy: () -> Unit,
) {
    /** Reads the key of the earlier `SharedPreferences` file. */
    @Inject
    constructor(settings: DataStore<AppSettings>, @ApplicationContext context: Context) : this(
        settings,
        { context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, null) },
        { context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit { remove(KEY) } },
    )

    /** The key; created on first use. */
    suspend fun get(): String {
        settings.data.first().shortcutKey?.let { return it }
        val stored = settings.updateData { current ->
            if (current.shortcutKey != null) current else current.copy(shortcutKey = legacy() ?: newKey())
        }
        removeLegacy()
        return checkNotNull(stored.shortcutKey)
    }

    /** A new random key. */
    private fun newKey(): String = UUID.randomUUID().toString()

    /** Name of the earlier preferences file and its entry. */
    private companion object {
        const val PREFS = "shortcut"
        const val KEY = "shortcut_key"
    }
}
