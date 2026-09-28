import dev.detekt.gradle.extensions.DetektExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

/** SDK levels and JVM target shared by all Android modules (concept 3.3). */
internal object AndroidSdk {
    const val COMPILE = 37
    const val TARGET = 36
    const val MIN = 28
    val JAVA = JavaVersion.VERSION_17
    val JVM = JvmTarget.JVM_17
}

/** Root package; module namespaces are derived from the Gradle path (concept 17.1). */
internal const val ROOT_PACKAGE = "com.stacknoise.haac"

/** The `libs` version catalog of the main build. */
internal val Project.libs: VersionCatalog
    get() = extensions.getByType<VersionCatalogsExtension>().named("libs")

/** Namespace for a module, e.g. `:core:error` -> `com.stacknoise.haac.core.error`. */
internal fun Project.moduleNamespace(): String =
    ROOT_PACKAGE + path.replace(':', '.').replace('-', '_').let { if (it == ".app") "" else it }

/** Kotlin compiler settings shared by all modules. */
internal fun Project.configureKotlin() {
    tasks.withType<KotlinJvmCompile>().configureEach {
        compilerOptions {
            jvmTarget.set(AndroidSdk.JVM)
            allWarningsAsErrors.set(true)
        }
    }
}

/** JUnit 5 for local unit tests (concept 3.3, 14.2). */
internal fun Project.configureUnitTests() {
    pluginManager.apply("de.mannodermaus.android-junit5")
    dependencies.apply {
        add("testImplementation", dependencies.platform(libs.findLibrary("junit-bom").get()))
        add("testImplementation", libs.findLibrary("junit-jupiter").get())
        add("testImplementation", libs.findLibrary("mockk").get())
        add("testImplementation", libs.findLibrary("turbine").get())
        add("testRuntimeOnly", libs.findLibrary("junit-platform-launcher").get())
    }
}

/** Detekt with the shared configuration in config/detekt (concept 17.6). */
internal fun Project.configureDetekt() {
    pluginManager.apply("dev.detekt")
    extensions.configure<DetektExtension> {
        buildUponDefaultConfig.set(true)
        config.setFrom(rootProject.file("config/detekt/detekt.yml"))
        parallel.set(true)
    }
}
