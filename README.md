# HAAC – HA Android Client

Native Android client for Home Assistant. You build your own homes, levels and rooms on the phone and place the entities your Home Assistant admin shared with you through the companion integration [HAAC Bridge](https://github.com/stacknoise/haac-bridge).

> This project is not affiliated with or endorsed by Home Assistant, the Open Home Foundation or Nabu Casa.

**Status:** early development. The project skeleton is in place; no features yet.

- Supported entity domains in v1: switch, sensor, climate.
- Android 9 (API 28) or newer.
- Requires Home Assistant 2026.9.0 or newer with HAAC Bridge installed.

## Build

Requirements: JDK 21 (e.g. the one bundled with Android Studio) and the Android SDK.

```bash
./gradlew assembleDebug        # both flavors: play and sideload
./gradlew lint detekt test     # checks and unit tests
./gradlew codeIndex            # regenerate docs/code-index.md and docs/error-codes.md
```

Read [CLAUDE.md](CLAUDE.md) and [docs/concept.md](docs/concept.md) before contributing.

## Security

See [SECURITY.md](SECURITY.md).

## License

[Apache License 2.0](LICENSE). See [NOTICE](NOTICE). The bundled Inter font is licensed under the SIL Open Font License 1.1 ([core/common/FONT-LICENSE-Inter.txt](core/common/FONT-LICENSE-Inter.txt)).
