import org.gradle.api.Plugin
import org.gradle.api.Project

/** A `:feature:*` module: library + Compose + Hilt and the core modules every feature may use (concept 17.1). */
class AndroidFeatureConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("haac.android.library")
            pluginManager.apply("haac.android.compose")
            pluginManager.apply("haac.hilt")
            dependencies.add("implementation", dependencies.project(mapOf("path" to ":core:common")))
            dependencies.add("implementation", dependencies.project(mapOf("path" to ":core:error")))
            dependencies.add("implementation", libs.findLibrary("androidx-lifecycle-runtime-compose").get())
            dependencies.add("implementation", libs.findLibrary("androidx-hilt-navigation-compose").get())
        }
    }
}
