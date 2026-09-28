package codeindex

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class KotlinIndexerTest {
    private val source = """
        package com.example.demo

        /** A demo class. */
        @Suppress("unused")
        class Demo(
            val a: Int,
            val b: String,
        ) : Base() {
            /** Does things. */
            override fun run(block: () -> Unit) {
                val text = "{ not a brace }"
            }

            fun undocumented() = 1

            companion object {
                /** Creates a demo. */
                fun create(): Demo = Demo(1, "x")
            }
        }

        /** Top level. */
        @Composable
        fun Screen(content: @Composable () -> Unit) {
        }

        /** Generic extension. */
        fun <T> List<T>.second(): T = this[1]

        /** A functional interface. */
        fun interface Callback {
            /** Called back. */
            fun call()
        }
    """.trimIndent()

    private val symbols = KotlinIndexer.scan("Demo.kt", source).associateBy { it.name }

    @Test
    fun `finds classes, members and top-level functions with qualified names`() {
        assertEquals(
            setOf("Demo", "Demo.run", "Demo.undocumented", "Demo.create", "Screen", "List.second", "Callback", "Callback.call"),
            symbols.keys,
        )
        assertEquals("com.example.demo", symbols.getValue("Demo").pkg)
    }

    @Test
    fun `summaries come from KDoc above annotations`() {
        assertEquals("A demo class.", symbols.getValue("Demo").summary)
        assertEquals("Top level.", symbols.getValue("Screen").summary)
        assertNull(symbols.getValue("Demo.undocumented").summary)
    }

    @Test
    fun `signatures are joined and cut before the body`() {
        assertEquals("class Demo(val a: Int, val b: String) : Base()", symbols.getValue("Demo").signature)
        assertEquals("fun Screen(content: @Composable () -> Unit)", symbols.getValue("Screen").signature)
        assertEquals("fun undocumented()", symbols.getValue("Demo.undocumented").signature)
        assertEquals("fun <T> List<T>.second(): T", symbols.getValue("List.second").signature)
    }

    @Test
    fun `error codes are parsed across lines`() {
        val codes = CodeIndex.parseErrorCodes(
            """
            NET_UNREACHABLE(
                "HAAC-NET-001",
                R.string.error_net_unreachable,
                ErrorAction.RETRY,
                "Connect failed (DNS, timeout)",
            ),
            """.trimIndent(),
        )
        assertEquals(listOf(ErrorCodeEntry("NET_UNREACHABLE", "HAAC-NET-001", "error_net_unreachable", "RETRY", "Connect failed (DNS, timeout)")), codes)
        assertEquals(mapOf("a" to "It's"), CodeIndex.parseStrings("""<string name="a">It\'s</string>"""))
    }
}
