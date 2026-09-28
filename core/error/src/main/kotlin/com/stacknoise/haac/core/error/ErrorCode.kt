package com.stacknoise.haac.core.error

import androidx.annotation.StringRes

/**
 * Every error code of the app (concept 17.3); the only place where codes are defined.
 *
 * A new error gets the next free number of its area. Codes are never reused or renumbered.
 */
enum class ErrorCode(
    val code: String,
    @param:StringRes val message: Int,
    val action: ErrorAction,
    val description: String,
) {
    NET_UNREACHABLE(
        "HAAC-NET-001",
        R.string.error_net_unreachable,
        ErrorAction.RETRY,
        "Connect or request to the server failed (DNS, timeout, refused, I/O)",
    ),
    NET_CONNECTION_LOST(
        "HAAC-NET-002",
        R.string.error_net_connection_lost,
        ErrorAction.NONE,
        "WebSocket closed unexpectedly; reconnect with back-off is running",
    ),
    NET_CERTIFICATE_CHANGED(
        "HAAC-NET-003",
        R.string.error_net_certificate_changed,
        ErrorAction.OPEN_SETTINGS,
        "Pinned certificate key does not match the server's key",
    ),
    NET_NOT_HOME_ASSISTANT(
        "HAAC-NET-004",
        R.string.error_net_not_home_assistant,
        ErrorAction.NONE,
        "GET /auth/providers did not return a HA auth provider list",
    ),
    NET_INVALID_ADDRESS(
        "HAAC-NET-005",
        R.string.error_net_invalid_address,
        ErrorAction.NONE,
        "Server URL could not be parsed after normalisation",
    ),
    NET_CLEARTEXT_NOT_ALLOWED(
        "HAAC-NET-006",
        R.string.error_net_cleartext_not_allowed,
        ErrorAction.NONE,
        "http:// to a host outside private address ranges and .local (concept 4.3)",
    ),
    NET_CERTIFICATE_UNTRUSTED(
        "HAAC-NET-007",
        R.string.error_net_certificate_untrusted,
        ErrorAction.NONE,
        "TLS handshake failed: certificate not trusted by the system (self-signed pinning comes later)",
    ),
    AUTH_INVALID_CREDENTIALS(
        "HAAC-AUTH-001",
        R.string.error_auth_invalid_credentials,
        ErrorAction.NONE,
        "HA login flow rejected username or password",
    ),
    AUTH_INVALID_MFA_CODE(
        "HAAC-AUTH-002",
        R.string.error_auth_invalid_mfa_code,
        ErrorAction.NONE,
        "HA login flow rejected the MFA code",
    ),
    AUTH_SESSION_EXPIRED(
        "HAAC-AUTH-003",
        R.string.error_auth_session_expired,
        ErrorAction.SIGN_IN,
        "Refresh token revoked or expired, or the bridge saw no user (HAB-AUTH-001)",
    ),
    AUTH_PASSWORD_LOGIN_UNAVAILABLE(
        "HAAC-AUTH-004",
        R.string.error_auth_password_login_unavailable,
        ErrorAction.NONE,
        "Server has no 'homeassistant' auth provider; browser fallback (5.1) not built yet",
    ),
    AUTH_SIGN_IN_ABORTED(
        "HAAC-AUTH-005",
        R.string.error_auth_sign_in_aborted,
        ErrorAction.NONE,
        "HA aborted the login flow (too_many_retry, login_expired) or no longer knows the flow",
    ),
    AUTH_USER_BLOCKED(
        "HAAC-AUTH-006",
        R.string.error_auth_user_blocked,
        ErrorAction.NONE,
        "HTTP 403 from /auth/login_flow or /auth/token: user inactive, local-only or IP banned",
    ),
    SEC_BIOMETRICS_CHANGED(
        "HAAC-SEC-001",
        R.string.error_sec_biometrics_changed,
        ErrorAction.SIGN_IN,
        "Fingerprint key invalidated by a biometric enrolment change, or missing",
    ),
    SEC_STORAGE_UNAVAILABLE(
        "HAAC-SEC-002",
        R.string.error_sec_storage_unavailable,
        ErrorAction.NONE,
        "Android Keystore could not create or use the key",
    ),
    SEC_LOCKED(
        "HAAC-SEC-003",
        R.string.error_sec_locked,
        ErrorAction.NONE,
        "Refresh token is fingerprint-protected and the app is locked",
    ),
    SEC_BIOMETRIC_UNAVAILABLE(
        "HAAC-SEC-004",
        R.string.error_sec_biometric_unavailable,
        ErrorAction.NONE,
        "BiometricPrompt error: too many attempts, no strong biometric enrolled or sensor unavailable",
    ),
    BRG_NOT_INSTALLED(
        "HAAC-BRG-001",
        R.string.error_brg_not_installed,
        ErrorAction.OPEN_SETTINGS,
        "haac_bridge/info returned unknown_command",
    ),
    BRG_UPDATE_REQUIRED(
        "HAAC-BRG-002",
        R.string.error_brg_update_required,
        ErrorAction.NONE,
        "Bridge api_version outside the range the app supports",
    ),
    BRG_NOT_ALLOWED(
        "HAAC-BRG-003",
        R.string.error_brg_not_allowed,
        ErrorAction.NONE,
        "Bridge rejected a service call to a non-exposed entity (HAB-SVC-001)",
    ),
    BRG_ACTION_NOT_AVAILABLE(
        "HAAC-BRG-004",
        R.string.error_brg_action_not_available,
        ErrorAction.NONE,
        "Bridge rejected a service outside the entity's domain (HAB-SVC-002)",
    ),
    BRG_ACTION_FAILED(
        "HAAC-BRG-005",
        R.string.error_brg_action_failed,
        ErrorAction.RETRY,
        "Bridge or HA failed to carry out a request, or an unknown HAB code",
    ),
    BRG_HISTORY_UNAVAILABLE(
        "HAAC-BRG-006",
        R.string.error_brg_history_unavailable,
        ErrorAction.NONE,
        "Recorder or history not available on the server (HAB-HIST-001)",
    ),
    ENT_NOT_FOUND(
        "HAAC-ENT-001",
        R.string.error_ent_not_found,
        ErrorAction.NONE,
        "Entity no longer exists in HA (HAB-ENT-001)",
    ),
    DB_SAVE_FAILED(
        "HAAC-DB-001",
        R.string.error_db_save_failed,
        ErrorAction.RETRY,
        "Room write failed (SQLiteException)",
    ),
    APP_UNEXPECTED(
        "HAAC-APP-000",
        R.string.error_app_unexpected,
        ErrorAction.NONE,
        "Exception without an error code reached ErrorFactory",
    ),
    ;

    /** Area part of the code, e.g. `NET` for `HAAC-NET-001`. */
    val area: String get() = code.split('-')[1]
}

/** The one action button an error entry offers besides Dismiss (concept 17.4). */
enum class ErrorAction {
    RETRY,
    SIGN_IN,
    OPEN_SETTINGS,
    NONE,
}
