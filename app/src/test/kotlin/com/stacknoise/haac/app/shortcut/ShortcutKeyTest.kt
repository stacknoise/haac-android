package com.stacknoise.haac.app.shortcut

import androidx.datastore.core.DataStore
import com.stacknoise.haac.core.database.settings.AppSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class ShortcutKeyTest {
    private val state = MutableStateFlow(AppSettings())
    private val store = object : DataStore<AppSettings> {
        override val data: Flow<AppSettings> = state

        override suspend fun updateData(transform: suspend (t: AppSettings) -> AppSettings): AppSettings =
            transform(state.value).also { state.value = it }
    }
    private var legacy: String? = null

    private val key = ShortcutKey(store, { legacy }, { legacy = null })

    @Test
    fun `the key of the earlier preferences file is taken over once and removed there`() = runTest {
        legacy = "old-key"
        assertEquals("old-key", key.get())
        assertEquals("old-key", state.value.shortcutKey)
        assertNull(legacy)
        assertEquals("old-key", key.get())
    }

    @Test
    fun `without an earlier key a new one is created and stays the same`() = runTest {
        val first = key.get()
        assertNotNull(state.value.shortcutKey)
        assertEquals(first, key.get())
    }
}
