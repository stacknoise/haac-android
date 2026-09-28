import codeindex.CodeIndex
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault

/** Registers `codeIndex` and `codeIndexCheck` on the root project (concept 17.5). */
class CodeIndexConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        target.tasks.register("codeIndex", CodeIndexTask::class.java) {
            group = "documentation"
            description = "Regenerates docs/code-index.md and docs/error-codes.md."
            rootDir.set(target.layout.projectDirectory)
            check.set(false)
        }
        target.tasks.register("codeIndexCheck", CodeIndexTask::class.java) {
            group = "verification"
            description = "Fails if docs/code-index.md or docs/error-codes.md is outdated or a KDoc summary is missing."
            rootDir.set(target.layout.projectDirectory)
            check.set(true)
        }
    }
}

/** Generates or verifies the code index and the error code list. */
@DisableCachingByDefault(because = "Reads the whole source tree and is cheap to run")
abstract class CodeIndexTask : DefaultTask() {
    /** Root directory of the Android project. */
    @get:Internal
    abstract val rootDir: DirectoryProperty

    /** True: only verify; false: write the files. */
    @get:Input
    abstract val check: Property<Boolean>

    /** Runs the generator and writes or compares the output. */
    @TaskAction
    fun run() {
        val root = rootDir.get().asFile
        val errorModule = root.resolve("core/error/src/main")
        val result = CodeIndex.generate(
            root,
            errorModule.resolve("kotlin/com/stacknoise/haac/core/error/ErrorCode.kt"),
            errorModule.resolve("res/values/strings.xml"),
        )
        val outputs = mapOf(
            root.resolve("docs/code-index.md") to result.codeIndex,
            root.resolve("docs/error-codes.md") to result.errorCodes,
        )
        val problems = result.problems.toMutableList()
        outputs.forEach { (file, content) ->
            if (check.get()) {
                val current = if (file.exists()) file.readText().replace("\r\n", "\n") else ""
                if (current != content) problems += "${file.relativeTo(root).invariantSeparatorsPath} is outdated; run ./gradlew codeIndex"
            } else {
                file.writeText(content)
                logger.lifecycle("wrote ${file.relativeTo(root).invariantSeparatorsPath}")
            }
        }
        if (problems.isNotEmpty()) {
            throw GradleException(problems.joinToString(separator = "\n", prefix = "Code index problems:\n"))
        }
    }
}
