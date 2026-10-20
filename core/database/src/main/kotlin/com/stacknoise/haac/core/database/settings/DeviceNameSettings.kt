package com.stacknoise.haac.core.database.settings

import android.os.Build
import androidx.datastore.core.DataStore
import java.lang.Character.CONTROL
import java.lang.Character.FORMAT
import java.lang.Character.LINE_SEPARATOR
import java.lang.Character.PARAGRAPH_SEPARATOR
import java.lang.Character.PRIVATE_USE
import java.lang.Character.SPACE_SEPARATOR
import java.lang.Character.UNASSIGNED
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/** The device name sent with a switching action, so Home Assistant can show where it came from (concept 21.5). */
object DeviceName {
    /** The longest name the bridge accepts. */
    const val MAX_LENGTH = 40

    /** Unicode categories the bridge's `isprintable` check rejects, apart from the space separators. */
    private val rejectedTypes = listOf(CONTROL, FORMAT, LINE_SEPARATOR, PARAGRAPH_SEPARATOR, PRIVATE_USE, UNASSIGNED)
        .map { it.toInt() }

    /**
     * [raw] as the bridge accepts it: trimmed, only printable characters, at most [MAX_LENGTH] characters, or null
     * if nothing is left. Characters the bridge would reject are dropped; other space characters become a space.
     */
    fun clean(raw: String): String? {
        val printable = raw.map { char ->
            when (Character.getType(char)) {
                SPACE_SEPARATOR.toInt() -> ' '
                in rejectedTypes -> null
                else -> char
            }
        }.filterNotNull().joinToString("")
        val limited = printable.trim().take(MAX_LENGTH)
        // A cut through a surrogate pair would leave half an emoji.
        return limited.trimEnd().dropLastWhile { it.isHighSurrogate() }.ifEmpty { null }
    }
}

/** *Settings → Device name* (concept 21.5): one name for all instances; not a secret. */
interface DeviceNameSettings {
    /** The stored name, or the device model until the user changes it; empty if the user chose to send none. */
    val deviceName: Flow<String>

    /** Stores [name] after [DeviceName.clean]; an empty or unusable name means no name is sent. */
    suspend fun setDeviceName(name: String)
}

/** [DeviceNameSettings] backed by the app settings DataStore. */
class DataStoreDeviceNameSettings @Inject constructor(
    private val settings: DataStore<AppSettings>,
) : DeviceNameSettings {
    /** Emits whenever the name changes; the model name stands in while nothing is stored. */
    override val deviceName: Flow<String> =
        settings.data.map { it.deviceName ?: Build.MODEL.orEmpty() }.distinctUntilChanged()

    /** Writes the cleaned name; an empty string is kept, so the default does not come back. */
    override suspend fun setDeviceName(name: String) {
        settings.updateData { it.copy(deviceName = DeviceName.clean(name).orEmpty()) }
    }
}
