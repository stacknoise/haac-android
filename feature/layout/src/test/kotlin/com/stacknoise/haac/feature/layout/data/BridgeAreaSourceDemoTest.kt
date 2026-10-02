package com.stacknoise.haac.feature.layout.data

import com.stacknoise.haac.core.network.connection.BridgeChannel
import com.stacknoise.haac.core.network.connection.LiveConnection
import com.stacknoise.haac.feature.layout.domain.HaArea
import com.stacknoise.haac.feature.layout.domain.HaFloor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/** [BridgeAreaSource] against the answer of the demo bridge (concept 6.3, 20.7). */
class BridgeAreaSourceDemoTest {
    @Test
    fun `the demo floor and areas are read with levels and entity counts`() = runTest {
        val live = object : LiveConnection {
            override val connection = MutableStateFlow<BridgeChannel?>(demoConnection())
        }
        val areas = BridgeAreaSource(live).load()
        assertEquals(listOf(HaFloor("ground_floor", "Ground floor", 0)), areas.floors)
        assertEquals(
            listOf(
                HaArea("living_room", "Living room", "ground_floor", 4),
                HaArea("kitchen", "Kitchen", "ground_floor", 2),
                HaArea("bedroom", "Bedroom", "ground_floor", 1),
            ),
            areas.areas,
        )
    }
}
