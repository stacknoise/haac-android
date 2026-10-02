package com.stacknoise.haac.core.network.demo

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.IOException
import javax.inject.Inject

/** Where the state of the demo bridge is kept between app starts (concept 20.5). */
interface DemoWorldStore {
    /** The saved state, or null if there is none or it cannot be read. */
    fun load(): String?

    /** Saves [text]; a failure is ignored, because the demo then starts with the default data next time. */
    fun save(text: String)

    /** Deletes the saved state. */
    fun delete()
}

/** [DemoWorldStore] with the file `demo-world.json` in the private files of the app; it holds no secrets. */
class FileDemoWorldStore @Inject constructor(@ApplicationContext private val context: Context) : DemoWorldStore {
    private val file: File get() = File(context.filesDir, FILE_NAME)

    /** The content of the file, or null if it is missing or unreadable. */
    override fun load(): String? = try {
        file.takeIf { it.isFile }?.readText()
    } catch (_: IOException) {
        null
    }

    /** Writes a temporary file and renames it, so a crash never leaves half a file. */
    override fun save(text: String) {
        try {
            val temp = File(context.filesDir, "$FILE_NAME.tmp")
            temp.writeText(text)
            if (!temp.renameTo(file)) temp.delete()
        } catch (_: IOException) {
            // Without the file the demo starts with the default data (concept 20.5).
        }
    }

    /** Removes the file, if there is one. */
    override fun delete() {
        file.delete()
        File(context.filesDir, "$FILE_NAME.tmp").delete()
    }

    /** Name of the file in the private files. */
    private companion object {
        const val FILE_NAME = "demo-world.json"
    }
}

/** [DemoWorldStore] in memory: nothing survives the process; for tests of the demo and of its consumers. */
class MemoryDemoWorldStore(private var text: String? = null) : DemoWorldStore {
    /** The saved text, if any. */
    val saved: String? get() = text

    /** The text saved last, or null. */
    override fun load(): String? = text

    /** Keeps [text]. */
    override fun save(text: String) {
        this.text = text
    }

    /** Forgets the text. */
    override fun delete() {
        text = null
    }
}
