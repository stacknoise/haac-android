import org.gradle.api.Plugin
import org.gradle.api.Project

/** Jetpack Compose with the BOM and Material 3 for a module that already applies an Android plugin. */
class AndroidComposeConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("org.jetbrains.kotlin.plugin.compose")
        pluginManager.withPlugin("com.android.base") {
            val bom = dependencies.platform(libs.findLibrary("compose-bom").get())
            dependencies.apply {
                add("implementation", bom)
                add("implementation", libs.findLibrary("compose-ui").get())
                add("implementation", libs.findLibrary("compose-material3").get())
                add("implementation", libs.findLibrary("compose-ui-tooling-preview").get())
                add("debugImplementation", libs.findLibrary("compose-ui-tooling").get())
            }
        }
    }
}
