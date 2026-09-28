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
| `HaacNavHost` | `fun HaacNavHost(start: StartViewModel = hiltViewModel())` | `app/src/main/kotlin/com/stacknoise/haac/app/navigation/HaacNavHost.kt` | Root navigation; the start screen follows the start routing of concept 4.1. |
| `NavController.replaceAll` | `private fun NavController.replaceAll(route: String)` | `app/src/main/kotlin/com/stacknoise/haac/app/navigation/HaacNavHost.kt` | Navigates to [route] and clears the back stack, so Back does not return to sign-in or signed-out screens. |
| `MainScaffold` | `fun MainScaffold(onSignedOut: (String) -> Unit)` | `app/src/main/kotlin/com/stacknoise/haac/app/navigation/MainScaffold.kt` | Main area after sign-in: bottom bar with Rooms, Places, Settings (concept 15.2); [onSignedOut] after logout. |
| `PlaceholderScreen` | `private fun PlaceholderScreen(@StringRes title: Int)` | `app/src/main/kotlin/com/stacknoise/haac/app/navigation/MainScaffold.kt` | Temporary screen until the feature modules provide their own. |
| `Routes` | `object Routes` | `app/src/main/kotlin/com/stacknoise/haac/app/navigation/Routes.kt` | Root routes of the app. |
| `Routes.onboarding` | `fun onboarding(serverId: String? = null): String` | `app/src/main/kotlin/com/stacknoise/haac/app/navigation/Routes.kt` | Onboarding for a new instance, or the login of the stored instance [serverId]. |
| `Routes.of` | `fun of(start: StartRoute): String` | `app/src/main/kotlin/com/stacknoise/haac/app/navigation/Routes.kt` | The route that opens [start]. |
| `TopLevelDestination` | `enum class TopLevelDestination(val route: String, @param:StringRes val label: Int, @param:DrawableRes val icon: Int)` | `app/src/main/kotlin/com/stacknoise/haac/app/navigation/TopLevelDestination.kt` | Top-level destinations of the bottom bar (concept 15.2). |

## com.stacknoise.haac.app.start

| Symbol | Signature | File | Description |
| --- | --- | --- | --- |
| `StartRoute` | `sealed interface StartRoute` | `app/src/main/kotlin/com/stacknoise/haac/app/start/StartRouter.kt` | Where the app opens (concept 4.1). |
| `StartRoute.Onboarding` | `data object Onboarding : StartRoute` | `app/src/main/kotlin/com/stacknoise/haac/app/start/StartRouter.kt` | No instance yet: server entry. |
| `StartRoute.SignIn` | `data class SignIn(val serverId: String) : StartRoute` | `app/src/main/kotlin/com/stacknoise/haac/app/start/StartRouter.kt` | The last active instance [serverId] has no refresh token: HA login for it. |
| `StartRoute.Main` | `data class Main(val serverId: String) : StartRoute` | `app/src/main/kotlin/com/stacknoise/haac/app/start/StartRouter.kt` | Token stored: the app opens directly (biometric unlock follows with concept 5.4). |
| `StartRouter` | `class StartRouter @Inject constructor(private val active: ActiveInstanceStore, private val servers: ServerDao, private val tokens: TokenStore)` | `app/src/main/kotlin/com/stacknoise/haac/app/start/StartRouter.kt` | Decides the start screen from the stored instances and tokens (concept 4.1). |
| `StartRouter.route` | `suspend fun route(): StartRoute` | `app/src/main/kotlin/com/stacknoise/haac/app/start/StartRouter.kt` | The last active instance, else the most recently used one; see [StartRoute]. |
| `StartViewModel` | `class StartViewModel @Inject constructor(router: StartRouter) : ViewModel()` | `app/src/main/kotlin/com/stacknoise/haac/app/start/StartViewModel.kt` | Computes the start route once per app start; null while it is being decided. |

## com.stacknoise.haac.core.common.ui

| Symbol | Signature | File | Description |
| --- | --- | --- | --- |
| `SecureWindow` | `fun SecureWindow()` | `core/common/src/main/kotlin/com/stacknoise/haac/core/common/ui/SecureWindow.kt` | Keeps the current screen out of screenshots and the recent-apps preview while it is shown (concept 5.5). |
| `Context.findActivity` | `private tailrec fun Context.findActivity(): Activity?` | `core/common/src/main/kotlin/com/stacknoise/haac/core/common/ui/SecureWindow.kt` | The activity behind this context, unwrapping context wrappers. |

## com.stacknoise.haac.core.common.ui.theme

| Symbol | Signature | File | Description |
| --- | --- | --- | --- |
| `HaacColors` | `object HaacColors` | `core/common/src/main/kotlin/com/stacknoise/haac/core/common/ui/theme/HaacColors.kt` | Design tokens of the "Nocturne" theme, read from mockup set 1c (concept 15.2). |
| `HaacShapes` | `object HaacShapes` | `core/common/src/main/kotlin/com/stacknoise/haac/core/common/ui/theme/HaacShapes.kt` | Corner radii of concept 15.2: 8 dp chips and fields, 12 dp tiles, dialogs and buttons. |
| `InstanceAccents` | `object InstanceAccents` | `core/common/src/main/kotlin/com/stacknoise/haac/core/common/ui/theme/InstanceAccents.kt` | Accent colours that tell instances apart (concept 4.4); the first one is the app accent. |
| `InstanceAccents.forIndex` | `fun forIndex(index: Int): Long` | `core/common/src/main/kotlin/com/stacknoise/haac/core/common/ui/theme/InstanceAccents.kt` | The accent for the instance with position [index], repeating after the last colour. |
| `HaacTheme` | `fun HaacTheme(content: @Composable () -> Unit)` | `core/common/src/main/kotlin/com/stacknoise/haac/core/common/ui/theme/Theme.kt` | The app theme: dark "Nocturne" colors, Inter and the corner radii of concept 15.2. |
| `TextStyle.inter` | `private fun TextStyle.inter(weight: FontWeight = FontWeight.Normal): TextStyle` | `core/common/src/main/kotlin/com/stacknoise/haac/core/common/ui/theme/Type.kt` | Returns this style with Inter and the given weight (default: regular). |

## com.stacknoise.haac.core.database

| Symbol | Signature | File | Description |
| --- | --- | --- | --- |
| `HaacDatabase` | `abstract class HaacDatabase : RoomDatabase()` | `core/database/src/main/kotlin/com/stacknoise/haac/core/database/HaacDatabase.kt` | The local database (concept 12). Tables of later chapters are added with versioned migrations; |
| `HaacDatabase.serverDao` | `abstract fun serverDao(): ServerDao` | `core/database/src/main/kotlin/com/stacknoise/haac/core/database/HaacDatabase.kt` | The `server` table. |

## com.stacknoise.haac.core.database.di

| Symbol | Signature | File | Description |
| --- | --- | --- | --- |
| `DatabaseModule` | `abstract class DatabaseModule` | `core/database/src/main/kotlin/com/stacknoise/haac/core/database/di/DatabaseModule.kt` | Room database, DAOs and the settings DataStore. |
| `DatabaseModule.bindActiveInstanceStore` | `abstract fun bindActiveInstanceStore(store: DataStoreActiveInstanceStore): ActiveInstanceStore` | `core/database/src/main/kotlin/com/stacknoise/haac/core/database/di/DatabaseModule.kt` | Active instance in the settings DataStore. |
| `DatabaseModule.provideDatabase` | `fun provideDatabase(@ApplicationContext context: Context): HaacDatabase` | `core/database/src/main/kotlin/com/stacknoise/haac/core/database/di/DatabaseModule.kt` | The app database `haac.db`; excluded from backup by the data extraction rules. |
| `DatabaseModule.provideServerDao` | `fun provideServerDao(database: HaacDatabase): ServerDao` | `core/database/src/main/kotlin/com/stacknoise/haac/core/database/di/DatabaseModule.kt` | DAO of the `server` table. |
| `DatabaseModule.provideSettings` | `fun provideSettings(@ApplicationContext context: Context): DataStore<AppSettings>` | `core/database/src/main/kotlin/com/stacknoise/haac/core/database/di/DatabaseModule.kt` | The single DataStore instance for [AppSettings]. |

## com.stacknoise.haac.core.database.server

| Symbol | Signature | File | Description |
| --- | --- | --- | --- |
| `ServerDao` | `interface ServerDao` | `core/database/src/main/kotlin/com/stacknoise/haac/core/database/server/ServerDao.kt` | Access to the `server` table (concept 12). |
| `ServerDao.insert` | `suspend fun insert(server: ServerEntity)` | `core/database/src/main/kotlin/com/stacknoise/haac/core/database/server/ServerDao.kt` | Adds a new instance. |
| `ServerDao.update` | `suspend fun update(server: ServerEntity)` | `core/database/src/main/kotlin/com/stacknoise/haac/core/database/server/ServerDao.kt` | Replaces the row with the same id. |
| `ServerDao.get` | `suspend fun get(id: String): ServerEntity?` | `core/database/src/main/kotlin/com/stacknoise/haac/core/database/server/ServerDao.kt` | The instance with [id], or null. |
| `ServerDao.mostRecent` | `suspend fun mostRecent(): ServerEntity?` | `core/database/src/main/kotlin/com/stacknoise/haac/core/database/server/ServerDao.kt` | The instance used last, or null if there is none. |
| `ServerDao.observe` | `fun observe(id: String): Flow<ServerEntity?>` | `core/database/src/main/kotlin/com/stacknoise/haac/core/database/server/ServerDao.kt` | The instance with [id] while it exists. |
| `ServerDao.count` | `suspend fun count(): Int` | `core/database/src/main/kotlin/com/stacknoise/haac/core/database/server/ServerDao.kt` | Number of instances; used to pick the accent colour of a new one. |
| `ServerEntity` | `data class ServerEntity(@PrimaryKey val id: String, @ColumnInfo(name = "base_url") val baseUrl: String, @ColumnInfo(name = "display_name") val displayName: String, @ColumnInfo(name = "accent_color") val accentColor: Long, @ColumnInfo(name = "ha_user_name") val haUserName: String, @ColumnInfo(name = "ha_version") val haVersion: String, @ColumnInfo(name = "bridge_api_version") val bridgeApiVersion: Int, @ColumnInfo(name = "pinned_key_hash") val pinnedKeyHash: String? = null, @ColumnInfo(name = "exposure_revision") val exposureRevision: String? = null, @ColumnInfo(name = "last_sync_at") val lastSyncAt: Long? = null, @ColumnInfo(name = "last_active_at") val lastActiveAt: Long)` | `core/database/src/main/kotlin/com/stacknoise/haac/core/database/server/ServerEntity.kt` | One HA instance (table `server`, concept 12); holds no credentials. |

## com.stacknoise.haac.core.database.settings

| Symbol | Signature | File | Description |
| --- | --- | --- | --- |
| `ActiveInstanceStore` | `interface ActiveInstanceStore` | `core/database/src/main/kotlin/com/stacknoise/haac/core/database/settings/ActiveInstanceStore.kt` | Remembers which instance is active (`activeServerId`, concept 4.4, 12). |
| `ActiveInstanceStore.setActive` | `suspend fun setActive(serverId: String?)` | `core/database/src/main/kotlin/com/stacknoise/haac/core/database/settings/ActiveInstanceStore.kt` | Makes [serverId] the active instance. |
| `DataStoreActiveInstanceStore` | `class DataStoreActiveInstanceStore @Inject constructor(private val settings: DataStore<AppSettings>) : ActiveInstanceStore` | `core/database/src/main/kotlin/com/stacknoise/haac/core/database/settings/ActiveInstanceStore.kt` | [ActiveInstanceStore] backed by the app settings DataStore. |
| `DataStoreActiveInstanceStore.setActive` | `override suspend fun setActive(serverId: String?)` | `core/database/src/main/kotlin/com/stacknoise/haac/core/database/settings/ActiveInstanceStore.kt` | Writes the new id. |
| `AppSettings` | `data class AppSettings(val activeServerId: String? = null)` | `core/database/src/main/kotlin/com/stacknoise/haac/core/database/settings/AppSettings.kt` | Non-sensitive app settings in the typed DataStore (concept 12); never secrets. |
| `AppSettingsSerializer` | `object AppSettingsSerializer : Serializer<AppSettings>` | `core/database/src/main/kotlin/com/stacknoise/haac/core/database/settings/AppSettings.kt` | Reads and writes [AppSettings] as JSON. |
| `AppSettingsSerializer.readFrom` | `override suspend fun readFrom(input: InputStream): AppSettings` | `core/database/src/main/kotlin/com/stacknoise/haac/core/database/settings/AppSettings.kt` | A broken file is reported to DataStore, which then starts from the defaults. |
| `AppSettingsSerializer.writeTo` | `override suspend fun writeTo(t: AppSettings, output: OutputStream)` | `core/database/src/main/kotlin/com/stacknoise/haac/core/database/settings/AppSettings.kt` | Writes the settings as UTF-8 JSON. |

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
| `AuthClientConfig` | `object AuthClientConfig` | `core/network/src/main/kotlin/com/stacknoise/haac/core/network/auth/AuthClientConfig.kt` | OAuth identity of the app towards HA (concept 5.1, 16.8). Never change these URLs: |
| `AuthProvider` | `data class AuthProvider(val name: String, val type: String, val id: String? = null)` | `core/network/src/main/kotlin/com/stacknoise/haac/core/network/auth/AuthProvidersClient.kt` | One login provider of a HA server, e.g. type `homeassistant` for username and password. |
| `AuthProvidersClient` | `class AuthProvidersClient @Inject constructor(private val http: HaHttpClient, private val json: Json)` | `core/network/src/main/kotlin/com/stacknoise/haac/core/network/auth/AuthProvidersClient.kt` | Reads `GET /auth/providers`, which confirms a HA server and lists its login providers (concept 4.2, 11.1). |
| `AuthProvidersClient.fetch` | `suspend fun fetch(baseUrl: HttpUrl): List<AuthProvider>` | `core/network/src/main/kotlin/com/stacknoise/haac/core/network/auth/AuthProvidersClient.kt` | Returns the providers of the server at [baseUrl]; errors come as HaacException (NET-00x). |
| `AuthProvidersClient.parse` | `private fun parse(body: String): List<AuthProvider>` | `core/network/src/main/kotlin/com/stacknoise/haac/core/network/auth/AuthProvidersClient.kt` | Accepts both the current object form `{"providers": [...]}` and the older plain list. |
| `LoginFlowStep` | `sealed interface LoginFlowStep` | `core/network/src/main/kotlin/com/stacknoise/haac/core/network/auth/LoginFlowClient.kt` | One answer of HA's login flow (concept 5.1). |
| `LoginFlowStep.Form` | `data class Form(val flowId: String, val stepId: String, val error: String?, val mfaModules: List<String> = emptyList()) : LoginFlowStep` | `core/network/src/main/kotlin/com/stacknoise/haac/core/network/auth/LoginFlowClient.kt` | HA asks for input in step [stepId] (`init`, `select_mfa_module`, `mfa`); [error] is `errors.base`. |
| `LoginFlowStep.Done` | `class Done(val code: String) : LoginFlowStep` | `core/network/src/main/kotlin/com/stacknoise/haac/core/network/auth/LoginFlowClient.kt` | The login succeeded; [code] is exchanged for tokens once and must not be logged. |
| `LoginFlowStep.Done.toString` | `override fun toString()` | `core/network/src/main/kotlin/com/stacknoise/haac/core/network/auth/LoginFlowClient.kt` | Hides the authorization code. |
| `LoginFlowClient` | `class LoginFlowClient @Inject constructor(private val http: HaHttpClient, private val json: Json)` | `core/network/src/main/kotlin/com/stacknoise/haac/core/network/auth/LoginFlowClient.kt` | Drives HA's login flow API natively (`POST /auth/login_flow`, concept 5.1, 11.1). |
| `LoginFlowClient.start` | `suspend fun start(baseUrl: HttpUrl): LoginFlowStep` | `core/network/src/main/kotlin/com/stacknoise/haac/core/network/auth/LoginFlowClient.kt` | Starts a flow with the username/password provider; HA answers with the `init` form. |
| `LoginFlowClient.submitCredentials` | `suspend fun submitCredentials(baseUrl: HttpUrl, flowId: String, username: String, password: CharArray): LoginFlowStep` | `core/network/src/main/kotlin/com/stacknoise/haac/core/network/auth/LoginFlowClient.kt` | Sends username and password; [password] is not changed, the request body is wiped afterwards. |
| `LoginFlowClient.selectMfaModule` | `suspend fun selectMfaModule(baseUrl: HttpUrl, flowId: String, module: String): LoginFlowStep` | `core/network/src/main/kotlin/com/stacknoise/haac/core/network/auth/LoginFlowClient.kt` | Chooses the MFA module when the user has several (`select_mfa_module`). |
| `LoginFlowClient.submitMfaCode` | `suspend fun submitMfaCode(baseUrl: HttpUrl, flowId: String, code: String): LoginFlowStep` | `core/network/src/main/kotlin/com/stacknoise/haac/core/network/auth/LoginFlowClient.kt` | Sends the code of the `mfa` step. |
| `LoginFlowClient.submit` | `private suspend fun submit(baseUrl: HttpUrl, flowId: String, field: String, value: String): LoginFlowStep` | `core/network/src/main/kotlin/com/stacknoise/haac/core/network/auth/LoginFlowClient.kt` | Sends one field of a flow step together with the client_id. |
| `LoginFlowClient.flowUrl` | `private fun flowUrl(baseUrl: HttpUrl, flowId: String): HttpUrl` | `core/network/src/main/kotlin/com/stacknoise/haac/core/network/auth/LoginFlowClient.kt` | `auth/login_flow/<flowId>` with the id encoded as one path segment. |
| `LoginFlowClient.send` | `private suspend fun send(url: HttpUrl, body: ByteArray): LoginFlowStep` | `core/network/src/main/kotlin/com/stacknoise/haac/core/network/auth/LoginFlowClient.kt` | Posts a JSON body and interprets the answer. |
| `LoginFlowClient.interpret` | `private fun interpret(response: HaResponse): LoginFlowStep` | `core/network/src/main/kotlin/com/stacknoise/haac/core/network/auth/LoginFlowClient.kt` | Maps status codes and flow result types to a step or an error. |
| `LoginFlowClient.parse` | `private fun parse(body: String): LoginFlowStep` | `core/network/src/main/kotlin/com/stacknoise/haac/core/network/auth/LoginFlowClient.kt` | Reads `type`, `flow_id`, `step_id`, `errors.base` and the MFA module options of a flow result. |
| `LoginFlowClient.mfaModules` | `private fun mfaModules(schema: JsonElement?): List<String>` | `core/network/src/main/kotlin/com/stacknoise/haac/core/network/auth/LoginFlowClient.kt` | Option keys of the `multi_factor_auth_module` field: pairs `[id, name]`, plain ids or an id → name map. |
| `SecretJson` | `internal object SecretJson` | `core/network/src/main/kotlin/com/stacknoise/haac/core/network/auth/SecretJson.kt` | Builds the JSON body with the password without ever creating a String of it (concept 5.1). |
| `SecretJson.credentials` | `fun credentials(clientId: String, username: String, password: CharArray): ByteArray` | `core/network/src/main/kotlin/com/stacknoise/haac/core/network/auth/SecretJson.kt` | `{"client_id": …, "username": …, "password": …}` as UTF-8 bytes. |
| `SecretJson.WipingChars` | `private class WipingChars` | `core/network/src/main/kotlin/com/stacknoise/haac/core/network/auth/SecretJson.kt` | A growable char buffer that zeroes every array it gives up. |
| `SecretJson.WipingChars.append` | `fun append(text: CharSequence)` | `core/network/src/main/kotlin/com/stacknoise/haac/core/network/auth/SecretJson.kt` | Appends [text] unchanged. |
| `SecretJson.WipingChars.quoted` | `fun quoted(text: CharSequence)` | `core/network/src/main/kotlin/com/stacknoise/haac/core/network/auth/SecretJson.kt` | Appends [text] as a JSON string literal. |
| `SecretJson.WipingChars.escaped` | `private fun escaped(c: Char)` | `core/network/src/main/kotlin/com/stacknoise/haac/core/network/auth/SecretJson.kt` | Appends one character with JSON escaping. |
| `SecretJson.WipingChars.put` | `private fun put(c: Char)` | `core/network/src/main/kotlin/com/stacknoise/haac/core/network/auth/SecretJson.kt` | Appends one character, growing the buffer if needed. |
| `SecretJson.WipingChars.toUtf8` | `fun toUtf8(): ByteArray` | `core/network/src/main/kotlin/com/stacknoise/haac/core/network/auth/SecretJson.kt` | Encodes the content as UTF-8 and wipes the encoder's buffer. |
| `SecretJson.WipingChars.wipe` | `fun wipe()` | `core/network/src/main/kotlin/com/stacknoise/haac/core/network/auth/SecretJson.kt` | Overwrites the content. |
| `AuthTokens` | `class AuthTokens(val accessToken: String, val refreshToken: String, val expiresInSeconds: Long)` | `core/network/src/main/kotlin/com/stacknoise/haac/core/network/auth/TokenClient.kt` | Tokens issued after login (concept 5.2); [toString] hides them so they never reach a log. |
| `AuthTokens.toString` | `override fun toString()` | `core/network/src/main/kotlin/com/stacknoise/haac/core/network/auth/TokenClient.kt` | Hides the tokens. |
| `AccessToken` | `class AccessToken(val token: String, val expiresInSeconds: Long)` | `core/network/src/main/kotlin/com/stacknoise/haac/core/network/auth/TokenClient.kt` | A new access token from a refresh; memory only (concept 5.2). |
| `AccessToken.toString` | `override fun toString()` | `core/network/src/main/kotlin/com/stacknoise/haac/core/network/auth/TokenClient.kt` | Hides the token. |
| `TokenResponse` | `private class TokenResponse(@SerialName("access_token") val accessToken: String, @SerialName("refresh_token") val refreshToken: String? = null, @SerialName("expires_in") val expiresIn: Long)` | `core/network/src/main/kotlin/com/stacknoise/haac/core/network/auth/TokenClient.kt` | Answer of `POST /auth/token`. |
| `TokenClient` | `class TokenClient @Inject constructor(private val http: HaHttpClient, private val json: Json)` | `core/network/src/main/kotlin/com/stacknoise/haac/core/network/auth/TokenClient.kt` | `POST /auth/token` and `POST /auth/revoke` (concept 5.1, 5.2, 11.1). |
| `TokenClient.exchangeCode` | `suspend fun exchangeCode(baseUrl: HttpUrl, code: String): AuthTokens` | `core/network/src/main/kotlin/com/stacknoise/haac/core/network/auth/TokenClient.kt` | Exchanges the login flow's authorization code; an expired code is HAAC-AUTH-005. |
| `TokenClient.refresh` | `suspend fun refresh(baseUrl: HttpUrl, refreshToken: String): AccessToken` | `core/network/src/main/kotlin/com/stacknoise/haac/core/network/auth/TokenClient.kt` | Gets a new access token; a revoked or expired refresh token is HAAC-AUTH-003 (concept 5.2). |
| `TokenClient.revoke` | `suspend fun revoke(baseUrl: HttpUrl, refreshToken: String)` | `core/network/src/main/kotlin/com/stacknoise/haac/core/network/auth/TokenClient.kt` | Revokes [refreshToken] in HA; HA answers 200 even for unknown tokens (RFC 7009). |
| `TokenClient.parse` | `private fun parse(response: HaResponse, rejected: ErrorCode): TokenResponse` | `core/network/src/main/kotlin/com/stacknoise/haac/core/network/auth/TokenClient.kt` | Reads the token JSON after checking the status code. |
| `TokenClient.failure` | `private fun failure(response: HaResponse, rejected: ErrorCode): HaacException?` | `core/network/src/main/kotlin/com/stacknoise/haac/core/network/auth/TokenClient.kt` | 400/401 become [rejected], 403 HAAC-AUTH-006, other errors HAAC-APP-000; null for success. |

## com.stacknoise.haac.core.network.bridge

| Symbol | Signature | File | Description |
| --- | --- | --- | --- |
| `BridgeInfo` | `data class BridgeInfo(@SerialName("bridge_version") val bridgeVersion: String, @SerialName("api_version") val apiVersion: Int, val domains: List<String> = emptyList(), @SerialName("ha_version") val haVersion: String)` | `core/network/src/main/kotlin/com/stacknoise/haac/core/network/bridge/BridgeInfoClient.kt` | Answer of `haac_bridge/info` (concept 11.2, 11.4). |
| `BridgeInfoClient` | `class BridgeInfoClient @Inject constructor(private val client: OkHttpClient, private val json: Json, private val messages: BridgeMessageFactory, private val errors: ErrorFactory)` | `core/network/src/main/kotlin/com/stacknoise/haac/core/network/bridge/BridgeInfoClient.kt` | Checks after login that HAAC Bridge is installed and speaks a supported API version (concept 4.2 step 4, 11.4). |
| `BridgeInfoClient.fetch` | `suspend fun fetch(baseUrl: HttpUrl, accessToken: String): BridgeInfo` | `core/network/src/main/kotlin/com/stacknoise/haac/core/network/bridge/BridgeInfoClient.kt` | Returns the bridge info; BRG-001 if the bridge is missing, BRG-002 if its API version is not supported. |
| `BridgeInfoClient.converse` | `private suspend fun converse(socket: WebSocket, incoming: Channel<String>, accessToken: String): BridgeInfo` | `core/network/src/main/kotlin/com/stacknoise/haac/core/network/bridge/BridgeInfoClient.kt` | Auth handshake of HA's WebSocket API, then the info command. |
| `BridgeInfoClient.requireType` | `private fun requireType(message: JsonObject, type: String)` | `core/network/src/main/kotlin/com/stacknoise/haac/core/network/bridge/BridgeInfoClient.kt` | The first message of HA's WebSocket API; anything else is not a HA server. |
| `BridgeInfoClient.checkAuth` | `private fun checkAuth(message: JsonObject)` | `core/network/src/main/kotlin/com/stacknoise/haac/core/network/bridge/BridgeInfoClient.kt` | `auth_ok` continues; `auth_invalid` means the access token was rejected (HAAC-AUTH-003). |
| `BridgeInfoClient.result` | `private fun result(message: JsonObject): BridgeInfo` | `core/network/src/main/kotlin/com/stacknoise/haac/core/network/bridge/BridgeInfoClient.kt` | Reads a result: the info on success, otherwise the matching bridge error. |
| `BridgeInfoClient.bridgeError` | `private fun bridgeError(message: JsonObject): HaacException` | `core/network/src/main/kotlin/com/stacknoise/haac/core/network/bridge/BridgeInfoClient.kt` | `unknown_command` means the bridge is not installed (BRG-001); HAB codes map via [ErrorFactory]. |
| `BridgeInfoClient.receive` | `private suspend fun receive(incoming: Channel<String>): JsonObject` | `core/network/src/main/kotlin/com/stacknoise/haac/core/network/bridge/BridgeInfoClient.kt` | Next message as JSON object; a failed or closed socket becomes a HaacException. |
| `BridgeInfoClient.parseMessage` | `private fun parseMessage(text: String): JsonObject` | `core/network/src/main/kotlin/com/stacknoise/haac/core/network/bridge/BridgeInfoClient.kt` | One text frame as JSON object; anything else is not HA's WebSocket API. |
| `BridgeInfoClient.Forwarder` | `private class Forwarder(private val channel: Channel<String>) : WebSocketListener()` | `core/network/src/main/kotlin/com/stacknoise/haac/core/network/bridge/BridgeInfoClient.kt` | Passes WebSocket text frames into [channel] and closes it on failure or close. |
| `BridgeInfoClient.Forwarder.onMessage` | `override fun onMessage(webSocket: WebSocket, text: String)` | `core/network/src/main/kotlin/com/stacknoise/haac/core/network/bridge/BridgeInfoClient.kt` | Queues a text frame. |
| `BridgeInfoClient.Forwarder.onFailure` | `override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?)` | `core/network/src/main/kotlin/com/stacknoise/haac/core/network/bridge/BridgeInfoClient.kt` | Ends the conversation with the cause, e.g. an IOException or SSL error. |
| `BridgeInfoClient.Forwarder.onClosed` | `override fun onClosed(webSocket: WebSocket, code: Int, reason: String)` | `core/network/src/main/kotlin/com/stacknoise/haac/core/network/bridge/BridgeInfoClient.kt` | The server closed the connection. |
| `BridgeCommand` | `data class BridgeCommand(val id: Int, val json: String)` | `core/network/src/main/kotlin/com/stacknoise/haac/core/network/bridge/BridgeMessageFactory.kt` | A WebSocket command with its message id. |
| `BridgeMessageFactory` | `interface BridgeMessageFactory` | `core/network/src/main/kotlin/com/stacknoise/haac/core/network/bridge/BridgeMessageFactory.kt` | Builds the messages of HA's WebSocket API with increasing ids (concept 11, 17.2). |
| `BridgeMessageFactory.auth` | `fun auth(accessToken: String): String` | `core/network/src/main/kotlin/com/stacknoise/haac/core/network/bridge/BridgeMessageFactory.kt` | The `auth` message; contains the access token and must never be logged. |
| `BridgeMessageFactory.command` | `fun command(type: String): BridgeCommand` | `core/network/src/main/kotlin/com/stacknoise/haac/core/network/bridge/BridgeMessageFactory.kt` | A command such as `haac_bridge/info` with the next message id. |
| `DefaultBridgeMessageFactory` | `class DefaultBridgeMessageFactory @Inject constructor() : BridgeMessageFactory` | `core/network/src/main/kotlin/com/stacknoise/haac/core/network/bridge/BridgeMessageFactory.kt` | Message ids grow per factory; HA only requires them to increase on one connection. |
| `DefaultBridgeMessageFactory.auth` | `override fun auth(accessToken: String): String` | `core/network/src/main/kotlin/com/stacknoise/haac/core/network/bridge/BridgeMessageFactory.kt` | `{"type": "auth", "access_token": …}`. |
| `DefaultBridgeMessageFactory.command` | `override fun command(type: String): BridgeCommand` | `core/network/src/main/kotlin/com/stacknoise/haac/core/network/bridge/BridgeMessageFactory.kt` | `{"id": n, "type": …}`. |

## com.stacknoise.haac.core.network.di

| Symbol | Signature | File | Description |
| --- | --- | --- | --- |
| `NetworkBindings` | `abstract class NetworkBindings` | `core/network/src/main/kotlin/com/stacknoise/haac/core/network/di/NetworkBindings.kt` | Factory bindings of the network topic (concept 17.2). |
| `NetworkBindings.bindBridgeMessageFactory` | `abstract fun bindBridgeMessageFactory(factory: DefaultBridgeMessageFactory): BridgeMessageFactory` | `core/network/src/main/kotlin/com/stacknoise/haac/core/network/di/NetworkBindings.kt` | WebSocket messages with increasing ids. |
| `NetworkBindings.bindInstanceSessionFactory` | `abstract fun bindInstanceSessionFactory(factory: DefaultInstanceSessionFactory): InstanceSessionFactory` | `core/network/src/main/kotlin/com/stacknoise/haac/core/network/di/NetworkBindings.kt` | Per-instance sessions. |
| `NetworkModule` | `object NetworkModule` | `core/network/src/main/kotlin/com/stacknoise/haac/core/network/di/NetworkModule.kt` | Shared HTTP client and JSON settings; no logging interceptor, so auth requests are never logged (concept 5.1). |
| `NetworkModule.provideOkHttpClient` | `fun provideOkHttpClient(): OkHttpClient` | `core/network/src/main/kotlin/com/stacknoise/haac/core/network/di/NetworkModule.kt` | The base client for requests before an instance exists (server validation, login). |
| `NetworkModule.provideJson` | `fun provideJson(): Json` | `core/network/src/main/kotlin/com/stacknoise/haac/core/network/di/NetworkModule.kt` | JSON that tolerates new fields from future HA versions. |

## com.stacknoise.haac.core.network.http

| Symbol | Signature | File | Description |
| --- | --- | --- | --- |
| `HaResponse` | `class HaResponse(val code: Int, val body: String)` | `core/network/src/main/kotlin/com/stacknoise/haac/core/network/http/HaHttpClient.kt` | Status code and body text of a HA response. |
| `HaResponse.toString` | `override fun toString()` | `core/network/src/main/kotlin/com/stacknoise/haac/core/network/http/HaHttpClient.kt` | Hides the body, which may contain tokens. |
| `HaHttpClient` | `class HaHttpClient @Inject constructor(private val client: OkHttpClient, private val errors: ErrorFactory)` | `core/network/src/main/kotlin/com/stacknoise/haac/core/network/http/HaHttpClient.kt` | Runs HTTP requests against a HA server: the cleartext rule (concept 4.3) and the error mapping (17.3) |
| `HaHttpClient.get` | `suspend fun get(url: HttpUrl): HaResponse` | `core/network/src/main/kotlin/com/stacknoise/haac/core/network/http/HaHttpClient.kt` | Sends a GET request. |
| `HaHttpClient.post` | `suspend fun post(url: HttpUrl, body: RequestBody): HaResponse` | `core/network/src/main/kotlin/com/stacknoise/haac/core/network/http/HaHttpClient.kt` | Sends a POST request with [body]. |
| `HaHttpClient.execute` | `private suspend fun execute(request: Request): HaResponse` | `core/network/src/main/kotlin/com/stacknoise/haac/core/network/http/HaHttpClient.kt` | Checks the cleartext rule, runs the call and converts I/O errors (NET-001, NET-007). |
| `HttpUrl.endpoint` | `fun HttpUrl.endpoint(path: String): HttpUrl` | `core/network/src/main/kotlin/com/stacknoise/haac/core/network/http/HaHttpClient.kt` | [this] base URL with [path] appended, e.g. `auth/token`. |
| `JsonObject.text` | `fun JsonObject.text(key: String): String?` | `core/network/src/main/kotlin/com/stacknoise/haac/core/network/http/JsonFields.kt` | String value of [key] in a HA JSON answer, or null if it is missing or JSON null. |
| `Json.decodeOrUnexpected` | `fun <T> Json.decodeOrUnexpected(deserializer: DeserializationStrategy<T>, body: String): T` | `core/network/src/main/kotlin/com/stacknoise/haac/core/network/http/JsonFields.kt` | Decodes an answer of a server already known to be HA; malformed JSON is HAAC-APP-000. |

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

## com.stacknoise.haac.core.network.session

| Symbol | Signature | File | Description |
| --- | --- | --- | --- |
| `InstanceSessionFactory` | `interface InstanceSessionFactory` | `core/network/src/main/kotlin/com/stacknoise/haac/core/network/session/InstanceSession.kt` | Creates the session of one instance (concept 4.4, 17.2). |
| `InstanceSessionFactory.create` | `fun create(serverId: String, baseUrl: HttpUrl): InstanceSession` | `core/network/src/main/kotlin/com/stacknoise/haac/core/network/session/InstanceSession.kt` | A session for the instance [serverId] at [baseUrl]; nothing is shared with other instances. |
| `DefaultInstanceSessionFactory` | `class DefaultInstanceSessionFactory @Inject constructor(private val tokens: TokenClient, private val store: TokenStore) : InstanceSessionFactory` | `core/network/src/main/kotlin/com/stacknoise/haac/core/network/session/InstanceSession.kt` | Default [InstanceSessionFactory]. |
| `DefaultInstanceSessionFactory.create` | `override fun create(serverId: String, baseUrl: HttpUrl): InstanceSession` | `core/network/src/main/kotlin/com/stacknoise/haac/core/network/session/InstanceSession.kt` | Wires the token client and store into a new session. |
| `InstanceSession` | `class InstanceSession(val serverId: String, val baseUrl: HttpUrl, private val tokens: TokenClient, private val store: TokenStore, private val clock: () -> Long = System::currentTimeMillis)` | `core/network/src/main/kotlin/com/stacknoise/haac/core/network/session/InstanceSession.kt` | Token handling of one instance (concept 5.2): the access token lives only in memory and is refreshed |
| `InstanceSession.accessToken` | `suspend fun accessToken(): String` | `core/network/src/main/kotlin/com/stacknoise/haac/core/network/session/InstanceSession.kt` | A valid access token; HAAC-AUTH-003 if there is no refresh token or HA revoked it. |
| `InstanceSession.invalidate` | `suspend fun invalidate()` | `core/network/src/main/kotlin/com/stacknoise/haac/core/network/session/InstanceSession.kt` | Drops the access token, e.g. after `auth_invalid` on the WebSocket; the next call refreshes. |
| `InstanceSession.signOut` | `suspend fun signOut(): Boolean` | `core/network/src/main/kotlin/com/stacknoise/haac/core/network/session/InstanceSession.kt` | Logout (concept 5.2): revokes the refresh token in HA, then deletes ciphertext and key. |

## com.stacknoise.haac.core.security.di

| Symbol | Signature | File | Description |
| --- | --- | --- | --- |
| `SecurityModule` | `abstract class SecurityModule` | `core/security/src/main/kotlin/com/stacknoise/haac/core/security/di/SecurityModule.kt` | Bindings of the security topic. |
| `SecurityModule.bindKeyFactory` | `abstract fun bindKeyFactory(factory: AndroidKeyFactory): KeyFactory` | `core/security/src/main/kotlin/com/stacknoise/haac/core/security/di/SecurityModule.kt` | Keys in the Android Keystore. |
| `SecurityModule.bindCipherFactory` | `abstract fun bindCipherFactory(factory: AesGcmCipherFactory): CipherFactory` | `core/security/src/main/kotlin/com/stacknoise/haac/core/security/di/SecurityModule.kt` | AES-GCM ciphers. |
| `SecurityModule.provideTokenStore` | `fun provideTokenStore(@ApplicationContext context: Context, keys: KeyFactory, ciphers: CipherFactory, errors: ErrorFactory): TokenStore` | `core/security/src/main/kotlin/com/stacknoise/haac/core/security/di/SecurityModule.kt` | Token files live in no-backup storage, so they never reach a cloud backup (concept 5.3). |

## com.stacknoise.haac.core.security.keystore

| Symbol | Signature | File | Description |
| --- | --- | --- | --- |
| `CipherFactory` | `interface CipherFactory` | `core/security/src/main/kotlin/com/stacknoise/haac/core/security/keystore/CipherFactory.kt` | Creates initialised ciphers for Keystore keys (concept 5.3, 17.2). |
| `CipherFactory.encrypt` | `fun encrypt(key: SecretKey): Cipher` | `core/security/src/main/kotlin/com/stacknoise/haac/core/security/keystore/CipherFactory.kt` | A cipher that encrypts with [key] and a fresh random IV. |
| `CipherFactory.decrypt` | `fun decrypt(key: SecretKey, iv: ByteArray): Cipher` | `core/security/src/main/kotlin/com/stacknoise/haac/core/security/keystore/CipherFactory.kt` | A cipher that decrypts with [key] and the stored [iv]. |
| `AesGcmCipherFactory` | `class AesGcmCipherFactory @Inject constructor() : CipherFactory` | `core/security/src/main/kotlin/com/stacknoise/haac/core/security/keystore/CipherFactory.kt` | AES-GCM without padding and a 128-bit tag. |
| `AesGcmCipherFactory.encrypt` | `override fun encrypt(key: SecretKey): Cipher` | `core/security/src/main/kotlin/com/stacknoise/haac/core/security/keystore/CipherFactory.kt` | Lets the provider choose the IV, as the Keystore requires. |
| `AesGcmCipherFactory.decrypt` | `override fun decrypt(key: SecretKey, iv: ByteArray): Cipher` | `core/security/src/main/kotlin/com/stacknoise/haac/core/security/keystore/CipherFactory.kt` | Uses the IV that was stored next to the ciphertext. |
| `KeyFactory` | `interface KeyFactory` | `core/security/src/main/kotlin/com/stacknoise/haac/core/security/keystore/KeyFactory.kt` | Creates and deletes the Keystore keys of an instance; the key parameters of concept 5.3 live only here (17.2). |
| `KeyFactory.tokenKey` | `fun tokenKey(serverId: String): SecretKey` | `core/security/src/main/kotlin/com/stacknoise/haac/core/security/keystore/KeyFactory.kt` | Returns the AES-GCM key that encrypts the refresh token of [serverId], creating it on first use. |
| `KeyFactory.deleteKeys` | `fun deleteKeys(serverId: String)` | `core/security/src/main/kotlin/com/stacknoise/haac/core/security/keystore/KeyFactory.kt` | Deletes every key of [serverId]; does nothing if none exists. |
| `AndroidKeyFactory` | `class AndroidKeyFactory @Inject constructor() : KeyFactory` | `core/security/src/main/kotlin/com/stacknoise/haac/core/security/keystore/KeyFactory.kt` | Keys in the Android Keystore, StrongBox-backed when the device has it (concept 5.3). |
| `AndroidKeyFactory.tokenKey` | `override fun tokenKey(serverId: String): SecretKey` | `core/security/src/main/kotlin/com/stacknoise/haac/core/security/keystore/KeyFactory.kt` | Loads the key of [serverId] or generates it. |
| `AndroidKeyFactory.deleteKeys` | `override fun deleteKeys(serverId: String)` | `core/security/src/main/kotlin/com/stacknoise/haac/core/security/keystore/KeyFactory.kt` | Removes the token key of [serverId] from the Keystore. |
| `AndroidKeyFactory.generate` | `private fun generate(alias: String, strongBox: Boolean): SecretKey` | `core/security/src/main/kotlin/com/stacknoise/haac/core/security/keystore/KeyFactory.kt` | AES-256-GCM, encrypt/decrypt only, usable only while the device is unlocked. |
| `AndroidKeyFactory.keyStore` | `private fun keyStore(): KeyStore` | `core/security/src/main/kotlin/com/stacknoise/haac/core/security/keystore/KeyFactory.kt` | The loaded Android Keystore. |
| `AndroidKeyFactory.alias` | `private fun alias(serverId: String)` | `core/security/src/main/kotlin/com/stacknoise/haac/core/security/keystore/KeyFactory.kt` | Keystore alias of the token key of [serverId]. |

## com.stacknoise.haac.core.security.token

| Symbol | Signature | File | Description |
| --- | --- | --- | --- |
| `KeystoreTokenStore` | `class KeystoreTokenStore(private val directory: File, private val keys: KeyFactory, private val ciphers: CipherFactory, private val errors: ErrorFactory) : TokenStore` | `core/security/src/main/kotlin/com/stacknoise/haac/core/security/token/KeystoreTokenStore.kt` | One file per instance under `noBackupFilesDir` with format byte, IV and AES-GCM ciphertext (concept 5.3). |
| `KeystoreTokenStore.save` | `override suspend fun save(serverId: String, refreshToken: String)` | `core/security/src/main/kotlin/com/stacknoise/haac/core/security/token/KeystoreTokenStore.kt` | Encrypts with the instance key and replaces the file atomically. |
| `KeystoreTokenStore.read` | `override suspend fun read(serverId: String): String?` | `core/security/src/main/kotlin/com/stacknoise/haac/core/security/token/KeystoreTokenStore.kt` | Reads and decrypts the file of [serverId]. |
| `KeystoreTokenStore.contains` | `override suspend fun contains(serverId: String): Boolean` | `core/security/src/main/kotlin/com/stacknoise/haac/core/security/token/KeystoreTokenStore.kt` | Checks only that the file exists. |
| `KeystoreTokenStore.delete` | `override suspend fun delete(serverId: String)` | `core/security/src/main/kotlin/com/stacknoise/haac/core/security/token/KeystoreTokenStore.kt` | Removes file and key; missing ones are fine. |
| `KeystoreTokenStore.file` | `private fun file(serverId: String)` | `core/security/src/main/kotlin/com/stacknoise/haac/core/security/token/KeystoreTokenStore.kt` | Token file of [serverId]. |
| `KeystoreTokenStore.guarded` | `private suspend fun <T> guarded(block: () -> T): T` | `core/security/src/main/kotlin/com/stacknoise/haac/core/security/token/KeystoreTokenStore.kt` | Runs [block] on the IO dispatcher and converts file and Keystore errors to HAAC-SEC codes. |
| `TokenStore` | `interface TokenStore` | `core/security/src/main/kotlin/com/stacknoise/haac/core/security/token/TokenStore.kt` | Keeps the HA refresh token of each instance encrypted at rest (concept 5.2, 5.3). |
| `TokenStore.save` | `suspend fun save(serverId: String, refreshToken: String)` | `core/security/src/main/kotlin/com/stacknoise/haac/core/security/token/TokenStore.kt` | Encrypts and stores [refreshToken] for [serverId], replacing an older one. |
| `TokenStore.read` | `suspend fun read(serverId: String): String?` | `core/security/src/main/kotlin/com/stacknoise/haac/core/security/token/TokenStore.kt` | Returns the decrypted refresh token of [serverId], or null if none is stored. |
| `TokenStore.contains` | `suspend fun contains(serverId: String): Boolean` | `core/security/src/main/kotlin/com/stacknoise/haac/core/security/token/TokenStore.kt` | Returns true if a refresh token is stored for [serverId]; does not decrypt it. |
| `TokenStore.delete` | `suspend fun delete(serverId: String)` | `core/security/src/main/kotlin/com/stacknoise/haac/core/security/token/TokenStore.kt` | Deletes the ciphertext and the Keystore key of [serverId]. |

## com.stacknoise.haac.feature.onboarding.data

| Symbol | Signature | File | Description |
| --- | --- | --- | --- |
| `HaServerValidator` | `class HaServerValidator @Inject constructor(private val providers: AuthProvidersClient) : ServerValidator` | `feature/onboarding/src/main/kotlin/com/stacknoise/haac/feature/onboarding/data/HaServerValidator.kt` | Validates a server via `GET /auth/providers` and requires the username/password provider (concept 4.2, 5.1). |
| `HaServerValidator.validate` | `override suspend fun validate(url: HttpUrl): ValidatedServer` | `feature/onboarding/src/main/kotlin/com/stacknoise/haac/feature/onboarding/data/HaServerValidator.kt` | Throws HAAC-AUTH-004 if the server offers no username/password login. |
| `HaServiceParser` | `object HaServiceParser` | `feature/onboarding/src/main/kotlin/com/stacknoise/haac/feature/onboarding/data/HaServiceParser.kt` | Builds a [DiscoveredServer] from a resolved `_home-assistant._tcp` service and its TXT record. |
| `HaServiceParser.parse` | `fun parse(serviceName: String, ip: String, port: Int, txt: Map<String, ByteArray?>): DiscoveredServer` | `feature/onboarding/src/main/kotlin/com/stacknoise/haac/feature/onboarding/data/HaServiceParser.kt` | Returns the server; the URL is the TXT `base_url`, else `http://<ip>:<port>` as HA announces it. |
| `HaSignInRepository` | `class HaSignInRepository @Inject constructor(private val flows: LoginFlowClient, private val tokens: TokenClient, private val bridge: BridgeInfoClient, private val registry: InstanceRegistry) : SignInRepository` | `feature/onboarding/src/main/kotlin/com/stacknoise/haac/feature/onboarding/data/HaSignInRepository.kt` | Native login via HA's login flow API, bridge check and storage of the instance (concept 4.2, 5.1). |
| `HaSignInRepository.knownServer` | `override suspend fun knownServer(serverId: String): KnownServer?` | `feature/onboarding/src/main/kotlin/com/stacknoise/haac/feature/onboarding/data/HaSignInRepository.kt` | Reads the stored instance for the login of concept 4.1. |
| `HaSignInRepository.signIn` | `override suspend fun signIn(url: HttpUrl, username: String, password: CharArray): SignInStep` | `feature/onboarding/src/main/kotlin/com/stacknoise/haac/feature/onboarding/data/HaSignInRepository.kt` | A new flow per attempt, so a wrong password never leaves a half-used flow behind. |
| `HaSignInRepository.submitCode` | `override suspend fun submitCode(url: HttpUrl, flowId: String, code: String): SignInStep` | `feature/onboarding/src/main/kotlin/com/stacknoise/haac/feature/onboarding/data/HaSignInRepository.kt` | Sends the code; a wrong one keeps the flow, so the user can try again. |
| `HaSignInRepository.finish` | `override suspend fun finish(target: SignInTarget, username: String, tokens: AuthTokens): String` | `feature/onboarding/src/main/kotlin/com/stacknoise/haac/feature/onboarding/data/HaSignInRepository.kt` | Bridge check first: without HAAC Bridge the instance is not stored. |
| `HaSignInRepository.discard` | `override suspend fun discard(url: HttpUrl, tokens: AuthTokens): Boolean` | `feature/onboarding/src/main/kotlin/com/stacknoise/haac/feature/onboarding/data/HaSignInRepository.kt` | Best effort: an unrevoked token expires in HA after a period of inactivity (concept 5.2). |
| `HaSignInRepository.next` | `private suspend fun next(url: HttpUrl, step: LoginFlowStep): SignInStep` | `feature/onboarding/src/main/kotlin/com/stacknoise/haac/feature/onboarding/data/HaSignInRepository.kt` | Interprets a flow step: the code is exchanged for tokens, `invalid_auth`/`invalid_code` become |
| `InstanceRegistry` | `class InstanceRegistry @Inject constructor(private val servers: ServerDao, private val tokens: TokenStore, private val active: ActiveInstanceStore, private val errors: ErrorFactory)` | `feature/onboarding/src/main/kotlin/com/stacknoise/haac/feature/onboarding/data/InstanceRegistry.kt` | Stores a signed-in instance: `server` row, encrypted refresh token and `activeServerId` (concept 4.4, 5.3, 12). |
| `InstanceRegistry.find` | `suspend fun find(id: String): ServerEntity?` | `feature/onboarding/src/main/kotlin/com/stacknoise/haac/feature/onboarding/data/InstanceRegistry.kt` | The stored instance [id], or null. |
| `InstanceRegistry.save` | `suspend fun save(target: SignInTarget, username: String, bridge: BridgeInfo, refreshToken: String): String` | `feature/onboarding/src/main/kotlin/com/stacknoise/haac/feature/onboarding/data/InstanceRegistry.kt` | Creates the instance (or updates the stored one of [target]) and makes it active; returns its id. |
| `InstanceRegistry.newServer` | `private suspend fun newServer(id: String, target: SignInTarget, username: String, bridge: BridgeInfo, now: Long)` | `feature/onboarding/src/main/kotlin/com/stacknoise/haac/feature/onboarding/data/InstanceRegistry.kt` | Row of a new instance with the next accent colour. |
| `InstanceRegistry.database` | `private suspend fun <T> database(block: suspend () -> T): T` | `feature/onboarding/src/main/kotlin/com/stacknoise/haac/feature/onboarding/data/InstanceRegistry.kt` | Runs a database call and converts SQLite errors to HAAC-DB-001. |
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
| `OnboardingModule.bindSignInRepository` | `abstract fun bindSignInRepository(repository: HaSignInRepository): SignInRepository` | `feature/onboarding/src/main/kotlin/com/stacknoise/haac/feature/onboarding/di/OnboardingModule.kt` | Native HA login and storage of the instance. |
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
| `SignInStep` | `sealed interface SignInStep` | `feature/onboarding/src/main/kotlin/com/stacknoise/haac/feature/onboarding/domain/SignInRepository.kt` | Where HA's login flow stands after credentials or a code were sent (concept 5.1). |
| `SignInStep.CodeRequired` | `data class CodeRequired(val flowId: String) : SignInStep` | `feature/onboarding/src/main/kotlin/com/stacknoise/haac/feature/onboarding/domain/SignInRepository.kt` | HA asks for the MFA code of the flow [flowId]. |
| `SignInStep.Authorized` | `class Authorized(val tokens: AuthTokens) : SignInStep` | `feature/onboarding/src/main/kotlin/com/stacknoise/haac/feature/onboarding/domain/SignInRepository.kt` | HA accepted the login; [tokens] stay in memory until the instance is saved. |
| `SignInStep.Authorized.toString` | `override fun toString()` | `feature/onboarding/src/main/kotlin/com/stacknoise/haac/feature/onboarding/domain/SignInRepository.kt` | Hides the tokens. |
| `SignInTarget` | `data class SignInTarget(val url: HttpUrl, val displayName: String, val serverId: String? = null)` | `feature/onboarding/src/main/kotlin/com/stacknoise/haac/feature/onboarding/domain/SignInRepository.kt` | The instance a sign-in is for: a new one, or the stored instance [serverId] that lost its token (concept 4.1). |
| `KnownServer` | `data class KnownServer(val id: String, val url: String, val displayName: String, val userName: String)` | `feature/onboarding/src/main/kotlin/com/stacknoise/haac/feature/onboarding/domain/SignInRepository.kt` | A stored instance shown when only the login is missing (concept 4.1). |
| `SignInRepository` | `interface SignInRepository` | `feature/onboarding/src/main/kotlin/com/stacknoise/haac/feature/onboarding/domain/SignInRepository.kt` | Login against HA and creation of the instance (concept 4.2 step 4, 4.4, 5.1–5.3). |
| `SignInRepository.knownServer` | `suspend fun knownServer(serverId: String): KnownServer?` | `feature/onboarding/src/main/kotlin/com/stacknoise/haac/feature/onboarding/domain/SignInRepository.kt` | The stored instance [serverId], or null if it no longer exists. |
| `SignInRepository.signIn` | `suspend fun signIn(url: HttpUrl, username: String, password: CharArray): SignInStep` | `feature/onboarding/src/main/kotlin/com/stacknoise/haac/feature/onboarding/domain/SignInRepository.kt` | Starts the login flow and sends the credentials; [password] is not kept and not changed. |
| `SignInRepository.submitCode` | `suspend fun submitCode(url: HttpUrl, flowId: String, code: String): SignInStep` | `feature/onboarding/src/main/kotlin/com/stacknoise/haac/feature/onboarding/domain/SignInRepository.kt` | Sends the MFA [code] of the flow [flowId]. |
| `SignInRepository.finish` | `suspend fun finish(target: SignInTarget, username: String, tokens: AuthTokens): String` | `feature/onboarding/src/main/kotlin/com/stacknoise/haac/feature/onboarding/domain/SignInRepository.kt` | Checks HAAC Bridge with the new access token, then stores the encrypted refresh token and the |
| `SignInRepository.discard` | `suspend fun discard(url: HttpUrl, tokens: AuthTokens): Boolean` | `feature/onboarding/src/main/kotlin/com/stacknoise/haac/feature/onboarding/domain/SignInRepository.kt` | Revokes tokens that will not be used; false if HA could not be reached (they then expire in HA). |

## com.stacknoise.haac.feature.onboarding.ui

| Symbol | Signature | File | Description |
| --- | --- | --- | --- |
| `OnboardingActions` | `data class OnboardingActions(val onServerSelected: (DiscoveredServer) -> Unit = {}, val onOtherAddress: () -> Unit = {}, val onManualUrlChanged: (String) -> Unit = {}, val onUsernameChanged: (String) -> Unit = {}, val onCodeChanged: (String) -> Unit = {}, val onSignIn: () -> Unit = {}, val onSubmitCode: () -> Unit = {}, val onRetryBridgeCheck: () -> Unit = {}, val onStartOver: () -> Unit = {}, val onCleartextConfirmed: () -> Unit = {}, val onCleartextDismissed: () -> Unit = {})` | `feature/onboarding/src/main/kotlin/com/stacknoise/haac/feature/onboarding/ui/OnboardingActions.kt` | Callbacks of the onboarding screen, grouped to keep composable signatures short. |
| `OnboardingScreen` | `fun OnboardingScreen(onSignedIn: () -> Unit, viewModel: OnboardingViewModel = hiltViewModel())` | `feature/onboarding/src/main/kotlin/com/stacknoise/haac/feature/onboarding/ui/OnboardingScreen.kt` | M-01: pick a HA server, sign in, optional MFA code (concept 4.2, 5.1, 15.3); [onSignedIn] after success. |
| `CharSequence.copyChars` | `private fun CharSequence.copyChars(): CharArray` | `feature/onboarding/src/main/kotlin/com/stacknoise/haac/feature/onboarding/ui/OnboardingScreen.kt` | Copies the characters without creating a String (concept 5.1). |
| `OnboardingContent` | `fun OnboardingContent(state: OnboardingUiState, password: TextFieldState, actions: OnboardingActions)` | `feature/onboarding/src/main/kotlin/com/stacknoise/haac/feature/onboarding/ui/OnboardingScreen.kt` | Stateless layout of M-01. |
| `ErrorMessage` | `private fun ErrorMessage(code: ErrorCode)` | `feature/onboarding/src/main/kotlin/com/stacknoise/haac/feature/onboarding/ui/OnboardingScreen.kt` | User text of an error code plus the code in the mono font. |
| `PrimaryAction` | `private fun PrimaryAction(state: OnboardingUiState, actions: OnboardingActions)` | `feature/onboarding/src/main/kotlin/com/stacknoise/haac/feature/onboarding/ui/OnboardingScreen.kt` | Sign in, Verify or Try again, depending on the stage. |
| `PrimaryButton` | `private fun PrimaryButton(label: String, enabled: Boolean, busy: Boolean, onClick: () -> Unit)` | `feature/onboarding/src/main/kotlin/com/stacknoise/haac/feature/onboarding/ui/OnboardingScreen.kt` | Primary action: outlined in the accent colour with an arrow (concept 15.2). |
| `CleartextDialog` | `private fun CleartextDialog(url: String, actions: OnboardingActions)` | `feature/onboarding/src/main/kotlin/com/stacknoise/haac/feature/onboarding/ui/OnboardingScreen.kt` | Warning before an unencrypted connection to a private address (concept 4.3). |
| `OnboardingPreview` | `private fun OnboardingPreview()` | `feature/onboarding/src/main/kotlin/com/stacknoise/haac/feature/onboarding/ui/OnboardingScreen.kt` | Preview with two discovered servers, the first selected. |
| `SignInStage` | `enum class SignInStage` | `feature/onboarding/src/main/kotlin/com/stacknoise/haac/feature/onboarding/ui/OnboardingViewModel.kt` | Which part of M-01 is shown. |
| `OnboardingUiState` | `data class OnboardingUiState(val servers: List<DiscoveredServer> = emptyList(), val scanning: Boolean = true, val selectedUrl: String? = null, val manualEntry: Boolean = false, val manualUrl: String = "", val knownServer: KnownServer? = null, val username: String = "", val code: String = "", val stage: SignInStage = SignInStage.CREDENTIALS, val busy: Boolean = false, val error: ErrorCode? = null, val cleartextWarningFor: String? = null, val signedInServerId: String? = null)` | `feature/onboarding/src/main/kotlin/com/stacknoise/haac/feature/onboarding/ui/OnboardingViewModel.kt` | Everything M-01 shows. |
| `OnboardingViewModel` | `class OnboardingViewModel @Inject constructor(private val discovery: ServerDiscovery, private val validator: ServerValidator, private val repository: SignInRepository, savedState: SavedStateHandle) : ViewModel()` | `feature/onboarding/src/main/kotlin/com/stacknoise/haac/feature/onboarding/ui/OnboardingViewModel.kt` | M-01 (concept 4.2, 4.3, 5.1): pick or enter a server, sign in with username, password and optional |
| `OnboardingViewModel.discover` | `private fun discover()` | `feature/onboarding/src/main/kotlin/com/stacknoise/haac/feature/onboarding/ui/OnboardingViewModel.kt` | LAN discovery while the screen is open. |
| `OnboardingViewModel.loadKnownServer` | `private fun loadKnownServer(serverId: String)` | `feature/onboarding/src/main/kotlin/com/stacknoise/haac/feature/onboarding/ui/OnboardingViewModel.kt` | Shows the stored instance and its user; falls back to discovery if it was removed meanwhile. |
| `OnboardingViewModel.onServerSelected` | `fun onServerSelected(server: DiscoveredServer)` | `feature/onboarding/src/main/kotlin/com/stacknoise/haac/feature/onboarding/ui/OnboardingViewModel.kt` | Selects a discovered server and leaves manual entry. |
| `OnboardingViewModel.onOtherAddress` | `fun onOtherAddress()` | `feature/onboarding/src/main/kotlin/com/stacknoise/haac/feature/onboarding/ui/OnboardingViewModel.kt` | Opens the manual address field (*Other address…*). |
| `OnboardingViewModel.onManualUrlChanged` | `fun onManualUrlChanged(value: String)` | `feature/onboarding/src/main/kotlin/com/stacknoise/haac/feature/onboarding/ui/OnboardingViewModel.kt` | Updates the manual address. |
| `OnboardingViewModel.onUsernameChanged` | `fun onUsernameChanged(value: String)` | `feature/onboarding/src/main/kotlin/com/stacknoise/haac/feature/onboarding/ui/OnboardingViewModel.kt` | Updates the username. |
| `OnboardingViewModel.onCodeChanged` | `fun onCodeChanged(value: String)` | `feature/onboarding/src/main/kotlin/com/stacknoise/haac/feature/onboarding/ui/OnboardingViewModel.kt` | Updates the MFA code. |
| `OnboardingViewModel.onSignIn` | `fun onSignIn(password: CharArray)` | `feature/onboarding/src/main/kotlin/com/stacknoise/haac/feature/onboarding/ui/OnboardingViewModel.kt` | Signs in with [password]; the array is wiped when the attempt ends. For an unencrypted address |
| `OnboardingViewModel.onSubmitCode` | `fun onSubmitCode()` | `feature/onboarding/src/main/kotlin/com/stacknoise/haac/feature/onboarding/ui/OnboardingViewModel.kt` | Sends the MFA code; a wrong code keeps the code step. |
| `OnboardingViewModel.onRetryBridgeCheck` | `fun onRetryBridgeCheck()` | `feature/onboarding/src/main/kotlin/com/stacknoise/haac/feature/onboarding/ui/OnboardingViewModel.kt` | Repeats the bridge check with the tokens of the last sign-in, e.g. after installing HAAC Bridge. |
| `OnboardingViewModel.onStartOver` | `fun onStartOver()` | `feature/onboarding/src/main/kotlin/com/stacknoise/haac/feature/onboarding/ui/OnboardingViewModel.kt` | Leaves the code or bridge step; unused tokens are revoked. |
| `OnboardingViewModel.onCleartextConfirmed` | `fun onCleartextConfirmed()` | `feature/onboarding/src/main/kotlin/com/stacknoise/haac/feature/onboarding/ui/OnboardingViewModel.kt` | The user accepted the unencrypted connection in the warning dialog. |
| `OnboardingViewModel.onCleartextDismissed` | `fun onCleartextDismissed()` | `feature/onboarding/src/main/kotlin/com/stacknoise/haac/feature/onboarding/ui/OnboardingViewModel.kt` | The user declined the unencrypted connection. |
| `OnboardingViewModel.needsCleartextWarning` | `private fun needsCleartextWarning(url: HttpUrl): Boolean` | `feature/onboarding/src/main/kotlin/com/stacknoise/haac/feature/onboarding/ui/OnboardingViewModel.kt` | Shows the warning dialog for `http://` unless it was confirmed or the instance is already stored. |
| `OnboardingViewModel.chosenUrl` | `private fun chosenUrl(): HttpUrl?` | `feature/onboarding/src/main/kotlin/com/stacknoise/haac/feature/onboarding/ui/OnboardingViewModel.kt` | Normalised address of the stored, selected or entered server; shows the error and returns null if unusable. |
| `OnboardingViewModel.displayName` | `private fun displayName(url: HttpUrl): String` | `feature/onboarding/src/main/kotlin/com/stacknoise/haac/feature/onboarding/ui/OnboardingViewModel.kt` | Name of a new instance: stored name, zeroconf `location_name` or host (concept 4.4). |
| `OnboardingViewModel.handle` | `private suspend fun handle(step: SignInStep)` | `feature/onboarding/src/main/kotlin/com/stacknoise/haac/feature/onboarding/ui/OnboardingViewModel.kt` | Continues after a flow step: code step, or bridge check and storage. |
| `OnboardingViewModel.finish` | `private suspend fun finish()` | `feature/onboarding/src/main/kotlin/com/stacknoise/haac/feature/onboarding/ui/OnboardingViewModel.kt` | Bridge check and storage; a missing or outdated bridge keeps the tokens for another try. |
| `OnboardingViewModel.perform` | `private fun perform(block: suspend () -> Unit)` | `feature/onboarding/src/main/kotlin/com/stacknoise/haac/feature/onboarding/ui/OnboardingViewModel.kt` | Runs one step with the busy indicator; errors are shown with their code (concept 17.4). |
| `OnboardingViewModel.fail` | `private fun fail(code: ErrorCode)` | `feature/onboarding/src/main/kotlin/com/stacknoise/haac/feature/onboarding/ui/OnboardingViewModel.kt` | Shows [code]; an aborted flow or failed save returns to the credentials. |
| `OnboardingViewModel.discardTokens` | `private fun discardTokens()` | `feature/onboarding/src/main/kotlin/com/stacknoise/haac/feature/onboarding/ui/OnboardingViewModel.kt` | Revokes tokens of a sign-in that will not be completed. |
| `ServerSection` | `internal fun ServerSection(state: OnboardingUiState, actions: OnboardingActions)` | `feature/onboarding/src/main/kotlin/com/stacknoise/haac/feature/onboarding/ui/ServerSection.kt` | The stored instance when signing in again, otherwise discovered servers and *Other address…* (M-01). |
| `SectionHeader` | `private fun SectionHeader(scanning: Boolean)` | `feature/onboarding/src/main/kotlin/com/stacknoise/haac/feature/onboarding/ui/ServerSection.kt` | "ON THIS NETWORK" label with the live scan indicator. |
| `ServerList` | `private fun ServerList(state: OnboardingUiState, actions: OnboardingActions)` | `feature/onboarding/src/main/kotlin/com/stacknoise/haac/feature/onboarding/ui/ServerSection.kt` | Discovered servers, or a hint if none was found. |
| `ServerRow` | `private fun ServerRow(name: String, address: String, selected: Boolean, onClick: () -> Unit)` | `feature/onboarding/src/main/kotlin/com/stacknoise/haac/feature/onboarding/ui/ServerSection.kt` | One server with accent bar, name, address in mono font and a check mark when selected. |
| `ManualAddress` | `private fun ManualAddress(state: OnboardingUiState, actions: OnboardingActions)` | `feature/onboarding/src/main/kotlin/com/stacknoise/haac/feature/onboarding/ui/ServerSection.kt` | Other address…* link, or the URL field once it was tapped. |
| `CredentialFields` | `internal fun CredentialFields(state: OnboardingUiState, password: TextFieldState, actions: OnboardingActions)` | `feature/onboarding/src/main/kotlin/com/stacknoise/haac/feature/onboarding/ui/SignInFields.kt` | Username and password fields of the mockup; the password never leaves [password] as a String. |
| `CodeSection` | `internal fun CodeSection(state: OnboardingUiState, actions: OnboardingActions)` | `feature/onboarding/src/main/kotlin/com/stacknoise/haac/feature/onboarding/ui/SignInFields.kt` | MFA step: explanation and code field (concept 5.1). |
| `BridgeHint` | `internal fun BridgeHint()` | `feature/onboarding/src/main/kotlin/com/stacknoise/haac/feature/onboarding/ui/SignInFields.kt` | Install hint when HAAC Bridge is missing or outdated (concept 4.2 step 4, 14.1). |
| `FieldIcon` | `private fun FieldIcon(@DrawableRes icon: Int)` | `feature/onboarding/src/main/kotlin/com/stacknoise/haac/feature/onboarding/ui/SignInFields.kt` | Leading icon of an input field. |
| `fieldColors` | `internal fun fieldColors(): TextFieldColors` | `feature/onboarding/src/main/kotlin/com/stacknoise/haac/feature/onboarding/ui/SignInFields.kt` | Filled fields in the surface colour without an indicator line, as in the mockup. |

## com.stacknoise.haac.feature.settings.ui

| Symbol | Signature | File | Description |
| --- | --- | --- | --- |
| `SettingsScreen` | `fun SettingsScreen(onSignedOut: (String) -> Unit, viewModel: SettingsViewModel = hiltViewModel())` | `feature/settings/src/main/kotlin/com/stacknoise/haac/feature/settings/ui/SettingsScreen.kt` | Settings (concept 15.4); [onSignedOut] receives the id of the instance that was signed out. |
| `SettingsContent` | `fun SettingsContent(state: SettingsUiState, onSignOut: () -> Unit)` | `feature/settings/src/main/kotlin/com/stacknoise/haac/feature/settings/ui/SettingsScreen.kt` | Stateless layout of the settings. |
| `SettingsPreview` | `private fun SettingsPreview()` | `feature/settings/src/main/kotlin/com/stacknoise/haac/feature/settings/ui/SettingsScreen.kt` | Preview with a signed-in instance. |
| `ActiveInstance` | `data class ActiveInstance(val id: String, val name: String, val url: String, val userName: String)` | `feature/settings/src/main/kotlin/com/stacknoise/haac/feature/settings/ui/SettingsViewModel.kt` | The active instance as the settings show it. |
| `SettingsUiState` | `data class SettingsUiState(val instance: ActiveInstance? = null, val busy: Boolean = false, val signedOutServerId: String? = null)` | `feature/settings/src/main/kotlin/com/stacknoise/haac/feature/settings/ui/SettingsViewModel.kt` | What the settings screen shows. |
| `SettingsViewModel` | `class SettingsViewModel @Inject constructor(active: ActiveInstanceStore, servers: ServerDao, private val sessions: InstanceSessionFactory) : ViewModel()` | `feature/settings/src/main/kotlin/com/stacknoise/haac/feature/settings/ui/SettingsViewModel.kt` | Settings of the active instance; for now account and logout (concept 5.2, 15.4). |
| `SettingsViewModel.onSignOut` | `fun onSignOut()` | `feature/settings/src/main/kotlin/com/stacknoise/haac/feature/settings/ui/SettingsViewModel.kt` | Logout: revokes the refresh token in HA and deletes it locally (concept 5.2). The instance and its |
