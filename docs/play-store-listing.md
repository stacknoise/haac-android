# Google Play listing – HAAC (HA Android Client)

Texts and answers for the Play Console (concept 14.4). Nothing in this file is a promise to Google that the maintainer has not checked: the answers in sections 4 and 5 describe what the app does today and must be re-checked against the Play Console wording before they are submitted. Review them again whenever the app starts to collect or send new data.

## 1. Store settings

| Field | Value |
| --- | --- |
| App name (max. 30 characters) | `HAAC – HA Android Client` |
| Package name | `com.stacknoise.haac` |
| Default language | German (Germany), because the graphics exist only in German; add English (United States) with the English texts below, it then shows the German graphics until English ones are uploaded |
| App or game | App |
| Free or paid | Free |
| Category | House & Home (alternative: Tools) |
| Contact e-mail | the address on the privacy page (`google_play@…`) |
| Website | https://stacknoise.com/haac/ |
| Privacy policy | https://stacknoise.com/haac/privacy/ (source text: [privacy-policy.md](privacy-policy.md)) |
| Ads | No |
| Target audience | 18 and older (not designed for children) |

The name does not contain "Home Assistant"; the description says that HAAC is not affiliated with it (concept 16.2).

## 2. Short description (max. 80 characters)

**English**

```text
Control the Home Assistant devices shared with you, with schedules.
```

**Deutsch**

```text
Steuere die dir freigegebenen Home-Assistant-Geräte, mit Zeitplänen.
```

## 3. Full description (max. 4000 characters)

**English**

```text
HAAC lets you control the Home Assistant devices that your administrator shared with you, and nothing else.

Home Assistant has no per-entity permissions for users, so the official Companion App shows everything a signed-in user can reach. HAAC works differently: the administrator decides, user by user, which switches, sensors and climate devices the app may show and control. A user who is not configured sees nothing. This needs the free integration HAAC Bridge on your Home Assistant server (installed through HACS, https://github.com/stacknoise/haac-bridge).

No server at hand? Tap "Try the demo" on the first screen to look around with sample data.

Your home, your way
• Build your own homes, levels and rooms on the phone and place the shared devices in as many rooms as you like.
• Tiles in three sizes, edit mode with drag and drop, local names that never change Home Assistant.
• Import levels and rooms from the areas of Home Assistant if you want a head start.

Control
• Switches with a toggle on the tile.
• Climate devices with a target temperature dial; on the detail screen HVAC mode, presets, fan, swing, humidity and target range as far as the device supports them.
• Clear messages when an action is not available, and a "stale" indication when the connection is down.

Schedules
• "Every weekday at 06:45 turn the light on": turn on, turn off or toggle, at a fixed time on chosen weekdays or at sunrise or sunset with an offset.
• Schedules run on your Home Assistant server, so they work with the app closed or the phone off.
• Administrators see and manage all schedules.

History
• Charts for 24 hours, 7 days or a custom period for your sensors, switches and climate devices.

Notifications
• A list of new or removed shared devices, schedules that were paused, and errors, each with a plain-language message and an error code.

Secure by design
• Only the sign-in token is stored, encrypted with the Android Keystore; your password is never stored.
• Optional fingerprint unlock with a cryptographic key, an unlock window and an app lock.
• Sign-in and settings screens are hidden from screenshots.
• Self-signed certificates can be trusted on first use with a pinned key; a later change blocks the connection.
• Several Home Assistant instances, each with an internal and an external address, chosen automatically.
• No analytics, no ads, no tracking. The app talks only to your own Home Assistant server.

HAAC limits what the app shows and controls. It is not a Home Assistant permission: use a dedicated non-admin user for each person.

HAAC is open source (Apache License 2.0): https://github.com/stacknoise/haac-android

HAAC is not affiliated with or endorsed by Home Assistant, the Open Home Foundation or Nabu Casa.
```

**Deutsch**

```text
Mit HAAC steuerst du die Home-Assistant-Geräte, die dir dein Administrator freigegeben hat, und sonst nichts.

Home Assistant kennt keine Rechte pro Entität für Benutzer, deshalb zeigt die offizielle Companion App alles, was ein angemeldeter Benutzer erreichen kann. HAAC arbeitet anders: Der Administrator legt Benutzer für Benutzer fest, welche Schalter, Sensoren und Klimageräte die App anzeigen und steuern darf. Ein nicht eingerichteter Benutzer sieht nichts. Dafür braucht dein Home-Assistant-Server die kostenlose Integration HAAC Bridge (Installation über HACS, https://github.com/stacknoise/haac-bridge).

Gerade kein Server zur Hand? Tippe auf dem ersten Bildschirm auf „Demo ausprobieren“ und sieh dich mit Beispieldaten um.

Dein Zuhause, auf deine Art
• Baue deine eigenen Orte, Ebenen und Räume auf dem Telefon und platziere die freigegebenen Geräte in beliebig vielen Räumen.
• Kacheln in drei Größen, Bearbeitungsmodus mit Drag-and-drop, lokale Namen, die Home Assistant nie verändern.
• Importiere Ebenen und Räume aus den Bereichen von Home Assistant, wenn du einen Vorsprung willst.

Steuern
• Schalter mit einem Umschalter direkt auf der Kachel.
• Klimageräte mit einem Drehregler für die Zieltemperatur; auf dem Detailbildschirm HVAC-Modus, Voreinstellungen, Lüfter, Swing, Feuchte und Zielbereich, soweit das Gerät sie unterstützt.
• Klare Meldungen, wenn eine Aktion nicht möglich ist, und ein Hinweis „veraltet“, wenn die Verbindung fehlt.

Zeitpläne
• „Werktags um 06:45 das Licht einschalten“: einschalten, ausschalten oder umschalten, zu einer festen Uhrzeit an gewählten Wochentagen oder bei Sonnenauf- oder -untergang mit Versatz.
• Zeitpläne laufen auf deinem Home-Assistant-Server, also auch bei geschlossener App oder ausgeschaltetem Telefon.
• Administratoren sehen und verwalten alle Zeitpläne.

Verlauf
• Diagramme für 24 Stunden, 7 Tage oder einen eigenen Zeitraum für Sensoren, Schalter und Klimageräte.

Benachrichtigungen
• Eine Liste neuer oder entfernter Geräte, pausierter Zeitpläne und Fehler, jeweils mit verständlicher Meldung und Fehlercode.

Sicher gebaut
• Gespeichert wird nur das Anmelde-Token, verschlüsselt mit dem Android Keystore; dein Passwort wird nie gespeichert.
• Optionales Entsperren per Fingerabdruck mit kryptografischem Schlüssel, Entsperrfenster und App-Sperre.
• Anmelde- und Einstellungsbildschirme werden in Screenshots ausgeblendet.
• Selbstsignierte Zertifikate können beim ersten Mal mit festgelegtem Schlüssel vertraut werden; eine spätere Änderung blockiert die Verbindung.
• Mehrere Home-Assistant-Instanzen, jeweils mit interner und externer Adresse, automatisch gewählt.
• Keine Analyse, keine Werbung, kein Tracking. Die App spricht nur mit deinem eigenen Home-Assistant-Server.

HAAC begrenzt, was die App anzeigt und steuert. Es ist keine Berechtigung in Home Assistant: Verwende für jede Person einen eigenen Benutzer ohne Administratorrechte.

HAAC ist Open Source (Apache License 2.0): https://github.com/stacknoise/haac-android

HAAC steht in keiner Verbindung zu Home Assistant, der Open Home Foundation oder Nabu Casa und wird von ihnen nicht unterstützt.
```

## 4. Declarations

**App access.** Reviewers need no account and no server: the app has a built-in demo (version 0.3.0 and newer). Choose "All or some functionality is restricted" and give this instruction: "No sign-in is needed. Open the app and tap *Try the demo* on the first screen. The demo works without a network and contains sample data and prefilled rooms: open a room, tap a tile to switch it, open a tile for its detail and history, and open *Schedules*. To leave the demo, open *Settings*, remove the instance *Demo* and the first screen returns." The production release must contain a build with the demo, so the internal test of 0.4.0 (the first version with a prefilled demo) comes first.

**Permissions** (merged manifest; none of them needs a special declaration form):

| Permission | Why |
| --- | --- |
| `INTERNET` | Talks to the user's Home Assistant server |
| `ACCESS_NETWORK_STATE` | Detects the network, to choose between the internal and the external address |
| `ACCESS_LOCAL_NETWORK` (Android 17) | Finds Home Assistant servers in the home network and connects to private addresses |
| `USE_BIOMETRIC`, `USE_FINGERPRINT` (both from the biometric library) | Optional fingerprint unlock |

**Cleartext traffic.** The app allows `http://` only to private addresses (home network) after a warning; `https://` is the default. If the console asks, this is why the network security config permits cleartext at system level and the rule is enforced in code (concept 4.3).

**Content rating (questionnaire).** Category: utility / productivity app. No violence, no sexual content, no profanity, no controlled substances, no gambling, no user-generated content shared between users, no location sharing, no purchases. Expected rating: everyone / PEGI 3.

**Target audience and content.** Age group 18 and older only. Not designed for children; not a "Designed for Families" app. No ads.

**Government apps, financial features, health:** none (answer No to each).

**Advertising ID:** not used (answer No; the manifest does not declare `AD_ID`).

## 5. Data safety form

What the app does, as facts (concept 13, 5.2, 12):

- The developer runs no server and receives no data from the app. There is no analytics, no crash reporting, no advertising, no third-party SDK that sends data.
- Data stored on the device: the sign-in token (encrypted with the Android Keystore), instance addresses and names, the user's own layout, local names, cached entities and schedules, notifications, settings. Excluded from cloud backup and device transfer.
- Data sent off the device: sign-in data and commands to **the Home Assistant server of the user**, which the user (or their administrator) operates and chooses. Passwords are sent there once at sign-in and never stored.
- `https://` is the default; `http://` is allowed to private addresses after a warning.

Suggested answers:

| Question | Answer | Note |
| --- | --- | --- |
| Does the app collect or share any of the required user data types? | **Yes: collected, not shared** | Everything that leaves the device goes to the Home Assistant server of the user; the developer and third parties receive nothing (decision below) |
| Data types | **Personal info → User IDs** (the Home Assistant account name); **App activity → App interactions** (commands such as switching a device or changing a schedule) | Both: purpose *App functionality*, collection required, not shared. If the Console lists a type for sign-in credentials, tick it too; the password is sent once at sign-in and never stored |
| Is all data encrypted in transit? | **No** | `http://` to private addresses is possible (concept 4.3); answering Yes would not be true |
| Can users request that their data is deleted? | Yes, in the app | Removing an instance revokes the token and deletes all its local data; uninstalling deletes everything. The developer holds no data |
| Independent security review | No | |

**Decision (maintainer, 3 October 2026): option 2, declare what goes to the user's own server.** Google defines "collect" as transmitting data off the device ([Provide information for Google Play's Data safety section](https://support.google.com/googleplay/android-developer/answer/10787469)), and the page names no exemption for a server the user operates. "No data collected" would therefore be hard to defend; the honest answer is the table above. It matches the privacy policy, which says that sign-in and commands go to the user's own server and that nothing goes to the developer or to a third party.

- The demo (version 0.3.0 and newer) sends nothing at all.
- The names of the data types follow Google's help page. If the Console words them differently, take the matching type and update this table and, if needed, the privacy policy in the same pull request.
- Encryption in transit stays **No**: Google allows "Yes" only if it applies to all data the app transmits, and `http://` to private addresses is possible after a warning (concept 4.3).

## 6. Graphics

| Asset | Requirement | State |
| --- | --- | --- |
| App icon | 512 × 512 PNG, up to 1 MB | Ready: `docs/icons/playstore-icon-512.png` |
| Phone screenshots | 2 to 8; JPEG or 24-bit PNG; each side 320 to 3840 px; the longer side at most twice the shorter | Ready, German: six files in `docs/play/de/screenshots` (1100 × 2150, 24-bit PNG): rooms, places, add entities, edit layout, schedules, sign in. There is no English set yet |
| Feature graphic | 1024 × 500 JPEG or 24-bit PNG | Ready, German: `docs/play/de/feature-graphic.png` |
| 7-inch / 10-inch tablet screenshots | Optional | Tablet and landscape are not designed (concept 15.4) |

Screenshots to submit, in this order: Rooms, Places, Add entities, Edit layout, Schedules, Sign in. Do not show real names or addresses. The files are generated from phone screenshots taken in the German app (the IP address on the sign-in screen is pixelated) (headline above the unchanged screen, 2:1 limit respected); the generated PNGs are the ones to upload.

## 7. Release steps in the Play Console

1. Create the app (name, default language, free, declarations).
2. Enrol in **Play App Signing** and upload the existing app signing key (concept 14.4) so that the Play build and the sideload APK carry the same signature. The upload certificate fingerprint is printed by `release.yml` in the step "Name the Play bundle and verify its signature".
3. Complete the store listing (sections 1 to 3 and 6), the declarations (4), the data safety form (5), the content rating and the target audience.
4. Upload `haac-vX.Y.Z-play.aab` (a workflow artifact of the Release run) to the **internal testing** track and install it from there on a real phone.
5. After the internal test, go to closed testing (new personal developer accounts must run a closed test with a minimum number of testers for a minimum number of days before production; check the current rule in the console), then production.
6. Keep `versionCode` strictly increasing: it is derived from `appVersion` (0.2.1 is 201).
