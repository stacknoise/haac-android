package com.stacknoise.haac.core.network.bridge

import com.stacknoise.haac.core.error.BridgeException
import com.stacknoise.haac.core.error.ErrorCode
import com.stacknoise.haac.core.error.ErrorFactory
import com.stacknoise.haac.core.error.HaacException
import com.stacknoise.haac.core.network.http.text
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull

/** HA's error code for a command nobody registered, i.e. HAAC Bridge is not installed. */
private const val UnknownCommand = "unknown_command"

/** The `result` of a reply (JSON null if there is none), or the bridge error it carries (concept 11.3). */
fun JsonObject.resultOrThrow(errors: ErrorFactory): JsonElement {
    if ((this["success"] as? JsonPrimitive)?.booleanOrNull != true) throw bridgeError(this, errors)
    return this["result"] ?: JsonNull
}

/** `unknown_command` means the bridge is not installed (BRG-001); HAB codes map via [ErrorFactory] (18.3). */
private fun bridgeError(reply: JsonObject, errors: ErrorFactory): HaacException {
    val code = (reply["error"] as? JsonObject)?.text("code")
    return when {
        code == UnknownCommand -> BridgeException(ErrorCode.BRG_NOT_INSTALLED, bridgeCode = code)
        code != null && code.startsWith("HAB-") -> errors.fromBridgeError(code)
        else -> BridgeException(ErrorCode.BRG_ACTION_FAILED, bridgeCode = code)
    }
}
