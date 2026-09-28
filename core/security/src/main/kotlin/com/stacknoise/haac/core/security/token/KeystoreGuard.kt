package com.stacknoise.haac.core.security.token

import com.stacknoise.haac.core.error.ErrorCode
import com.stacknoise.haac.core.error.ErrorFactory
import com.stacknoise.haac.core.error.KeystoreException
import java.io.IOException
import java.security.GeneralSecurityException
import java.security.ProviderException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Runs [block] on the IO dispatcher and converts file and Keystore errors to HAAC-SEC codes (concept 17.3). */
internal suspend fun <T> keystoreGuarded(errors: ErrorFactory, block: suspend () -> T): T =
    withContext(Dispatchers.IO) {
        try {
            block()
        } catch (e: IOException) {
            throw KeystoreException(ErrorCode.SEC_STORAGE_UNAVAILABLE, e)
        } catch (e: GeneralSecurityException) {
            throw errors.from(e)
        } catch (e: ProviderException) {
            throw errors.from(e)
        }
    }
