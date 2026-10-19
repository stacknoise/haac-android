package com.stacknoise.haac.feature.schedules.data

import com.stacknoise.haac.core.network.connection.LiveConnection
import com.stacknoise.haac.core.network.connection.requireOpen
import com.stacknoise.haac.core.network.http.decodeOrUnexpected
import com.stacknoise.haac.feature.schedules.domain.ScheduleDraft
import com.stacknoise.haac.feature.schedules.domain.isoEpochMillis
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.json.Json

/** Asks the bridge for the next runs of a draft that is not saved yet (`schedules/preview`, concept 19.10). */
@Singleton
class SchedulePreviews @Inject constructor(private val live: LiveConnection, private val json: Json) {
    /**
     * The next runs of [draft] in milliseconds, at most three. A bridge error arrives as `HaacException`: an invalid
     * date (`HAAC-SCH-001`/`HAAC-SCH-009`) or a connection that is gone; the editor then shows no preview.
     */
    suspend fun nextRuns(draft: ScheduleDraft): List<Long> {
        val reply = live.requireOpen().request(PREVIEW, ScheduleFields.preview(draft))
        return json.decodeOrUnexpected(SchedulePreview.serializer(), reply).nextRuns.mapNotNull(::isoEpochMillis)
    }

    /** The bridge command. */
    private companion object {
        const val PREVIEW = "haac_bridge/schedules/preview"
    }
}
