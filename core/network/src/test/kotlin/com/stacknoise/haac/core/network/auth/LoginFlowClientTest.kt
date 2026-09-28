package com.stacknoise.haac.core.network.auth

import com.stacknoise.haac.core.error.AuthException
import com.stacknoise.haac.core.error.DefaultErrorFactory
import com.stacknoise.haac.core.error.ErrorCode
import com.stacknoise.haac.core.network.http.HaHttpClient
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.OkHttpClient
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class LoginFlowClientTest {
    private val server = MockWebServer()
    private val json = Json { ignoreUnknownKeys = true }
    private val flows = LoginFlowClient(HaHttpClient(OkHttpClient(), DefaultErrorFactory()), json)

    @BeforeEach
    fun start() = server.start()

    @AfterEach
    fun stop() = server.close()

    private fun reply(body: String, code: Int = 200) =
        server.enqueue(MockResponse.Builder().code(code).body(body).build())

    private fun form(step: String, error: String? = null, schema: String = "[]") =
        """{"type":"form","flow_id":"f1","handler":["homeassistant",null],"step_id":"$step",""" +
            """"data_schema":$schema,"errors":${if (error == null) "{}" else """{"base":"$error"}"""}}"""

    @Test
    fun `start asks for the password provider with the app's client id`() = runTest {
        reply(form("init"))
        val step = flows.start(server.url("/"))
        assertEquals(LoginFlowStep.Form("f1", "init", null), step)
        val request = server.takeRequest()
        assertEquals("/auth/login_flow", request.url.encodedPath)
        val body = json.parseToJsonElement(request.body!!.utf8()).jsonObject
        assertEquals(AuthClientConfig.CLIENT_ID, body["client_id"]!!.jsonPrimitive.content)
        assertEquals(AuthClientConfig.REDIRECT_URI, body["redirect_uri"]!!.jsonPrimitive.content)
        assertEquals("""["homeassistant",null]""", body["handler"].toString())
    }

    @Test
    fun `credentials are sent as JSON and escaped`() = runTest {
        reply("""{"type":"create_entry","flow_id":"f1","result":"code-123"}""")
        val password = "p\"w\\ö".toCharArray()
        val step = flows.submitCredentials(server.url("/"), "f1", "anna", password)
        assertEquals("code-123", assertInstanceOf(LoginFlowStep.Done::class.java, step).code)
        assertEquals("p\"w\\ö", String(password))
        val request = server.takeRequest()
        assertEquals("/auth/login_flow/f1", request.url.encodedPath)
        val body = json.parseToJsonElement(request.body!!.utf8()).jsonObject
        assertEquals("anna", body["username"]!!.jsonPrimitive.content)
        assertEquals("p\"w\\ö", body["password"]!!.jsonPrimitive.content)
    }

    @Test
    fun `wrong password and MFA step are forms`() = runTest {
        reply(form("init", error = "invalid_auth"))
        assertEquals(
            LoginFlowStep.Form("f1", "init", "invalid_auth"),
            flows.submitCredentials(server.url("/"), "f1", "anna", "x".toCharArray()),
        )
        reply(form("mfa", schema = """[{"name":"code","type":"string"}]"""))
        assertEquals(LoginFlowStep.Form("f1", "mfa", null), flows.submitMfaCode(server.url("/"), "f1", "123456"))
    }

    @Test
    fun `MFA module options are read`() = runTest {
        val schema = """[{"name":"multi_factor_auth_module","type":"select","options":[["totp","Authenticator"],""" +
            """["notify","Notify"]]}]"""
        reply(form("select_mfa_module", schema = schema))
        val step = flows.start(server.url("/")) as LoginFlowStep.Form
        assertEquals(listOf("totp", "notify"), step.mfaModules)
    }

    @Test
    fun `abort, unknown flow and blocked user are errors`() = runTest {
        reply("""{"type":"abort","flow_id":"f1","reason":"too_many_retry"}""")
        assertEquals(ErrorCode.AUTH_SIGN_IN_ABORTED, error { flows.submitMfaCode(server.url("/"), "f1", "1") })
        reply("""{"message":"Invalid flow specified"}""", code = 404)
        assertEquals(ErrorCode.AUTH_SIGN_IN_ABORTED, error { flows.submitMfaCode(server.url("/"), "f1", "1") })
        reply("""{"message":"Login blocked: User is not active"}""", code = 403)
        assertEquals(ErrorCode.AUTH_USER_BLOCKED, error { flows.start(server.url("/")) })
    }

    private suspend fun error(block: suspend () -> Unit): ErrorCode = assertThrows<AuthException> { block() }.code
}
