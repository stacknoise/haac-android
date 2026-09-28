package com.stacknoise.haac.core.database.settings

import androidx.datastore.core.DataStore
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/** Settings → Security (concept 5.4, 5.5): unlock window and app lock timeout, for all instances. */
interface SecuritySettings {
    /** Unlock window in minutes; 0 means one fingerprint per unlock. */
    val unlockWindowMinutes: Flow<Int>

    /** Minutes in the background after which the app locks again; 0 means at once. */
    val lockTimeoutMinutes: Flow<Int>

    /** Stores the unlock window; the caller has re-keyed the tokens before. */
    suspend fun setUnlockWindowMinutes(minutes: Int)

    /** Stores the lock timeout. */
    suspend fun setLockTimeoutMinutes(minutes: Int)

    /** Choices offered in the settings. */
    companion object {
        /** Unlock window choices of concept 5.4. */
        val UNLOCK_WINDOW_CHOICES = listOf(0, 1, 5, 15)

        /** Lock timeout choices; 5 minutes is the default of concept 5.5. */
        val LOCK_TIMEOUT_CHOICES = listOf(0, 1, 5, 15, 30)

        /** Lock timeout before the user changes it. */
        const val DEFAULT_LOCK_TIMEOUT_MINUTES = 5
    }
}

/** [SecuritySettings] backed by the app settings DataStore. */
class DataStoreSecuritySettings @Inject constructor(
    private val settings: DataStore<AppSettings>,
) : SecuritySettings {
    /** Emits whenever the window changes. */
    override val unlockWindowMinutes: Flow<Int> = settings.data.map { it.unlockWindowMinutes }.distinctUntilChanged()

    /** Emits whenever the timeout changes. */
    override val lockTimeoutMinutes: Flow<Int> = settings.data.map { it.lockTimeoutMinutes }.distinctUntilChanged()

    /** Writes the window. */
    override suspend fun setUnlockWindowMinutes(minutes: Int) {
        settings.updateData { it.copy(unlockWindowMinutes = minutes) }
    }

    /** Writes the timeout. */
    override suspend fun setLockTimeoutMinutes(minutes: Int) {
        settings.updateData { it.copy(lockTimeoutMinutes = minutes) }
    }
}
