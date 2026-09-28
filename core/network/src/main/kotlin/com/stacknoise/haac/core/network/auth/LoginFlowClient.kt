package com.stacknoise.haac.core.network.auth

import com.stacknoise.haac.core.error.AuthException
import com.stacknoise.haac.core.error.ErrorCode
import com.stacknoise.haac.core.error.UnexpectedException
import com.stacknoise.haac.core.network.http.HaHttpClient
import com.stacknoise.haac.core.network.http.HaResponse
import com.stacknoise.haac.core.network.http.endpoint
import com.stacknoise.haac.core.network.http.text
import java.net.HttpURLConnection.HTTP_BAD_REQUEST
import java.net.HttpURLConnection.HTTP_FORBIDDEN
import java.net.HttpURLConnection.HTTP_NOT_FOUND
import javax.inject.Inject
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import okhttp3.HttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody

/** One answer of HA's login flow (concept 5.1). */
sealed interface LoginFlowStep {
    /** HA asks for input in step [stepId] (`init`, `select_mfa_module`, `mfa`); [error] is `errors.base`. */
    data class Form(
        val flowId: String,
        val stepId: String,
        val error: String?,
        val mfaModules: List<String> = emptyList(),
    ) : LoginFlowStep

    /** The login succeeded; [code] is exchanged for tokens once and must not be logged. */
    class Done(val code: String) : LoginFlowStep {
        /** Hides the authorization code. */
        override fun toString() = "Done(***)"
    }
}

/**
 * Drives HA's login flow API natively (`POST /auth/login_flow`, concept 5.1, 11.1).
 *
 * Aborted or unknown flows become HAAC-AUTH-005, HTTP 403 (blocked user or IP) HAAC-AUTH-006.
 */
class LoginFlowClient @Inject constructor(
    private val http: HaHttpClient,
    private val json: Json,
) {
    /** Starts a flow with the username/password provider; HA answers with the `init` form. */
    suspend fun start(baseUrl: HttpUrl): LoginFlowStep {
        val body = buildJsonObject {
            put("client_id", AuthClientConfig.CLIENT_ID)
            putJsonArray("handler") {
                add(AuthProvidersClient.PASSWORD_PROVIDER)
                add(JsonNull)
            }
            put("redirect_uri", AuthClientConfig.REDIRECT_URI)
        }
        return send(baseUrl.endpoint(FLOW_PATH), body.toString().encodeToByteArray())
    }

    /** Sends username and password; [password] is not changed, the request body is wiped afterwards. */
    suspend fun submitCredentials(
        baseUrl: HttpUrl,
        flowId: String,
        username: String,
        password: CharArray,
    ): LoginFlowStep {
        val body = SecretJson.credentials(AuthClientConfig.CLIENT_ID, username, password)
        try {
            return send(flowUrl(baseUrl, flowId), body)
        } finally {
            body.fill(0)
        }
    }

    /** Chooses the MFA module when the user has several (`select_mfa_module`). */
    suspend fun selectMfaModule(baseUrl: HttpUrl, flowId: String, module: String): LoginFlowStep =
        submit(baseUrl, flowId, "multi_factor_auth_module", module)

    /** Sends the code of the `mfa` step. */
    suspend fun submitMfaCode(baseUrl: HttpUrl, flowId: String, code: String): LoginFlowStep =
        submit(baseUrl, flowId, "code", code)

    /** Sends one field of a flow step together with the client_id. */
    private suspend fun submit(baseUrl: HttpUrl, flowId: String, field: String, value: String): LoginFlowStep {
        val body = buildJsonObject {
            put("client_id", AuthClientConfig.CLIENT_ID)
            put(field, value)
        }
        return send(flowUrl(baseUrl, flowId), body.toString().encodeToByteArray())
    }

    /** `auth/login_flow/<flowId>` with the id encoded as one path segment. */
    private fun flowUrl(baseUrl: HttpUrl, flowId: String): HttpUrl =
        baseUrl.endpoint(FLOW_PATH).newBuilder().addPathSegment(flowId).build()

    /** Posts a JSON body and interprets the answer. */
    private suspend fun send(url: HttpUrl, body: ByteArray): LoginFlowStep =
        interpret(http.post(url, body.toRequestBody(JSON_TYPE)))

    /** Maps status codes and flow result types to a step or an error. */
    private fun interpret(response: HaResponse): LoginFlowStep = when {
        response.code == HTTP_FORBIDDEN -> throw AuthException(ErrorCode.AUTH_USER_BLOCKED)
        response.code == HTTP_NOT_FOUND || response.code == HTTP_BAD_REQUEST ->
            throw AuthException(ErrorCode.AUTH_SIGN_IN_ABORTED)
        !response.isSuccessful -> throw UnexpectedException()
        else -> parse(response.body)
    }

    /** Reads `type`, `flow_id`, `step_id`, `errors.base` and the MFA module options of a flow result. */
    private fun parse(body: String): LoginFlowStep = try {
        val root = json.parseToJsonElement(body).jsonObject
        when (root.text("type")) {
            "create_entry" -> LoginFlowStep.Done(root.text("result") ?: throw UnexpectedException())
            "form" -> LoginFlowStep.Form(
                flowId = root.text("flow_id") ?: throw UnexpectedException(),
                stepId = root.text("step_id").orEmpty(),
                error = (root["errors"] as? JsonObject)?.text("base"),
                mfaModules = mfaModules(root["data_schema"]),
            )
            "abort" -> throw AuthException(ErrorCode.AUTH_SIGN_IN_ABORTED)
            else -> throw UnexpectedException()
        }
    } catch (e: SerializationException) {
        throw UnexpectedException(e)
    } catch (e: IllegalArgumentException) {
        throw UnexpectedException(e)
    }

    /** Option keys of the `multi_factor_auth_module` field: pairs `[id, name]`, plain ids or an id → name map. */
    private fun mfaModules(schema: JsonElement?): List<String> {
        val field = (schema as? JsonArray)
            ?.map { it.jsonObject }
            ?.firstOrNull { it.text("name") == MFA_MODULE_FIELD }
            ?: return emptyList()
        return when (val options = field["options"]) {
            is JsonObject -> options.keys.toList()
            is JsonArray -> options.mapNotNull { option ->
                when (option) {
                    is JsonArray -> option.firstOrNull()?.jsonPrimitive?.contentOrNull
                    is JsonPrimitive -> option.contentOrNull
                    else -> null
                }
            }
            else -> emptyList()
        }
    }

    /** Paths and field names of the HA login flow API. */
    private companion object {
        const val FLOW_PATH = "auth/login_flow"
        const val MFA_MODULE_FIELD = "multi_factor_auth_module"
        val JSON_TYPE = "application/json".toMediaType()
    }
}
