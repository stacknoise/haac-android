# HAAC – HA Android Client (repo: stacknoise/haac-android)

Native Android client for Home Assistant (HA). This repository contains the Android app only; the companion HA custom integration ("HAAC Bridge", domain `haac_bridge`) lives in `stacknoise/haac-bridge` and is distributed via HACS. The app lets a HA user build their own homes / levels / rooms on the phone, place entities that the HA admin exposed to that user, and control them.

## Source of truth

- `docs/concept.md` is the full technical concept. Read the chapter that matches the task before writing code; do not work from this file alone.
- `docs/mockups/png/M-0x-*.png` shows one screen each; `docs/mockups/haac-mockups-1c.html` is the interactive design source (open it in a browser). Chapter 15 of the concept describes every screen in text and lists the design tokens.
- Precedence: concept chapters 1–14 define behaviour and data; mockups define layout, style and wording. If they disagree, follow the concept; `docs/concept.md` 15.5 lists the decisions that override the mockups.
- Items in concept 14.5 ("Open points") are undecided. Do not pick an answer silently: ask, or implement behind a clearly marked TODO.

## Key domain rules

- A home is mandatory: every level and every room belongs to exactly one home. A room links either to a level of its home or directly to the home; homes without levels are allowed (concept 6.1).
- An entity can be assigned to any number of rooms; within one room only once (7.2).
- An entity removed or no longer shared in HA stays in its rooms as an inactive tile with a warning until the user removes it, and cannot be assigned again (7.4).

## Where things are specified

| Topic | Concept chapter |
| --- | --- |
| Scope v1, assumptions | 1 |
| Components and how they talk | 2 |
| App modules, tech stack | 3 |
| Server URL, LAN discovery, multiple instances, switching | 4 |
| Login, token storage, fingerprint, unlock window, app lock | 5 |
| Homes, levels (floors), rooms | 6 |
| Entities: picker, rooms, tile sizes, aliases, removed entities | 7 |
| Switch, sensor, climate controls | 8 |
| Sync on start / switch, notification list | 9 |
| HACS integration, per-user YAML exposure | 10 |
| HTTPS and WebSocket API (`haac_bridge/*`) | 11 |
| Room database schema | 12 |
| Threat model, security rules | 13 |
| Errors, offline, tests, distribution, open points | 14 |
| Mockups, design tokens, screen specs | 15 |
| GitHub repos, branches, CI/CD, secrets | 16 |

## Repository layout

```text
CLAUDE.md
docs/
  concept.md           # full specification (leading copy, exported from the concept document)
  mockups/             # haac-mockups-1c.html, png/M-0x-*.png
  icons/               # playstore-icon-512.png, launcher previews (concept 15.6)
app/src/main/res/      # launcher icons (adaptive + legacy mipmaps) already in place; do not regenerate or replace them
app/  core/security/  core/network/  core/database/
feature/onboarding/  feature/layout/  feature/entities/  feature/settings/
gradle/libs.versions.toml
.github/workflows/     # ci.yml, release.yml (concept 16.5)
LICENSE  NOTICE        # Apache-2.0 (concept 16.2)
```

- The integration is NOT part of this repo. For its API, read concept chapters 10 and 11; do not add Python code here.
- Branches, commits, versions and CI follow concept chapter 16 (Conventional Commits, tags `vX.Y.Z`, protected `main`).

## Non-negotiable rules

- Never store, log or hard-code a password or token in plaintext. Only the HA refresh token is persisted, AES-GCM encrypted with an Android Keystore key (concept 5.2, 5.3). No secrets in Room or DataStore.
- Login: `client_id` `https://stacknoise.com/haac/`, `redirect_uri` `https://stacknoise.com/haac/auth-callback` (same host, so HA needs no internet access; concept 5.1, 16.8). Do not change these URLs: existing refresh tokens are bound to the client_id.
- Fingerprint unlock must use `BiometricPrompt` with a `CryptoObject` (Class 3); a UI-only check is not acceptable (5.4).
- No HTTP logging of `/auth/*` requests, also in debug builds.
- The app talks to entities only through `haac_bridge/*` WebSocket commands, never through HA's generic state/service APIs (11, 13.1).
- Local aliases, layout and tile sizes never write back to HA.
- Data of different HA instances is strictly separated by `serverId` (4.4, 12).

## Conventions

- Kotlin, Jetpack Compose, Material 3, Hilt, Room, OkHttp, kotlinx.serialization (concept 3.3). minSdk 28.
- Manifest: `android:icon="@mipmap/ic_launcher"`, `android:roundIcon="@mipmap/ic_launcher_round"` (concept 15.6).
- UI: dark theme `HaacTheme` with the tokens from concept 15.2; font Inter. UI says "Level", code says `Floor`.
- UI strings in `strings.xml` (English); mockup texts are sample data.
- Tests as in concept 14.2 (JUnit 5, Turbine, MockK, MockWebServer, Compose UI tests).
- Signing keys and Play credentials exist only as GitHub Actions secrets (concept 16.6); never commit keystores or `*.jks`.
- License: Apache-2.0 (concept 16.2). No license headers in source files. Only add dependencies under Apache-2.0-compatible licenses (Apache-2.0, MIT, BSD), never GPL. The app lists all library licenses via the AboutLibraries plugin under Settings → About.
