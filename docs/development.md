# Developer guide – HAAC Android app

This guide is for people (and coding agents) who change the app in `stacknoise/haac-android`. It explains how the project is built and checked, and the rules every change has to follow: packages by topic, the factory pattern, error codes, the generated code index and the CI checks.

It is a practical summary. The specification is [concept.md](concept.md); the rules below are concept chapters 16 and 17. If this guide and the concept disagree, the concept wins. [CLAUDE.md](../CLAUDE.md) is the short version for coding agents.

The companion integration lives in `stacknoise/haac-bridge` and has its own guide ([development.md there](https://github.com/stacknoise/haac-bridge/blob/main/docs/development.md)). This repository contains no Python code.

## 1. Get started

Requirements:

- JDK 21 (the one bundled with Android Studio works) and the Android SDK (`compileSdk` 37, `targetSdk` 37, `minSdk` 28). The SDK levels are set only in `build-logic` (`AndroidSdk`), never in a module.
- Android Studio, or the Gradle wrapper on the command line. The SDK path goes into `local.properties` (not committed).

```bash
./gradlew assembleDebug      # both flavors: play and sideload
./gradlew test               # unit tests (JUnit 5)
./gradlew lint detekt        # Android Lint and Detekt
./gradlew codeIndex          # regenerate docs/code-index.md and docs/error-codes.md
./gradlew codeIndexCheck     # fail if those files are outdated or a KDoc summary is missing
./gradlew -p build-logic test   # tests of the code index generator
```

Before every pull request this must pass:

```bash
./gradlew assembleDebug lint detekt test codeIndexCheck
```

Notes:

- `:core:security:testDebugUnitTest` fails on a development machine (Android Keystore, `IOException` in `TokenFiles.kt`); it is green in CI. Use `--continue` to see all other results, and read Detekt errors from lines like `e: …MaxLineLength` in the output.
- On Windows, stop the Gradle daemons after a build (`./gradlew --stop`); a running daemon locks files in `build/` and the next build fails with "The process cannot access the file".
- The `play` and `sideload` flavors differ only in how the app is distributed (concept 14.4). The release APK is signed from environment variables (section 10).

## 2. Project layout

```text
app/                  Application, MainActivity, navigation graph; wires all modules together
core/common           shared helpers, theme (HaacTheme), dispatchers, time
core/error            ErrorCode, HaacException hierarchy, ErrorFactory, ErrorReporter
core/security         keystore, crypto, biometrics
core/network          HTTP, WebSocket, login, bridge channel, LAN discovery, address selection
core/database         Room database, DAOs, entities (schemas in core/database/schemas)
feature/onboarding    server entry, login
feature/instance      instances, switching
feature/layout        homes, levels (floors), rooms
feature/entities      picker, room grid, controls, detail, history, sync
feature/notifications notification list incl. errors
feature/schedules     schedule list, editor, detail, sync
feature/settings      settings, about, licenses
build-logic/          convention plugins and the code index generator
config/detekt/        Detekt overrides
gradle/libs.versions.toml   all versions; modules never declare versions themselves
docs/                 concept.md, code-index.md, error-codes.md, mockups, icons, screenshots
.github/workflows/    ci.yml, release.yml, codeql.yml
```

Dependency rules:

- A feature module depends on `core:common` and `core:error` (added by its convention plugin) and on the other `core:*` modules it needs. **Features never depend on each other.** Code two features need goes into a `core:*` module; they talk through interfaces defined there.
- `:app` depends on everything and holds nothing but wiring.

### Packages by topic (concept 17.1)

The root package is `com.stacknoise.haac`. Packages are organised by topic first, then by layer:

```text
com.stacknoise.haac.<core|feature>.<topic>.<ui|domain|data|di>
```

For example `com.stacknoise.haac.feature.layout.domain`. A class lives in exactly one topic. `ui` holds screens, ViewModels and Compose; `domain` the pure rules and models; `data` repositories, database and network access; `di` the Hilt modules.

### Convention plugins

A module's `build.gradle.kts` only applies a convention plugin and lists dependencies; it contains no Android or Kotlin settings.

| Plugin | Use |
| --- | --- |
| `haac.android.application` | `:app`: flavors, signing from environment variables |
| `haac.android.library` | any library module: SDK levels, Kotlin options (warnings are errors), JUnit 5, Detekt |
| `haac.android.compose` | Compose (BOM) and Material 3 |
| `haac.android.feature` | a `:feature:*` module: library + Compose + Hilt + `core:common` + `core:error` |
| `haac.hilt` | Hilt and KSP |

To add a module: `include(":feature:<topic>")` in `settings.gradle.kts`, a `build.gradle.kts` with `id("haac.android.feature")`, and `implementation(project(":feature:<topic>"))` in `app/build.gradle.kts`. The namespace is derived from the Gradle path.

## 3. Factory pattern (concept 17.2)

**Rule:** whenever the kind of an object depends on a type or on runtime data, a factory creates it. Outside a factory no code branches on the entity domain or the key type to construct objects. This keeps the places that need a change when Home Assistant gets a new domain down to one.

A factory is an interface with a default implementation, bound by Hilt, so tests can replace it with a fake:

```kotlin
/** Builds the controls of an entity from its domain, supported features and attributes. */
interface EntityControlFactory {
    /** The control of [entity]'s tile in [state], or null if it has none. */
    fun create(entity: ExposedEntity, state: EntityState): EntityControl?
}

/** [EntityControlFactory] for switch, sensor (no controls) and climate. */
class DefaultEntityControlFactory @Inject constructor() : EntityControlFactory {
    override fun create(entity: ExposedEntity, state: EntityState): EntityControl? =
        when (entity.domain) {            // the only `when (domain)` that builds controls
            EntityDomains.SWITCH -> toggle(state)
            EntityDomains.CLIMATE -> ClimateControls(entity.supportedFeatures, state).targetTemperature()
            else -> null
        }
}

/** Bindings of the entities feature. */
@Module
@InstallIn(SingletonComponent::class)
abstract class EntitiesModule {
    /** The controls of concept 8. */
    @Binds
    abstract fun bindEntityControlFactory(factory: DefaultEntityControlFactory): EntityControlFactory
}
```

(Shortened from `feature/entities/.../domain/EntityControlFactory.kt` and `.../di/EntitiesModule.kt`.)

The factories of the app:

| Factory | Creates |
| --- | --- |
| `EntityControlFactory` | control model per entity domain (switch, sensor, climate) |
| `TileFactory` | tile spec with default size per domain |
| `HistoryChartFactory` | history query (states or statistics) and chart kind per entity |
| `ServiceCallFactory` | typed `haac_bridge/call_service` requests, checked against `supported_features` |
| `BridgeMessageFactory` | WebSocket commands with message IDs |
| `HaWebSocketFactory` | WebSocket to `/api/websocket` of one address |
| `KeyFactory` / `CipherFactory` | Keystore keys and ciphers |
| `InstanceSessionFactory` | per-instance session: HTTP client, WebSocket, token store for one `serverId` |
| `EndpointSelector` | the address of an instance to connect to |
| `ErrorFactory` | `HaacException` from any caught `Throwable` |
| ViewModel factories (`@AssistedFactory`) | ViewModels with runtime parameters (`roomId`, `entityId`) |

Adding a factory:

1. Interface and `Default…` implementation in the `domain` (or `data`) package of its topic, both with KDoc.
2. `@Binds` in the topic's `di` module.
3. A test with the cases per type; use a fake of the interface in the tests of its users.
4. Add it to the table in concept 17.2 and regenerate the code index (section 5).

When a new entity domain is added, the change belongs in the factories above, not in screens or repositories.

## 4. Error codes (concept 17.3, 17.4)

Every error the app throws or passes on is a `HaacException` with an `ErrorCode`.

- **All codes are defined in one file:** `core/error/src/main/kotlin/com/stacknoise/haac/core/error/ErrorCode.kt`. Nowhere else.
- Format `HAAC-<AREA>-<NNN>`. Areas: `NET`, `AUTH`, `SEC`, `BRG`, `SYNC`, `DB`, `LAY`, `ENT`, `INST`, `DISC`, `SCH`, `APP`. A code is never reused or renumbered.
- `docs/error-codes.md` is **generated** from `ErrorCode.kt` and the string resources. Look codes up there; never edit it by hand.

An entry has a name that starts with its area, the code, a string resource for the user text, an action and a one-line technical description:

```kotlin
NET_UNREACHABLE(
    "HAAC-NET-001",
    R.string.error_net_unreachable,
    ErrorAction.RETRY,       // RETRY, SIGN_IN, OPEN_SETTINGS or NONE
    "Connect or request to the server failed (DNS, timeout, refused, I/O)",
),
```

**Adding an error code**

1. Take the next free number of the area and add the enum entry in `ErrorCode.kt` (not in the middle of existing numbering: never renumber).
2. Add the user text as `error_<name>` to `core/error/src/main/res/values/strings.xml` **and** `values-de/strings.xml`. Short, plain language, says what happened and what to do; no technical terms, passwords, tokens or full URLs. An apostrophe in an Android string must be written `\'`; better rephrase.
3. Throw the matching exception subclass with the code, e.g. `throw ValidationException(ErrorCode.LAY_NAME_MISSING)`. The hierarchy: `NetworkException`, `AuthException`, `KeystoreException`, `BridgeException`, `SyncException`, `StorageException`, `ValidationException` (areas LAY, ENT, INST, DISC) and `UnexpectedException` (`HAAC-APP-000`).
4. Run `./gradlew codeIndex` and commit `docs/error-codes.md`.
5. `ErrorCodeTest` checks that codes are unique, match the format, have a text and a description, and that the name starts with the area. It must stay green.

**Errors reported by the bridge** carry a HAB code. `ErrorFactory.fromBridgeError(habCode)` maps it with the table `DefaultErrorFactory.BRIDGE_CODES` (the same table as concept 18.3); unknown HAB codes become `HAAC-BRG-005`. The HAB code stays on the exception (`bridgeCode`) and is shown in the detail sheet. When the bridge gets a new HAB code that reaches the app, update `BRIDGE_CODES`, the table in concept 18.3 and the bridge's `APP_CODES`.

**Rules**

- Never throw plain `Exception`, `IllegalStateException` or `RuntimeException`.
- Catch low-level exceptions (`IOException`, `SSLException`, `SerializationException`, `SQLiteException`, `KeyPermanentlyInvalidatedException`) at the data-layer boundary and convert them with `ErrorFactory.from(throwable)`. Room calls can use `errorFactory.database { … }`. Code above the data layer only sees `HaacException`.
- Never catch or wrap `CancellationException`; `ErrorFactory.from` rethrows it.
- Never swallow an exception: either the UI handles it or it goes to `ErrorReporter.report(error, serverId)`, which creates the entry in the notification list (with the code). The same code within 10 minutes is grouped into one entry.
- Errors that block the screen are additionally shown there, always with the code.
- Uncaught exceptions are recorded as `HAAC-APP-000` by `CrashMarker`: only a marker file, never a stack trace or message (they could contain addresses or tokens).

## 5. Code index (concept 17.5)

`docs/code-index.md` lists **every class and every function** of the app with signature, file and a one-line description, grouped by package. It exists so that existing code is found and reused instead of written again.

- **Before writing code**, search `docs/code-index.md` for a function or class with the same purpose. Reuse or extend it.
- **Every** class and function, including private ones, has a one-line KDoc summary. `codeIndexCheck` fails on a missing one.
- After adding, renaming or removing a class or function: `./gradlew codeIndex`, and commit the regenerated `docs/code-index.md` and `docs/error-codes.md` **in the same commit**. CI (`codeIndexCheck`) fails otherwise.
- Never edit the two files by hand. After a merge or rebase conflict in `docs/code-index.md`, take either side and regenerate it; never merge it by hand.
- The generator lives in `build-logic` (`codeindex`, task registered by `CodeIndexConventionPlugin`) and has its own tests (`./gradlew -p build-logic test`).

## 6. No duplicate code (concept 17.6)

- Code needed in a second place is extracted into its own function **at that moment**, in the topic package or in `core:common` if several topics use it.
- CI runs PMD CPD over `app/src`, `core` and `feature` and fails from **100 duplicated tokens**. This includes tests: identical fakes (for example a `ScheduleDao`) belong in one shared test class (`FakeScheduleDao`).
- Detekt (`config/detekt/detekt.yml`): documented public classes and functions, lines of at most 120 characters, no magic numbers (`20.dp` is allowed), top-level constants in PascalCase (`StaleDelayMs`), at most 11 functions per file. Compose functions are exempt from the length and naming rules.
- The Kotlin compiler treats warnings as errors.

## 7. UI rules

- **Theme:** `HaacTheme` ("Salbei", light and dark) lives in `:core:common`. The colour tokens are in `HaacPalette`; `HaacColors.X` are `@Composable` getters that read the current palette. Add a new colour as a token to the palette (light and dark), never as `Color(0x…)` in a screen. In draw lambdas (`Canvas`, `drawBehind`, `DrawScope` functions) read the colour first in the composable and pass it in as a parameter.
- **Strings:** all UI text in `strings.xml`, with `values/` (English) **and** `values-de/` (German, informal "Du"). Do not change German nouns with `lowercase()` in code; use a full string instead. Texts that Home Assistant sends (HVAC, fan and preset names) stay as sent.
- **Title lines:** screen titles use `HaacScreenTitle` (one line, shrinks to 24 sp).
- Mockups under `docs/mockups` show layout; the concept (chapter 15) and the built screens are the reference for colours and wording.
- Launcher icons under `app/src/main/res` are generated artefacts; replace them only on an explicit request for a new icon.

## 8. Data and tests

- Room database `HaacDatabase` in `:core:database`; schemas are exported to `core/database/schemas` and committed. A schema change needs a version bump, a migration and a migration test. Data of different HA instances is separated by `serverId`.
- Settings that are not part of the database live in DataStore. No secrets in Room or DataStore; only the refresh token is stored, AES-GCM encrypted with an Android Keystore key.
- Tests: JUnit 5, MockK, Turbine, MockWebServer, Compose UI tests (concept 14.2). Test files mirror the package of the class they test. Use fakes of the factory interfaces and shared `Fake…` classes.

## 9. Git, pull requests and CI

- `main` is protected by rulesets: changes only by pull request, squash merge only, linear history, no force push or deletion, required checks (build/lint/detekt/tests, CPD, CodeQL with "high or higher" blocking).
- Branches `feat/<topic>`, `fix/<topic>`, `docs/<topic>`, `chore/<topic>`, `ci/<topic>`. Commit messages and PR titles follow Conventional Commits (`feat:`, `fix:`, `docs:`, `chore:`, optionally with a scope such as `feat(ui):`).
- Start every branch from the current `origin/main` (`git switch --no-track -c <name> origin/main`) and push with upstream set.
- **Stacked PRs and squash merges:** after the first PR is squash-merged, rebase the second one with `git rebase --onto origin/main <last commit of the first branch> <second branch>` and push with `--force-with-lease`.
- Workflows: `ci.yml` (build, lint, Detekt, unit tests, `codeIndexCheck`, build-logic tests; a separate CPD job), `codeql.yml` (Kotlin with an explicit Gradle build, plus the workflow files), `release.yml`.
- A GitHub default CodeQL setup and a CodeQL workflow in the repository exclude each other; this repository uses the workflow.

### The concept document

`docs/concept.md` here is the **leading copy** of the specification; `stacknoise/haac-bridge` holds an identical copy. A change goes first into this repository; after it is merged, the copy is taken over into the bridge repository in its own pull request (same blob). The online document (Claude Doc) is updated by hand. Read the matching chapter before changing behaviour; items in concept 14.5 ("Open points") are undecided: ask, or implement behind a clearly marked TODO.

## 10. Versions and releases

- The version is `appVersion` in `app/build.gradle.kts`. `versionName` equals it, `versionCode` is derived (`major * 10000 + minor * 100 + patch`; 0.2.1 is 201).
- Releasing:
  1. Raise `appVersion` in its own pull request (`chore(release): bump the version to X.Y.Z`) and merge it. Update the status line of the README.
  2. Check `main` once more, then tag the merge commit: `git tag -a vX.Y.Z <sha> -m vX.Y.Z` and `git push origin vX.Y.Z`.
  3. `release.yml` builds `assembleSideloadRelease`, verifies the signature with `apksigner` and attaches `haac-vX.Y.Z-sideload.apk` and its `.sha256` to the GitHub Release.
- Tags `v*` are protected (no deleting, moving or overwriting). A wrong tag cannot be corrected, so check the version first.
- Signing: the keystore and passwords exist only as secrets of the GitHub environment `release` (`HAAC_KEYSTORE_BASE64`, `HAAC_KEYSTORE_PASSWORD`, `HAAC_KEY_ALIAS`, `HAAC_KEY_PASSWORD`); the build reads them from environment variables. Never commit a keystore or `*.jks`.
- A breaking change of a `haac_bridge/*` command raises the bridge `api_version`; the app declares the range it supports.

## 11. Non-negotiable rules

- Never store, log or hard-code a password or token in plain text. No HTTP logging of `/auth/*` requests, also in debug builds.
- Fingerprint unlock uses `BiometricPrompt` with a `CryptoObject` (Class 3); a UI-only check is not acceptable.
- Login uses `client_id` `https://stacknoise.com/haac/` and `redirect_uri` `https://stacknoise.com/haac/auth-callback`. Do not change them; existing refresh tokens are bound to the `client_id`.
- The app talks to entities only through `haac_bridge/*` WebSocket commands, never through Home Assistant's generic state and service APIs.
- Local aliases, layout and tile sizes never write back to Home Assistant.
- Only add dependencies under Apache-2.0-compatible licenses (Apache-2.0, MIT, BSD), never GPL. No license headers in source files.
