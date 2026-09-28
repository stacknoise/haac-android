# Error codes – HAAC Android

> GENERATED FILE – do not edit by hand. Regenerated from `ErrorCode.kt` and `strings.xml` with `./gradlew codeIndex` (concept 17.3, 17.5).

Every error of the app carries one of these codes (format `HAAC-<AREA>-<NNN>`) and appears in the notification list (17.4).

| Code | User message | Action | Technical description |
| --- | --- | --- | --- |
| HAAC-NET-001 | The server is not reachable. Check your connection and try again. | RETRY | Connect or request to the server failed (DNS, timeout, refused, I/O) |
| HAAC-NET-002 | The connection to the server was lost. Reconnecting… | NONE | WebSocket closed unexpectedly; reconnect with back-off is running |
| HAAC-NET-003 | The server's certificate has changed. For your safety the connection was blocked. | OPEN_SETTINGS | Pinned certificate key does not match the server's key |
| HAAC-NET-004 | This address is not a Home Assistant server. Check the address and try again. | NONE | GET /auth/providers did not return a HA auth provider list |
| HAAC-NET-005 | This is not a valid address. Check it and try again. | NONE | Server URL could not be parsed after normalisation |
| HAAC-NET-006 | Unencrypted connections are only allowed in your home network. Use an https address. | NONE | http:// to a host outside private address ranges and .local (concept 4.3) |
| HAAC-NET-007 | The server's certificate is not trusted, so the connection was blocked. | NONE | TLS handshake failed: certificate not trusted by the system (self-signed pinning comes later) |
| HAAC-AUTH-001 | Username or password is wrong. | NONE | HA login flow rejected username or password |
| HAAC-AUTH-002 | The verification code is wrong. | NONE | HA login flow rejected the MFA code |
| HAAC-AUTH-003 | Your sign-in has expired. Please sign in again. | SIGN_IN | Refresh token revoked or expired, or the bridge saw no user (HAB-AUTH-001) |
| HAAC-AUTH-004 | This server does not allow sign-in with username and password. | NONE | Server has no 'homeassistant' auth provider; browser fallback (5.1) not built yet |
| HAAC-AUTH-005 | Sign-in took too long or had too many wrong codes. Please start again. | NONE | HA aborted the login flow (too_many_retry, login_expired) or no longer knows the flow |
| HAAC-AUTH-006 | Home Assistant does not let this user sign in here. Ask your administrator. | NONE | HTTP 403 from /auth/login_flow or /auth/token: user inactive, local-only or IP banned |
| HAAC-SEC-001 | Your fingerprints have changed. Please sign in with your password. | SIGN_IN | Fingerprint key invalidated by a biometric enrolment change, or missing |
| HAAC-SEC-002 | Secure storage on this device is not available. | NONE | Android Keystore could not create or use the key |
| HAAC-SEC-003 | The app is locked. Unlock it with your fingerprint. | NONE | Refresh token is fingerprint-protected and the app is locked |
| HAAC-SEC-004 | Fingerprint unlock is not available right now. Try again later or sign in with your password. | NONE | BiometricPrompt error: too many attempts, no strong biometric enrolled or sensor unavailable |
| HAAC-BRG-001 | HAAC Bridge is not installed on this server. | OPEN_SETTINGS | haac_bridge/info returned unknown_command |
| HAAC-BRG-002 | HAAC Bridge on the server needs an update. | NONE | Bridge api_version outside the range the app supports |
| HAAC-BRG-003 | You are not allowed to control this device. | NONE | Bridge rejected a service call to a non-exposed entity (HAB-SVC-001) |
| HAAC-BRG-004 | This action is not available for this device. | NONE | Bridge rejected a service outside the entity's domain (HAB-SVC-002) |
| HAAC-BRG-005 | Home Assistant could not carry out the action. Please try again. | RETRY | Bridge or HA failed to carry out a request, or an unknown HAB code |
| HAAC-BRG-006 | History is not available on this server. | NONE | Recorder or history not available on the server (HAB-HIST-001) |
| HAAC-ENT-001 | This device no longer exists in Home Assistant. | NONE | Entity no longer exists in HA (HAB-ENT-001) |
| HAAC-DB-001 | Your changes could not be saved. Please try again. | RETRY | Room write failed (SQLiteException) |
| HAAC-APP-000 | Something went wrong. | NONE | Exception without an error code reached ErrorFactory |
