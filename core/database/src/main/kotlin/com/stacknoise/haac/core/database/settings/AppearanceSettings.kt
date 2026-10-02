package com.stacknoise.haac.core.database.settings

import androidx.datastore.core.DataStore
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable

/** Which design the app shows: the system's choice, or always light or dark (concept 15.2). */
@Serializable
enum class ThemeMode {
    /** Follows the dark mode setting of the phone. */
    SYSTEM,

    /** Always the light "Salbei" design. */
    LIGHT,

    /** Always the dark design. */
    DARK,
}

/** Settings → Appearance (concept 15.2): the design mode, for all instances. */
interface AppearanceSettings {
    /** The chosen mode; [ThemeMode.SYSTEM] until the user picks another. */
    val themeMode: Flow<ThemeMode>

    /** Stores the mode. */
    suspend fun setThemeMode(mode: ThemeMode)
}

/** [AppearanceSettings] backed by the app settings DataStore. */
class DataStoreAppearanceSettings @Inject constructor(
    private val settings: DataStore<AppSettings>,
) : AppearanceSettings {
    /** Emits whenever the mode changes. */
    override val themeMode: Flow<ThemeMode> = settings.data.map { it.themeMode }.distinctUntilChanged()

    /** Writes the mode. */
    override suspend fun setThemeMode(mode: ThemeMode) {
        settings.updateData { it.copy(themeMode = mode) }
    }
}
