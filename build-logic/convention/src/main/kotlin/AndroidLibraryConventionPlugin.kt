import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

/** Every `:core:*` and `:feature:*` module: library plugin, SDK levels, tests (also shared fixtures) and Detekt. */
class AndroidLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("com.android.library")
        extensions.configure<LibraryExtension> {
            namespace = moduleNamespace()
            compileSdk = AndroidSdk.COMPILE
            defaultConfig {
                minSdk = AndroidSdk.MIN
            }
            compileOptions {
                sourceCompatibility = AndroidSdk.JAVA
                targetCompatibility = AndroidSdk.JAVA
            }
            lint {
                warningsAsErrors = true
                abortOnError = true
            }
            // A module opts in to shared test code by having a `src/testFixtures` folder.
            testFixtures.enable = target.file("src/testFixtures").exists()
        }
        configureKotlin()
        configureUnitTests()
        configureDetekt()
    }
}
