package com.stacknoise.haac.feature.instance.domain

import com.stacknoise.haac.core.database.server.ServerEntity
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class InstanceItemTest {
    private fun server(id: String, uuid: String?, user: String, external: String? = null, internal: String? = null) =
        ServerEntity(
            id = id,
            instanceUuid = uuid,
            internalUrl = internal,
            externalUrl = external,
            displayName = "Home $id",
            accentColor = 0xFF4DFF7A,
            haUserName = user,
            haVersion = "2026.9.0",
            bridgeApiVersion = 1,
            lastActiveAt = 0,
        )

    @Test
    fun `the active instance is marked and the external host is preferred`() {
        val items = listOf(
            server("a", "u1", "anna", external = "https://ha.example.com/", internal = "http://192.168.1.10:8123/"),
            server("b", "u2", "anna", internal = "http://192.168.1.20:8123/"),
        ).toItems(activeId = "b")
        assertEquals(listOf(false, true), items.map { it.active })
        assertEquals(listOf("ha.example.com", "192.168.1.20"), items.map { it.address })
    }

    @Test
    fun `the user name is shown only when the same server is stored twice`() {
        val items = listOf(
            server("a", "u1", "anna"),
            server("b", "u1", "guest"),
            server("c", "u2", "anna"),
        ).toItems(activeId = null)
        assertEquals(listOf("anna", "guest"), items.take(2).map { it.userName })
        assertNull(items[2].userName)
    }

    @Test
    fun `the demo shows no address`() {
        val items = listOf(
            server("demo", "d3a0d3a0d3a0d3a0d3a0d3a0d3a0d3a0", "Demo", external = "https://demo.haac.invalid/"),
            server("a", "u1", "anna", external = "https://ha.example.com/"),
        ).toItems(activeId = "demo")
        assertEquals(listOf("", "ha.example.com"), items.map { it.address })
    }
}
