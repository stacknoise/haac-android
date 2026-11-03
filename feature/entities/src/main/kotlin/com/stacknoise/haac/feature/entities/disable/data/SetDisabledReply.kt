package com.stacknoise.haac.feature.entities.disable.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Answer of `haac_bridge/entities/set_disabled` (concept 7.5, 11.2): [disabled] is the caller's full list of
 * deactivated entities, [schedulesChanged] the ids of the schedules switched off or on again by the change.
 */
@Serializable
internal class SetDisabledReply(
    val disabled: List<String> = emptyList(),
    @SerialName("schedules_changed") val schedulesChanged: List<String> = emptyList(),
)
