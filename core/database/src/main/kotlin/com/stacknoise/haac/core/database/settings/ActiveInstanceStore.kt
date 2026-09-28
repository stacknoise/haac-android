package com.stacknoise.haac.core.database.settings

import androidx.datastore.core.DataStore
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/** Remembers which instance is active (`activeServerId`, concept 4.4, 12). */
interface ActiveInstanceStore {
    /** The id of the active instance, or null before the first sign-in. */
    val activeServerId: Flow<String?>

    /** Makes [serverId] the active instance. */
    suspend fun setActive(serverId: String?)
}

/** [ActiveInstanceStore] backed by the app settings DataStore. */
class DataStoreActiveInstanceStore @Inject constructor(
    private val settings: DataStore<AppSettings>,
) : ActiveInstanceStore {
    /** Emits whenever the active instance changes. */
    override val activeServerId: Flow<String?> = settings.data.map { it.activeServerId }.distinctUntilChanged()

    /** Writes the new id. */
    override suspend fun setActive(serverId: String?) {
        settings.updateData { it.copy(activeServerId = serverId) }
    }
}
