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
        is IOException -> NetworkException(networkCode(throwable), throwable)
        else -> UnexpectedException(throwable)
    }

    /**
     * The code of an I/O failure. OkHttp tries every address of a host and keeps the earlier failures as suppressed
     * exceptions, so a rejected certificate is found there too (concept 4.3).
     */
    private fun networkCode(failure: IOException): ErrorCode = when {
        failure.hasCause<CertificatePinException>() -> ErrorCode.NET_CERTIFICATE_CHANGED
        failure.hasCause<SSLHandshakeException>() || failure.hasCause<SSLPeerUnverifiedException>() ->
            ErrorCode.NET_CERTIFICATE_UNTRUSTED
        else -> ErrorCode.NET_UNREACHABLE
    }

    /** Looks the HAB code up in [BRIDGE_CODES] and keeps it on the exception for the detail sheet. */
    override fun fromBridgeError(habCode: String): HaacException = when (val code = BRIDGE_CODES[habCode]) {
        null -> BridgeException(ErrorCode.BRG_ACTION_FAILED, bridgeCode = habCode)
        ErrorCode.AUTH_SESSION_EXPIRED, ErrorCode.AUTH_USER_BLOCKED -> AuthException(code, bridgeCode = habCode)
        ErrorCode.ENT_NOT_FOUND -> ValidationException(code, bridgeCode = habCode)
        else -> BridgeException(code, bridgeCode = habCode)
    }

    /** HAB -> HAAC mapping table of concept 18.3; unknown HAB codes map to HAAC-BRG-005. */
    companion object {
        /** HAB codes that reach the app and the HAAC code shown for each. */
        val BRIDGE_CODES: Map<String, ErrorCode> = mapOf(
            "HAB-AUTH-001" to ErrorCode.AUTH_SESSION_EXPIRED,
            "HAB-AUTH-002" to ErrorCode.AUTH_USER_BLOCKED,
            "HAB-SVC-001" to ErrorCode.BRG_NOT_ALLOWED,
            "HAB-SVC-002" to ErrorCode.BRG_ACTION_NOT_AVAILABLE,
            "HAB-SVC-003" to ErrorCode.BRG_ACTION_FAILED,
            "HAB-ENT-001" to ErrorCode.ENT_NOT_FOUND,
            "HAB-HIST-001" to ErrorCode.BRG_HISTORY_UNAVAILABLE,
            "HAB-HIST-002" to ErrorCode.BRG_HISTORY_UNAVAILABLE,
            "HAB-SCH-001" to ErrorCode.SCH_INVALID,
            "HAB-SCH-003" to ErrorCode.SCH_REMOVED,
            "HAB-SCH-004" to ErrorCode.SCH_CONFLICT,
            "HAB-SCH-005" to ErrorCode.SCH_LIMIT,
            "HAB-SCH-006" to ErrorCode.SCH_NOT_ALLOWED,
            "HAB-WS-001" to ErrorCode.BRG_ACTION_FAILED,
            "HAB-INT-000" to ErrorCode.BRG_ACTION_FAILED,
        )
    }
}

/** True if [this], one of its causes or one of their suppressed exceptions is a [T]. */
private inline fun <reified T : Throwable> Throwable.hasCause(): Boolean = anyInChain { it is T }

/** True if [test] holds for [this], a cause or a suppressed exception, searched to [MaxCauseDepth] levels. */
private fun Throwable.anyInChain(depth: Int = MaxCauseDepth, test: (Throwable) -> Boolean): Boolean =
    test(this) ||
        (depth > 0 && (listOfNotNull(cause) + suppressed).any { it !== this && it.anyInChain(depth - 1, test) })

/** Deepest cause chain that is searched, so a cyclic chain cannot loop. */
private const val MaxCauseDepth = 10
