import com.android.build.api.dsl.ApplicationExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import java.io.File

/**
 * The `:app` module: application plugin, SDK levels, the `play`/`sideload` flavors (concept 14.4), R8 for the release
 * build (concept 13.3) and the release
 * signing taken from environment variables (concept 16.6); without them the release build stays unsigned.
 */
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
            signing()
            buildTypes.getByName("release") {
                // R8 shrinks, optimises and obfuscates the release build (concept 13.3).
                isMinifyEnabled = true
                isShrinkResources = true
                proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
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

/** Names of the environment variables that carry the release signing data; the CI fills them from secrets. */
private object SigningEnv {
    const val KEYSTORE_FILE = "HAAC_KEYSTORE_FILE"
    const val KEYSTORE_PASSWORD = "HAAC_KEYSTORE_PASSWORD"
    const val KEY_ALIAS = "HAAC_KEY_ALIAS"
    const val KEY_PASSWORD = "HAAC_KEY_PASSWORD"
}

/** Signs the release build with the keystore of [SigningEnv], when all four variables are set. */
private fun ApplicationExtension.signing() {
    val keystore = System.getenv(SigningEnv.KEYSTORE_FILE)
    val storePassword = System.getenv(SigningEnv.KEYSTORE_PASSWORD)
    val alias = System.getenv(SigningEnv.KEY_ALIAS)
    val keyPassword = System.getenv(SigningEnv.KEY_PASSWORD)
    if (listOf(keystore, storePassword, alias, keyPassword).any { it.isNullOrEmpty() }) return
    val release = signingConfigs.create("release") {
        storeFile = File(keystore)
        this.storePassword = storePassword
        keyAlias = alias
        this.keyPassword = keyPassword
    }
    buildTypes.getByName("release").signingConfig = release
}
