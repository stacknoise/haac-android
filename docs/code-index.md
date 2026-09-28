# Code index – HAAC Android

> GENERATED FILE – do not edit by hand. Regenerate with `./gradlew codeIndex` (concept 17.5).

Lists every class and function of the app with signature, file and a one-line KDoc summary, grouped by package. Read it before writing code to reuse existing functions instead of duplicating them.

## com.stacknoise.haac.app

| Symbol | Signature | File | Description |
| --- | --- | --- | --- |
| `HaacApplication` | `class HaacApplication : Application()` | `app/src/main/kotlin/com/stacknoise/haac/app/HaacApplication.kt` | Application entry point; sets up Hilt. |
| `MainActivity` | `class MainActivity : ComponentActivity()` | `app/src/main/kotlin/com/stacknoise/haac/app/MainActivity.kt` | The single activity of the app; hosts the Compose navigation graph (concept 3). |
| `MainActivity.onCreate` | `override fun onCreate(savedInstanceState: Bundle?)` | `app/src/main/kotlin/com/stacknoise/haac/app/MainActivity.kt` | Enables edge-to-edge drawing and sets the themed Compose content. |

## com.stacknoise.haac.app.navigation

| Symbol | Signature | File | Description |
| --- | --- | --- | --- |
| `HaacNavHost` | `fun HaacNavHost()` | `app/src/main/kotlin/com/stacknoise/haac/app/navigation/HaacNavHost.kt` | Scaffold with the bottom bar and one placeholder screen per top-level destination. |
| `PlaceholderScreen` | `private fun PlaceholderScreen(@StringRes title: Int)` | `app/src/main/kotlin/com/stacknoise/haac/app/navigation/HaacNavHost.kt` | Temporary screen until the feature modules provide their own. |
| `TopLevelDestination` | `enum class TopLevelDestination(val route: String, @param:StringRes val label: Int, @param:DrawableRes val icon: Int)` | `app/src/main/kotlin/com/stacknoise/haac/app/navigation/TopLevelDestination.kt` | Top-level destinations of the bottom bar (concept 15.2). |

## com.stacknoise.haac.core.common.ui.theme

| Symbol | Signature | File | Description |
| --- | --- | --- | --- |
| `HaacColors` | `object HaacColors` | `core/common/src/main/kotlin/com/stacknoise/haac/core/common/ui/theme/HaacColors.kt` | Design tokens of the "Nocturne" theme, read from mockup set 1c (concept 15.2). |
| `HaacShapes` | `object HaacShapes` | `core/common/src/main/kotlin/com/stacknoise/haac/core/common/ui/theme/HaacShapes.kt` | Corner radii of concept 15.2: 8 dp chips and fields, 12 dp tiles, dialogs and buttons. |
| `HaacTheme` | `fun HaacTheme(content: @Composable () -> Unit)` | `core/common/src/main/kotlin/com/stacknoise/haac/core/common/ui/theme/Theme.kt` | The app theme: dark "Nocturne" colors, Inter and the corner radii of concept 15.2. |
| `TextStyle.inter` | `private fun TextStyle.inter(weight: FontWeight = FontWeight.Normal): TextStyle` | `core/common/src/main/kotlin/com/stacknoise/haac/core/common/ui/theme/Type.kt` | Returns this style with Inter and the given weight (default: regular). |

## com.stacknoise.haac.core.error

| Symbol | Signature | File | Description |
| --- | --- | --- | --- |
| `ErrorCode` | `enum class ErrorCode(val code: String, @param:StringRes val message: Int, val action: ErrorAction, val description: String)` | `core/error/src/main/kotlin/com/stacknoise/haac/core/error/ErrorCode.kt` | Every error code of the app (concept 17.3); the only place where codes are defined. |
| `ErrorAction` | `enum class ErrorAction` | `core/error/src/main/kotlin/com/stacknoise/haac/core/error/ErrorCode.kt` | The one action button an error entry offers besides Dismiss (concept 17.4). |
| `ErrorFactory` | `interface ErrorFactory` | `core/error/src/main/kotlin/com/stacknoise/haac/core/error/ErrorFactory.kt` | Turns caught throwables and bridge error replies into [HaacException]s (concept 17.2, 17.3). |
| `ErrorFactory.from` | `fun from(throwable: Throwable): HaacException` | `core/error/src/main/kotlin/com/stacknoise/haac/core/error/ErrorFactory.kt` | Returns a [HaacException] for [throwable]; rethrows [CancellationException] unchanged. |
| `ErrorFactory.fromBridgeError` | `fun fromBridgeError(habCode: String): HaacException` | `core/error/src/main/kotlin/com/stacknoise/haac/core/error/ErrorFactory.kt` | Returns the [HaacException] for a bridge error reply with HAB code [habCode] (concept 18.3). |
| `DefaultErrorFactory` | `class DefaultErrorFactory @Inject constructor() : ErrorFactory` | `core/error/src/main/kotlin/com/stacknoise/haac/core/error/ErrorFactory.kt` | Default mapping; the only place that decides which code an error gets. |
| `DefaultErrorFactory.from` | `override fun from(throwable: Throwable): HaacException` | `core/error/src/main/kotlin/com/stacknoise/haac/core/error/ErrorFactory.kt` | Maps low-level exceptions by type; everything unknown becomes HAAC-APP-000. |
| `DefaultErrorFactory.fromBridgeError` | `override fun fromBridgeError(habCode: String): HaacException` | `core/error/src/main/kotlin/com/stacknoise/haac/core/error/ErrorFactory.kt` | Looks the HAB code up in [BRIDGE_CODES] and keeps it on the exception for the detail sheet. |
| `ErrorReporter` | `interface ErrorReporter` | `core/error/src/main/kotlin/com/stacknoise/haac/core/error/ErrorReporter.kt` | Receives every error that is not handled on screen and turns it into an entry of the |
| `ErrorReporter.report` | `fun report(error: HaacException, serverId: String?)` | `core/error/src/main/kotlin/com/stacknoise/haac/core/error/ErrorReporter.kt` | Records [error] for the instance [serverId], or as a global entry when it is null. |
| `HaacException` | `sealed class HaacException(val code: ErrorCode, cause: Throwable? = null, val bridgeCode: String? = null) : Exception(code.description, cause)` | `core/error/src/main/kotlin/com/stacknoise/haac/core/error/HaacException.kt` | Base of every exception the app throws or passes on (concept 17.3). |
| `NetworkException` | `class NetworkException(code: ErrorCode, cause: Throwable? = null) : HaacException(code, cause)` | `core/error/src/main/kotlin/com/stacknoise/haac/core/error/HaacException.kt` | Connection and request errors (area NET). |
| `AuthException` | `class AuthException(code: ErrorCode, cause: Throwable? = null, bridgeCode: String? = null)` | `core/error/src/main/kotlin/com/stacknoise/haac/core/error/HaacException.kt` | Login and token errors (area AUTH). |
| `KeystoreException` | `class KeystoreException(code: ErrorCode, cause: Throwable? = null) : HaacException(code, cause)` | `core/error/src/main/kotlin/com/stacknoise/haac/core/error/HaacException.kt` | Keystore and biometric errors (area SEC). |
| `BridgeException` | `class BridgeException(code: ErrorCode, cause: Throwable? = null, bridgeCode: String? = null)` | `core/error/src/main/kotlin/com/stacknoise/haac/core/error/HaacException.kt` | Errors reported by or about HAAC Bridge (area BRG). |
| `SyncException` | `class SyncException(code: ErrorCode, cause: Throwable? = null) : HaacException(code, cause)` | `core/error/src/main/kotlin/com/stacknoise/haac/core/error/HaacException.kt` | Synchronisation errors (area SYNC). |
| `StorageException` | `class StorageException(code: ErrorCode, cause: Throwable? = null) : HaacException(code, cause)` | `core/error/src/main/kotlin/com/stacknoise/haac/core/error/HaacException.kt` | Local database errors (area DB). |
| `ValidationException` | `class ValidationException(code: ErrorCode, cause: Throwable? = null, bridgeCode: String? = null)` | `core/error/src/main/kotlin/com/stacknoise/haac/core/error/HaacException.kt` | Invalid layout or entity operations (areas LAY, ENT, INST, DISC). |
| `UnexpectedException` | `class UnexpectedException(cause: Throwable? = null) : HaacException(ErrorCode.APP_UNEXPECTED, cause)` | `core/error/src/main/kotlin/com/stacknoise/haac/core/error/HaacException.kt` | Anything without its own code (HAAC-APP-000). |

## com.stacknoise.haac.core.error.di

| Symbol | Signature | File | Description |
| --- | --- | --- | --- |
| `ErrorModule` | `abstract class ErrorModule` | `core/error/src/main/kotlin/com/stacknoise/haac/core/error/di/ErrorModule.kt` | Provides the [ErrorFactory] implementation. |
| `ErrorModule.bindErrorFactory` | `abstract fun bindErrorFactory(factory: DefaultErrorFactory): ErrorFactory` | `core/error/src/main/kotlin/com/stacknoise/haac/core/error/di/ErrorModule.kt` | Binds [DefaultErrorFactory] as the app-wide [ErrorFactory]. |
