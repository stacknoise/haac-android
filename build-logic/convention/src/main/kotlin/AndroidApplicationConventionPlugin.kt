import com.android.build.api.dsl.ApplicationExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

/** The `:app` module: application plugin, SDK levels and the `play`/`sideload` flavors (concept 14.4). */
class AndroidApplicationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("com.android.application")
        extensions.configure<ApplicationExtension> {
            namespace = moduleNamespace()
            compileSdk = AndroidSdk.COMPILE
            defaultConfig {
                applicationId = ROOT_PACKAGE
                minSdk = AndroidSdk.MIN
                targetSdk = AndroidSdk.TARGET
            }
            compileOptions {
                sourceCompatibility = AndroidSdk.JAVA
                targetCompatibility = AndroidSdk.JAVA
            }
            flavorDimensions += "distribution"
            productFlavors {
                create("play") { dimension = "distribution" }
                create("sideload") { dimension = "distribution" }
            }
            lint {
                warningsAsErrors = true
                abortOnError = true
            }
        }
        configureKotlin()
        configureUnitTests()
        configureDetekt()
    }
}
