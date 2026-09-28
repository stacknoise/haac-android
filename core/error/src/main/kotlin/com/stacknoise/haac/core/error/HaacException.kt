package com.stacknoise.haac.core.error

/**
 * Base of every exception the app throws or passes on (concept 17.3).
 *
 * `bridgeCode` holds the HAB code when the error was reported by HAAC Bridge (concept 18.3).
 */
sealed class HaacException(
    val code: ErrorCode,
    cause: Throwable? = null,
    val bridgeCode: String? = null,
) : Exception(code.description, cause)

/** Connection and request errors (area NET). */
class NetworkException(code: ErrorCode, cause: Throwable? = null) : HaacException(code, cause)

/** Login and token errors (area AUTH). */
class AuthException(code: ErrorCode, cause: Throwable? = null, bridgeCode: String? = null) :
    HaacException(code, cause, bridgeCode)

/** Keystore and biometric errors (area SEC). */
class KeystoreException(code: ErrorCode, cause: Throwable? = null) : HaacException(code, cause)

/** Errors reported by or about HAAC Bridge (area BRG). */
class BridgeException(code: ErrorCode, cause: Throwable? = null, bridgeCode: String? = null) :
    HaacException(code, cause, bridgeCode)

/** Synchronisation errors (area SYNC). */
class SyncException(code: ErrorCode, cause: Throwable? = null) : HaacException(code, cause)

/** Local database errors (area DB). */
class StorageException(code: ErrorCode, cause: Throwable? = null) : HaacException(code, cause)

/** Invalid layout or entity operations (areas LAY, ENT, INST, DISC). */
class ValidationException(code: ErrorCode, cause: Throwable? = null, bridgeCode: String? = null) :
    HaacException(code, cause, bridgeCode)

/** Anything without its own code (HAAC-APP-000). */
class UnexpectedException(cause: Throwable? = null) : HaacException(ErrorCode.APP_UNEXPECTED, cause)
