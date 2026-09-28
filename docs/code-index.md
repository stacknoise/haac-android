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
| `HaacNavHost` | `fun HaacNavHost()` | `app/src/main/kotlin/com/stacknoise/haac/app/navigation/HaacNavHost.kt` | Root navigation (concept 4.1). Until instances are stored (login, next step) the app always |
| `MainScaffold` | `fun MainScaffold()` | `app/src/main/kotlin/com/stacknoise/haac/app/navigation/MainScaffold.kt` | Main area after sign-in: bottom bar with Rooms, Places, Settings (concept 15.2). |
| `PlaceholderScreen` | `private fun PlaceholderScreen(@StringRes title: Int)` | `app/src/main/kotlin/com/stacknoise/haac/app/navigation/MainScaffold.kt` | Temporary screen until the feature modules provide their own. |
| `Routes` | `object Routes` | `app/src/main/kotlin/com/stacknoise/haac/app/navigation/Routes.kt` | Root routes of the app. |
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

## com.stacknoise.haac.core.network.auth

| Symbol | Signature | File | Description |
| --- | --- | --- | --- |
| `AuthProvider` | `data class AuthProvider(val name: String, val type: String, val id: String? = null)` | `core/network/src/main/kotlin/com/stacknoise/haac/core/network/auth/AuthProvidersClient.kt` | One login provider of a HA server, e.g. type `homeassistant` for username and password. |
| `AuthProvidersClient` | `class AuthProvidersClient @Inject constructor(private val client: OkHttpClient, private val json: Json, private val errors: ErrorFactory)` | `core/network/src/main/kotlin/com/stacknoise/haac/core/network/auth/AuthProvidersClient.kt` | Reads `GET /auth/providers`, which confirms a HA server and lists its login providers (concept 4.2, 11.1). |
| `AuthProvidersClient.fetch` | `suspend fun fetch(baseUrl: HttpUrl): List<AuthProvider>` | `core/network/src/main/kotlin/com/stacknoise/haac/core/network/auth/AuthProvidersClient.kt` | Returns the providers of the server at [baseUrl]; errors come as HaacException (NET-00x). |
| `AuthProvidersClient.parse` | `private fun parse(body: String): List<AuthProvider>` | `core/network/src/main/kotlin/com/stacknoise/haac/core/network/auth/AuthProvidersClient.kt` | Accepts both the current object form `{"providers": [...]}` and the older plain list. |

## com.stacknoise.haac.core.network.di

| Symbol | Signature | File | Description |
| --- | --- | --- | --- |
| `NetworkModule` | `object NetworkModule` | `core/network/src/main/kotlin/com/stacknoise/haac/core/network/di/NetworkModule.kt` | Shared HTTP client and JSON settings; no logging interceptor, so auth requests are never logged (concept 5.1). |
| `NetworkModule.provideOkHttpClient` | `fun provideOkHttpClient(): OkHttpClient` | `core/network/src/main/kotlin/com/stacknoise/haac/core/network/di/NetworkModule.kt` | The base client for requests before an instance exists (server validation, login). |
| `NetworkModule.provideJson` | `fun provideJson(): Json` | `core/network/src/main/kotlin/com/stacknoise/haac/core/network/di/NetworkModule.kt` | JSON that tolerates new fields from future HA versions. |

## com.stacknoise.haac.core.network.server

| Symbol | Signature | File | Description |
| --- | --- | --- | --- |
| `CleartextPolicy` | `object CleartextPolicy` | `core/network/src/main/kotlin/com/stacknoise/haac/core/network/server/CleartextPolicy.kt` | HTTPS by default; plain `http://` only to private addresses (concept 4.3). |
| `CleartextPolicy.requireAllowed` | `fun requireAllowed(url: HttpUrl)` | `core/network/src/main/kotlin/com/stacknoise/haac/core/network/server/CleartextPolicy.kt` | Throws HAAC-NET-006 if [url] is `http://` to a host outside the private ranges. |
| `CleartextPolicy.needsWarning` | `fun needsWarning(url: HttpUrl): Boolean` | `core/network/src/main/kotlin/com/stacknoise/haac/core/network/server/CleartextPolicy.kt` | Returns true if [url] is unencrypted and the user must confirm the warning first. |
| `PrivateAddress` | `object PrivateAddress` | `core/network/src/main/kotlin/com/stacknoise/haac/core/network/server/PrivateAddress.kt` | Decides whether a host is in the user's own network (concept 4.3): RFC 1918, loopback and |
| `PrivateAddress.isPrivate` | `fun isPrivate(host: String): Boolean` | `core/network/src/main/kotlin/com/stacknoise/haac/core/network/server/PrivateAddress.kt` | Returns true if [host] is a private address or a `.local` name. |
| `PrivateAddress.isPrivateIpv4` | `private fun isPrivateIpv4(host: String): Boolean` | `core/network/src/main/kotlin/com/stacknoise/haac/core/network/server/PrivateAddress.kt` | RFC 1918, loopback 127/8 and link-local 169.254/16. |
| `PrivateAddress.isPrivateIpv6` | `private fun isPrivateIpv6(host: String): Boolean` | `core/network/src/main/kotlin/com/stacknoise/haac/core/network/server/PrivateAddress.kt` | Loopback ::1, unique-local fc00::/7 and link-local fe80::/10. |

## com.stacknoise.haac.feature.onboarding.data

| Symbol | Signature | File | Description |
| --- | --- | --- | --- |
| `HaServerValidator` | `class HaServerValidator @Inject constructor(private val providers: AuthProvidersClient) : ServerValidator` | `feature/onboarding/src/main/kotlin/com/stacknoise/haac/feature/onboarding/data/HaServerValidator.kt` | Validates a server via `GET /auth/providers` and requires the username/password provider (concept 4.2, 5.1). |
| `HaServerValidator.validate` | `override suspend fun validate(url: HttpUrl): ValidatedServer` | `feature/onboarding/src/main/kotlin/com/stacknoise/haac/feature/onboarding/data/HaServerValidator.kt` | Throws HAAC-AUTH-004 if the server offers no username/password login. |
| `HaServiceParser` | `object HaServiceParser` | `feature/onboarding/src/main/kotlin/com/stacknoise/haac/feature/onboarding/data/HaServiceParser.kt` | Builds a [DiscoveredServer] from a resolved `_home-assistant._tcp` service and its TXT record. |
| `HaServiceParser.parse` | `fun parse(serviceName: String, ip: String, port: Int, txt: Map<String, ByteArray?>): DiscoveredServer` | `feature/onboarding/src/main/kotlin/com/stacknoise/haac/feature/onboarding/data/HaServiceParser.kt` | Returns the server; the URL is the TXT `base_url`, else `http://<ip>:<port>` as HA announces it. |
| `NsdServerDiscovery` | `class NsdServerDiscovery @Inject constructor(private val nsd: NsdManager) : ServerDiscovery` | `feature/onboarding/src/main/kotlin/com/stacknoise/haac/feature/onboarding/data/NsdServerDiscovery.kt` | LAN discovery with Android's NsdManager (concept 4.2, M-01). |
| `NsdServerDiscovery.servers` | `override fun servers(): Flow<List<DiscoveredServer>>` | `feature/onboarding/src/main/kotlin/com/stacknoise/haac/feature/onboarding/data/NsdServerDiscovery.kt` | Discovers while collected; stops discovery when the collector goes away. |
| `NsdServerDiscovery.publish` | `fun publish()` | `feature/onboarding/src/main/kotlin/com/stacknoise/haac/feature/onboarding/data/NsdServerDiscovery.kt` | Sends a snapshot of all resolved servers. |
| `NsdServerDiscovery.onDiscoveryStarted` | `override fun onDiscoveryStarted(serviceType: String)` | `feature/onboarding/src/main/kotlin/com/stacknoise/haac/feature/onboarding/data/NsdServerDiscovery.kt` | Discovery is running; nothing to do. |
| `NsdServerDiscovery.onDiscoveryStopped` | `override fun onDiscoveryStopped(serviceType: String)` | `feature/onboarding/src/main/kotlin/com/stacknoise/haac/feature/onboarding/data/NsdServerDiscovery.kt` | Discovery stopped because the flow was closed. |
| `NsdServerDiscovery.onStartDiscoveryFailed` | `override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int)` | `feature/onboarding/src/main/kotlin/com/stacknoise/haac/feature/onboarding/data/NsdServerDiscovery.kt` | Ends the flow so the screen stops showing the scan indicator. |
| `NsdServerDiscovery.onStopDiscoveryFailed` | `override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int)` | `feature/onboarding/src/main/kotlin/com/stacknoise/haac/feature/onboarding/data/NsdServerDiscovery.kt` | Nothing to clean up if stopping fails. |
| `NsdServerDiscovery.onServiceFound` | `override fun onServiceFound(serviceInfo: NsdServiceInfo)` | `feature/onboarding/src/main/kotlin/com/stacknoise/haac/feature/onboarding/data/NsdServerDiscovery.kt` | Resolves the service to get its address and TXT record, then publishes it. |
| `NsdServerDiscovery.onServiceLost` | `override fun onServiceLost(serviceInfo: NsdServiceInfo)` | `feature/onboarding/src/main/kotlin/com/stacknoise/haac/feature/onboarding/data/NsdServerDiscovery.kt` | Removes a server that left the network. |
| `NsdServerDiscovery.resolve` | `private fun resolve(info: NsdServiceInfo, onResolved: (DiscoveredServer) -> Unit)` | `feature/onboarding/src/main/kotlin/com/stacknoise/haac/feature/onboarding/data/NsdServerDiscovery.kt` | Resolves one service with the API of the running Android version. |
| `NsdServerDiscovery.resolveWithCallback` | `private fun resolveWithCallback(info: NsdServiceInfo, onResolved: (DiscoveredServer) -> Unit)` | `feature/onboarding/src/main/kotlin/com/stacknoise/haac/feature/onboarding/data/NsdServerDiscovery.kt` | Android 14+: one-shot service info callback. |
| `NsdServerDiscovery.onServiceInfoCallbackRegistrationFailed` | `override fun onServiceInfoCallbackRegistrationFailed(errorCode: Int)` | `feature/onboarding/src/main/kotlin/com/stacknoise/haac/feature/onboarding/data/NsdServerDiscovery.kt` | Resolution failed; the server is simply not listed. |
| `NsdServerDiscovery.onServiceUpdated` | `override fun onServiceUpdated(serviceInfo: NsdServiceInfo)` | `feature/onboarding/src/main/kotlin/com/stacknoise/haac/feature/onboarding/data/NsdServerDiscovery.kt` | Publishes the first complete update and stops listening. |
| `NsdServerDiscovery.onServiceLost` | `override fun onServiceLost()` | `feature/onboarding/src/main/kotlin/com/stacknoise/haac/feature/onboarding/data/NsdServerDiscovery.kt` | The service went away before it was resolved. |
| `NsdServerDiscovery.onServiceInfoCallbackUnregistered` | `override fun onServiceInfoCallbackUnregistered()` | `feature/onboarding/src/main/kotlin/com/stacknoise/haac/feature/onboarding/data/NsdServerDiscovery.kt` | Callback removed; nothing to do. |
| `NsdServerDiscovery.NsdServiceInfo.toServer` | `private fun NsdServiceInfo.toServer(ip: String): DiscoveredServer` | `feature/onboarding/src/main/kotlin/com/stacknoise/haac/feature/onboarding/data/NsdServerDiscovery.kt` | Parses a resolved service reached at [ip]. |
| `NsdServerDiscovery.resolveLegacy` | `private fun resolveLegacy(info: NsdServiceInfo, onResolved: (DiscoveredServer) -> Unit)` | `feature/onboarding/src/main/kotlin/com/stacknoise/haac/feature/onboarding/data/NsdServerDiscovery.kt` | Android 9–13: resolveService, deprecated from API 34 on. |
| `NsdServerDiscovery.onResolveFailed` | `override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int)` | `feature/onboarding/src/main/kotlin/com/stacknoise/haac/feature/onboarding/data/NsdServerDiscovery.kt` | Resolution failed (e.g. another resolve is running); the server is not listed. |
| `NsdServerDiscovery.onServiceResolved` | `override fun onServiceResolved(serviceInfo: NsdServiceInfo)` | `feature/onboarding/src/main/kotlin/com/stacknoise/haac/feature/onboarding/data/NsdServerDiscovery.kt` | Publishes the resolved server. |

## com.stacknoise.haac.feature.onboarding.di

| Symbol | Signature | File | Description |
| --- | --- | --- | --- |
| `OnboardingModule` | `abstract class OnboardingModule` | `feature/onboarding/src/main/kotlin/com/stacknoise/haac/feature/onboarding/di/OnboardingModule.kt` | Bindings of the onboarding topic. |
| `OnboardingModule.bindServerDiscovery` | `abstract fun bindServerDiscovery(discovery: NsdServerDiscovery): ServerDiscovery` | `feature/onboarding/src/main/kotlin/com/stacknoise/haac/feature/onboarding/di/OnboardingModule.kt` | LAN discovery via NsdManager. |
| `OnboardingModule.bindServerValidator` | `abstract fun bindServerValidator(validator: HaServerValidator): ServerValidator` | `feature/onboarding/src/main/kotlin/com/stacknoise/haac/feature/onboarding/di/OnboardingModule.kt` | Server validation via /auth/providers. |
| `OnboardingModule.provideNsdManager` | `fun provideNsdManager(@ApplicationContext context: Context): NsdManager` | `feature/onboarding/src/main/kotlin/com/stacknoise/haac/feature/onboarding/di/OnboardingModule.kt` | The system NsdManager. |

## com.stacknoise.haac.feature.onboarding.domain

| Symbol | Signature | File | Description |
| --- | --- | --- | --- |
| `DiscoveredServer` | `data class DiscoveredServer(val id: String, val name: String, val address: String, val url: String, val version: String?)` | `feature/onboarding/src/main/kotlin/com/stacknoise/haac/feature/onboarding/domain/ServerDiscovery.kt` | A HA server announced on the local network via zeroconf (concept 4.2, M-01). |
| `ServerDiscovery` | `interface ServerDiscovery` | `feature/onboarding/src/main/kotlin/com/stacknoise/haac/feature/onboarding/domain/ServerDiscovery.kt` | Finds HA servers on the local network while collected. |
| `ServerDiscovery.servers` | `fun servers(): Flow<List<DiscoveredServer>>` | `feature/onboarding/src/main/kotlin/com/stacknoise/haac/feature/onboarding/domain/ServerDiscovery.kt` | Emits the current list whenever a server appears or disappears; completes if discovery fails. |
| `ServerUrlNormalizer` | `object ServerUrlNormalizer` | `feature/onboarding/src/main/kotlin/com/stacknoise/haac/feature/onboarding/domain/ServerUrlNormalizer.kt` | Turns user input into the base URL of a HA server (concept 4.2, step 2). |
| `ServerUrlNormalizer.normalize` | `fun normalize(input: String): HttpUrl` | `feature/onboarding/src/main/kotlin/com/stacknoise/haac/feature/onboarding/domain/ServerUrlNormalizer.kt` | Adds https, removes paths, query and trailing slashes; throws HAAC-NET-005 if nothing usable is left. |
| `ServerUrlNormalizer.invalid` | `private fun invalid()` | `feature/onboarding/src/main/kotlin/com/stacknoise/haac/feature/onboarding/domain/ServerUrlNormalizer.kt` | The error for input that is not a usable http(s) address. |
| `ValidatedServer` | `data class ValidatedServer(val url: HttpUrl)` | `feature/onboarding/src/main/kotlin/com/stacknoise/haac/feature/onboarding/domain/ServerValidator.kt` | A reachable HA server that offers username and password login. |
| `ServerValidator` | `interface ServerValidator` | `feature/onboarding/src/main/kotlin/com/stacknoise/haac/feature/onboarding/domain/ServerValidator.kt` | Confirms that a URL points to a HA server the app can sign in to (concept 4.2, step 3). |
| `ServerValidator.validate` | `suspend fun validate(url: HttpUrl): ValidatedServer` | `feature/onboarding/src/main/kotlin/com/stacknoise/haac/feature/onboarding/domain/ServerValidator.kt` | Returns the validated server or throws a HaacException (NET-00x, AUTH-004). |

## com.stacknoise.haac.feature.onboarding.ui

| Symbol | Signature | File | Description |
| --- | --- | --- | --- |
| `OnboardingActions` | `data class OnboardingActions(val onServerSelected: (DiscoveredServer) -> Unit = {}, val onOtherAddress: () -> Unit = {}, val onManualUrlChanged: (String) -> Unit = {}, val onContinue: () -> Unit = {}, val onCleartextConfirmed: () -> Unit = {}, val onCleartextDismissed: () -> Unit = {})` | `feature/onboarding/src/main/kotlin/com/stacknoise/haac/feature/onboarding/ui/OnboardingActions.kt` | Callbacks of the onboarding screen, grouped to keep composable signatures short. |
| `OnboardingScreen` | `fun OnboardingScreen(viewModel: OnboardingViewModel = hiltViewModel())` | `feature/onboarding/src/main/kotlin/com/stacknoise/haac/feature/onboarding/ui/OnboardingScreen.kt` | M-01: pick a discovered HA server or enter its address (concept 4.2, 15.3). |
| `OnboardingContent` | `fun OnboardingContent(state: OnboardingUiState, actions: OnboardingActions)` | `feature/onboarding/src/main/kotlin/com/stacknoise/haac/feature/onboarding/ui/OnboardingScreen.kt` | Stateless layout of M-01. |
| `SectionHeader` | `private fun SectionHeader(scanning: Boolean)` | `feature/onboarding/src/main/kotlin/com/stacknoise/haac/feature/onboarding/ui/OnboardingScreen.kt` | "ON THIS NETWORK" label with the live scan indicator. |
| `ServerList` | `private fun ServerList(state: OnboardingUiState, actions: OnboardingActions)` | `feature/onboarding/src/main/kotlin/com/stacknoise/haac/feature/onboarding/ui/OnboardingScreen.kt` | Discovered servers, or a hint if none was found. |
| `ServerRow` | `private fun ServerRow(server: DiscoveredServer, selected: Boolean, onClick: () -> Unit)` | `feature/onboarding/src/main/kotlin/com/stacknoise/haac/feature/onboarding/ui/OnboardingScreen.kt` | One server with accent bar, name, `IP:port` and a check mark when selected. |
| `ManualAddress` | `private fun ManualAddress(state: OnboardingUiState, actions: OnboardingActions)` | `feature/onboarding/src/main/kotlin/com/stacknoise/haac/feature/onboarding/ui/OnboardingScreen.kt` | Other address…* link, or the URL field once it was tapped. |
| `StatusMessage` | `private fun StatusMessage(state: OnboardingUiState)` | `feature/onboarding/src/main/kotlin/com/stacknoise/haac/feature/onboarding/ui/OnboardingScreen.kt` | Error with its code (concept 17.4), or the positive result of the server check. |
| `ErrorMessage` | `private fun ErrorMessage(code: ErrorCode)` | `feature/onboarding/src/main/kotlin/com/stacknoise/haac/feature/onboarding/ui/OnboardingScreen.kt` | User text of an error code plus the code in the mono font. |
| `ContinueButton` | `private fun ContinueButton(enabled: Boolean, checking: Boolean, onClick: () -> Unit)` | `feature/onboarding/src/main/kotlin/com/stacknoise/haac/feature/onboarding/ui/OnboardingScreen.kt` | Primary action: outlined in the accent colour with an arrow (concept 15.2). |
| `CleartextDialog` | `private fun CleartextDialog(url: String, actions: OnboardingActions)` | `feature/onboarding/src/main/kotlin/com/stacknoise/haac/feature/onboarding/ui/OnboardingScreen.kt` | Warning before an unencrypted connection to a private address (concept 4.3). |
| `OnboardingPreview` | `private fun OnboardingPreview()` | `feature/onboarding/src/main/kotlin/com/stacknoise/haac/feature/onboarding/ui/OnboardingScreen.kt` | Preview with two discovered servers, the first selected. |
| `OnboardingUiState` | `data class OnboardingUiState(val servers: List<DiscoveredServer> = emptyList(), val scanning: Boolean = true, val selectedUrl: String? = null, val manualEntry: Boolean = false, val manualUrl: String = "", val checking: Boolean = false, val error: ErrorCode? = null, val cleartextWarningFor: String? = null, val validatedUrl: String? = null)` | `feature/onboarding/src/main/kotlin/com/stacknoise/haac/feature/onboarding/ui/OnboardingViewModel.kt` | Everything the server part of M-01 shows. |
| `OnboardingViewModel` | `class OnboardingViewModel @Inject constructor(private val discovery: ServerDiscovery, private val validator: ServerValidator) : ViewModel()` | `feature/onboarding/src/main/kotlin/com/stacknoise/haac/feature/onboarding/ui/OnboardingViewModel.kt` | Server selection and validation of the onboarding screen (concept 4.2, 4.3). |
| `OnboardingViewModel.onServerSelected` | `fun onServerSelected(server: DiscoveredServer)` | `feature/onboarding/src/main/kotlin/com/stacknoise/haac/feature/onboarding/ui/OnboardingViewModel.kt` | Selects a discovered server and leaves manual entry. |
| `OnboardingViewModel.onOtherAddress` | `fun onOtherAddress()` | `feature/onboarding/src/main/kotlin/com/stacknoise/haac/feature/onboarding/ui/OnboardingViewModel.kt` | Opens the manual address field (*Other address…*). |
| `OnboardingViewModel.onManualUrlChanged` | `fun onManualUrlChanged(value: String)` | `feature/onboarding/src/main/kotlin/com/stacknoise/haac/feature/onboarding/ui/OnboardingViewModel.kt` | Updates the manual address. |
| `OnboardingViewModel.onContinue` | `fun onContinue()` | `feature/onboarding/src/main/kotlin/com/stacknoise/haac/feature/onboarding/ui/OnboardingViewModel.kt` | Normalises the chosen address, asks for cleartext confirmation if needed, then validates it. |
| `OnboardingViewModel.chosenUrl` | `private fun chosenUrl(): HttpUrl?` | `feature/onboarding/src/main/kotlin/com/stacknoise/haac/feature/onboarding/ui/OnboardingViewModel.kt` | The normalised address of the selected or entered server; shows the error and returns null if unusable. |
| `OnboardingViewModel.onCleartextConfirmed` | `fun onCleartextConfirmed()` | `feature/onboarding/src/main/kotlin/com/stacknoise/haac/feature/onboarding/ui/OnboardingViewModel.kt` | The user accepted the unencrypted connection in the warning dialog. |
| `OnboardingViewModel.onCleartextDismissed` | `fun onCleartextDismissed()` | `feature/onboarding/src/main/kotlin/com/stacknoise/haac/feature/onboarding/ui/OnboardingViewModel.kt` | The user declined the unencrypted connection. |
| `OnboardingViewModel.validate` | `private fun validate(url: HttpUrl)` | `feature/onboarding/src/main/kotlin/com/stacknoise/haac/feature/onboarding/ui/OnboardingViewModel.kt` | Runs the server check and shows its result or error code. |
