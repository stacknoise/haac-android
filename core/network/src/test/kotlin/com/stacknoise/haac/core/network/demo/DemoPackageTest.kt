package com.stacknoise.haac.core.network.demo

import java.io.File
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class DemoPackageTest {
    private val sources = File("src/main/kotlin/com/stacknoise/haac/core/network/demo")
        .walkTopDown().filter { it.extension == "kt" }.toList()

    @Test
    fun `the demo package imports nothing of OkHttp and no client of the real connection`() {
        assertTrue(sources.size > 5, "the sources of the demo package were not found")
        val forbidden = listOf("okhttp3", "TokenClient", "InstanceSession", "EndpointSelector", "BridgeInfoClient")
        val offenders = sources.flatMap { file ->
            file.readLines().filter { it.startsWith("import ") }
                .filter { line -> forbidden.any { it in line } }.map { "${file.name}: $it" }
        }
        assertEquals(emptyList<String>(), offenders)
    }
}
