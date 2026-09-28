package com.stacknoise.haac.core.error

import android.database.SQLException
import android.security.keystore.KeyPermanentlyInvalidatedException
import java.io.IOException
import java.security.GeneralSecurityException
import java.security.ProviderException
import javax.inject.Inject
import javax.net.ssl.SSLHandshakeException
import javax.net.ssl.SSLPeerUnverifiedException
import kotlin.coroutines.cancellation.CancellationException

/** Turns caught throwables and bridge error replies into [HaacException]s (concept 17.2, 17.3). */
interface ErrorFactory {
    /** Returns a [HaacException] for [throwable]; rethrows [CancellationException] unchanged. */
    fun from(throwable: Throwable): HaacException

    /** Returns the [HaacException] for a bridge error reply with HAB code [habCode] (concept 18.3). */
    fun fromBridgeError(habCode: String): HaacException
}

/** Runs a Room call and converts SQLite errors to HAAC-DB-001 (concept 17.3). */
suspend fun <T> ErrorFactory.database(block: suspend () -> T): T = try {
    block()
} catch (e: SQLException) {
    throw from(e)
}

/** Default mapping; the only place that decides which code an error gets. */
class DefaultErrorFactory @Inject constructor() : ErrorFactory {
    /** Maps low-level exceptions by type; everything unknown becomes HAAC-APP-000. */
    override fun from(throwable: Throwable): HaacException = when (throwable) {
        is CancellationException -> throw throwable
        is HaacException -> throwable
        is KeyPermanentlyInvalidatedException -> KeystoreException(ErrorCode.SEC_BIOMETRICS_CHANGED, throwable)
        is GeneralSecurityException, is ProviderException ->
            KeystoreException(ErrorCode.SEC_STORAGE_UNAVAILABLE, throwable)
        is SQLException -> StorageException(ErrorCode.DB_SAVE_FAILED, throwable)
        is SSLHandshakeException, is SSLPeerUnverifiedException ->
            NetworkException(ErrorCode.NET_CERTIFICATE_UNTRUSTED, throwable)
        is IOException -> NetworkException(ErrorCode.NET_UNREACHABLE, throwable)
        else -> UnexpectedException(throwable)
    }

    /** Looks the HAB code up in [BRIDGE_CODES] and keeps it on the exception for the detail sheet. */
    override fun fromBridgeError(habCode: String): HaacException = when (val code = BRIDGE_CODES[habCode]) {
        null -> BridgeException(ErrorCode.BRG_ACTION_FAILED, bridgeCode = habCode)
        ErrorCode.AUTH_SESSION_EXPIRED -> AuthException(code, bridgeCode = habCode)
        ErrorCode.ENT_NOT_FOUND -> ValidationException(code, bridgeCode = habCode)
        else -> BridgeException(code, bridgeCode = habCode)
    }

    /** HAB -> HAAC mapping table of concept 18.3; unknown HAB codes map to HAAC-BRG-005. */
    companion object {
        /** HAB codes that reach the app and the HAAC code shown for each. */
        val BRIDGE_CODES: Map<String, ErrorCode> = mapOf(
            "HAB-AUTH-001" to ErrorCode.AUTH_SESSION_EXPIRED,
            "HAB-SVC-001" to ErrorCode.BRG_NOT_ALLOWED,
            "HAB-SVC-002" to ErrorCode.BRG_ACTION_NOT_AVAILABLE,
            "HAB-SVC-003" to ErrorCode.BRG_ACTION_FAILED,
            "HAB-ENT-001" to ErrorCode.ENT_NOT_FOUND,
            "HAB-HIST-001" to ErrorCode.BRG_HISTORY_UNAVAILABLE,
            "HAB-WS-001" to ErrorCode.BRG_ACTION_FAILED,
            "HAB-INT-000" to ErrorCode.BRG_ACTION_FAILED,
        )
    }
}
