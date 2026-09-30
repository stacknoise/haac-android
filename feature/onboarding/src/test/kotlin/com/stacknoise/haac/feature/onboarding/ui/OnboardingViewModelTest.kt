package com.stacknoise.haac.feature.onboarding.ui

import androidx.lifecycle.SavedStateHandle
import com.stacknoise.haac.core.error.AuthException
import com.stacknoise.haac.core.error.BridgeException
import com.stacknoise.haac.core.error.ErrorCode
import com.stacknoise.haac.core.error.NetworkException
import com.stacknoise.haac.core.network.auth.AuthTokens
import com.stacknoise.haac.core.network.discovery.DiscoveredServer
import com.stacknoise.haac.core.network.discovery.ServerDiscovery
import com.stacknoise.haac.core.network.endpoint.AddressSlot
import com.stacknoise.haac.core.network.tls.CertificateProbe
import com.stacknoise.haac.core.network.tls.PeerCertificate
import com.stacknoise.haac.core.network.tls.PinRegistry
import com.stacknoise.haac.feature.onboarding.domain.KnownServer
import com.stacknoise.haac.feature.onboarding.domain.ServerValidator
import com.stacknoise.haac.feature.onboarding.domain.SignInRepository
import com.stacknoise.haac.feature.onboarding.domain.SignInResult
import com.stacknoise.haac.feature.onboarding.domain.SignInStep
import com.stacknoise.haac.feature.onboarding.domain.SignInTarget
import com.stacknoise.haac.feature.onboarding.domain.ValidatedServer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import okhttp3.HttpUrl
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class OnboardingViewModelTest {
    private val found = MutableSharedFlow<List<DiscoveredServer>>(replay = 1)
    private val validated = mutableListOf<HttpUrl>()
    private var failValidation: ErrorCode? = null
    private val tokens = AuthTokens("acc", "ref", 1800)

    private var scans = 0
    private val dispatcher = UnconfinedTestDispatcher()
    private val discovery = object : ServerDiscovery {
        override fun servers(): Flow<List<DiscoveredServer>> {
            scans++
            return found
        }
    }
    private val validator = object : ServerValidator {
        override suspend fun validate(url: HttpUrl): ValidatedServer {
            failValidation?.let { throw NetworkException(it) }
            validated += url
            return ValidatedServer(url)
        }
    }

    /** Scripted HA: credentials, code and bridge check answer as configured. */
    private val repository = object : SignInRepository {
        var needsCode = false
        var bridgeMissing = false
        val passwords = mutableListOf<CharArray>()
        val finished = mutableListOf<SignInTarget>()
        var discarded = 0
        var sameInstance: SignInResult.SameInstance? = null
        val added = mutableListOf<Pair<String, HttpUrl>>()

        override suspend fun knownServer(serverId: String) =
            KnownServer(serverId, "https://ha.example.com/", "Home", "anna").takeIf { serverId == "s1" }

        override suspend fun signIn(url: HttpUrl, username: String, password: CharArray): SignInStep {
            passwords += password
            if (String(password) != "secret") throw AuthException(ErrorCode.AUTH_INVALID_CREDENTIALS)
            return if (needsCode) SignInStep.CodeRequired("flow-1") else SignInStep.Authorized(tokens)
        }

        override suspend fun submitCode(url: HttpUrl, flowId: String, code: String): SignInStep {
            if (code != "123456") throw AuthException(ErrorCode.AUTH_INVALID_MFA_CODE)
            return SignInStep.Authorized(tokens)
        }

        override suspend fun finish(target: SignInTarget, username: String, tokens: AuthTokens): SignInResult {
            if (bridgeMissing) throw BridgeException(ErrorCode.BRG_NOT_INSTALLED)
            finished += target
            return sameInstance ?: SignInResult.Saved(target.serverId ?: "new-id")
        }

        override suspend fun addAddress(serverId: String, url: HttpUrl, tokens: AuthTokens) {
            added += serverId to url
        }

        override suspend fun discard(url: HttpUrl, tokens: AuthTokens): Boolean {
            discarded++
            return true
        }
    }

    private val certificate = PeerCertificate("ab".repeat(32), "AA:BB", "CN=ha", 0L)
    private val probe = CertificateProbe { certificate }
    private val pins = PinRegistry()

    private val home = DiscoveredServer("1", "Home", "192.168.1.10:8123", "http://192.168.1.10:8123", "2026.9.0")

    @BeforeEach
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterEach
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel(serverId: String? = null) = OnboardingViewModel(
        discovery,
        validator,
        repository,
        probe,
        pins,
        SavedStateHandle(if (serverId == null) emptyMap() else mapOf(OnboardingViewModel.SERVER_ID_ARG to serverId)),
    )

    /** A view model with a manually entered https address and username anna. */
    private fun manual(): OnboardingViewModel = viewModel().apply {
        onOtherAddress()
        onManualUrlChanged("ha.example.com/lovelace")
        onUsernameChanged("anna")
    }

    @Test
    fun `first discovered server is preselected`() {
        val viewModel = viewModel()
        found.tryEmit(listOf(home))
        assertEquals(listOf(home), viewModel.state.value.servers)
        assertEquals(home.url, viewModel.state.value.selectedUrl)
    }

    @Test
    fun `http to a private server needs confirmation first`() {
        val viewModel = viewModel()
        found.tryEmit(listOf(home))
        viewModel.onUsernameChanged("anna")

        viewModel.onSignIn("secret".toCharArray())
        assertEquals("http://192.168.1.10:8123/", viewModel.state.value.cleartextWarningFor)
        assertEquals(emptyList<HttpUrl>(), validated)

        viewModel.onCleartextConfirmed()
        viewModel.onSignIn("secret".toCharArray())
        assertNull(viewModel.state.value.cleartextWarningFor)
        assertEquals("new-id", viewModel.state.value.signedInServerId)
        assertEquals(SignInTarget(validated.single(), "Home"), repository.finished.single())
    }

    @Test
    fun `sign in with MFA stores the instance and wipes the password`() {
        repository.needsCode = true
        val viewModel = manual()
        viewModel.onSignIn("secret".toCharArray())
        assertEquals(SignInStage.CODE, viewModel.state.value.stage)

        viewModel.onCodeChanged("000000")
        viewModel.onSubmitCode()
        assertEquals(ErrorCode.AUTH_INVALID_MFA_CODE, viewModel.state.value.error)
        assertEquals(SignInStage.CODE, viewModel.state.value.stage)

        viewModel.onCodeChanged("123 456")
        viewModel.onSubmitCode()
        assertEquals("new-id", viewModel.state.value.signedInServerId)
        assertEquals("ha.example.com", repository.finished.single().displayName)
        assertTrue(repository.passwords.single().all { it == '\u0000' })
    }

    @Test
    fun `errors are shown with their code`() {
        val viewModel = manual()
        viewModel.onSignIn("wrong".toCharArray())
        assertEquals(ErrorCode.AUTH_INVALID_CREDENTIALS, viewModel.state.value.error)
        assertFalse(viewModel.state.value.busy)

        viewModel.onManualUrlChanged("http://ha.example.com")
        viewModel.onSignIn("secret".toCharArray())
        assertEquals(ErrorCode.NET_CLEARTEXT_NOT_ALLOWED, viewModel.state.value.error)

        failValidation = ErrorCode.NET_NOT_HOME_ASSISTANT
        viewModel.onManualUrlChanged("https://router.example.com")
        viewModel.onSignIn("secret".toCharArray())
        assertEquals(ErrorCode.NET_NOT_HOME_ASSISTANT, viewModel.state.value.error)
    }

    @Test
    fun `missing bridge keeps the tokens for another check`() {
        repository.bridgeMissing = true
        val viewModel = manual()
        viewModel.onSignIn("secret".toCharArray())
        assertEquals(SignInStage.BRIDGE, viewModel.state.value.stage)
        assertEquals(ErrorCode.BRG_NOT_INSTALLED, viewModel.state.value.error)

        repository.bridgeMissing = false
        viewModel.onRetryBridgeCheck()
        assertEquals("new-id", viewModel.state.value.signedInServerId)
        assertEquals(0, repository.discarded)
    }

    @Test
    fun `starting over revokes unused tokens`() {
        repository.bridgeMissing = true
        val viewModel = manual()
        viewModel.onSignIn("secret".toCharArray())
        viewModel.onStartOver()
        assertEquals(SignInStage.CREDENTIALS, viewModel.state.value.stage)
        assertEquals(1, repository.discarded)
    }

    @Test
    fun `a known server and user becomes an address of the stored instance`() {
        repository.sameInstance = SignInResult.SameInstance("s1", "Home", AddressSlot.EXTERNAL, null)
        val viewModel = manual()
        viewModel.onSignIn("secret".toCharArray())
        assertEquals(repository.sameInstance, viewModel.state.value.addressOffer)
        assertNull(viewModel.state.value.signedInServerId)

        viewModel.onAddAddress()
        assertEquals(listOf("s1" to validated.single()), repository.added)
        assertEquals("s1", viewModel.state.value.signedInServerId)
        assertNull(viewModel.state.value.addressOffer)
        assertEquals(0, repository.discarded)
    }

    @Test
    fun `declining the address offer revokes the new tokens`() {
        repository.sameInstance =
            SignInResult.SameInstance("s1", "Home", AddressSlot.EXTERNAL, "https://old.example.com/")
        val viewModel = manual()
        viewModel.onSignIn("secret".toCharArray())
        viewModel.onAddressOfferDismissed()
        assertNull(viewModel.state.value.addressOffer)
        assertEquals(SignInStage.CREDENTIALS, viewModel.state.value.stage)
        assertEquals(1, repository.discarded)
        assertTrue(repository.added.isEmpty())
    }

    @Test
    fun `stored instance signs in again without discovery`() {
        val viewModel = viewModel(serverId = "s1")
        found.tryEmit(listOf(home))
        assertEquals(emptyList<DiscoveredServer>(), viewModel.state.value.servers)
        assertEquals("anna", viewModel.state.value.username)

        viewModel.onSignIn("secret".toCharArray())
        assertEquals(SignInTarget(validated.single(), "Home", "s1"), repository.finished.single())
        assertEquals("s1", viewModel.state.value.signedInServerId)
    }

    @Test
    fun `an untrusted https certificate is offered and trusting pins its key`() {
        failValidation = ErrorCode.NET_CERTIFICATE_UNTRUSTED
        val viewModel = manual()
        viewModel.onSignIn("secret".toCharArray())
        val offer = viewModel.state.value.certificateOffer
        assertEquals(certificate, offer?.certificate)
        assertNull(viewModel.state.value.error)
        assertFalse(viewModel.state.value.busy)

        viewModel.onCertificateTrusted()
        assertNull(viewModel.state.value.certificateOffer)
        assertEquals(certificate.keyHash, pins.pinFor(offer!!.url))
    }

    @Test
    fun `declining the certificate keeps HAAC-NET-007`() {
        failValidation = ErrorCode.NET_CERTIFICATE_UNTRUSTED
        val viewModel = manual()
        viewModel.onSignIn("secret".toCharArray())
        viewModel.onCertificateDismissed()
        assertNull(viewModel.state.value.certificateOffer)
        assertEquals(ErrorCode.NET_CERTIFICATE_UNTRUSTED, viewModel.state.value.error)
        assertNull(pins.pinFor("ha.example.com", 443))
    }

    @Test
    fun `the scan indicator ends after the scan window and Scan again searches once more`() {
        val viewModel = viewModel()
        assertTrue(viewModel.state.value.scanning)
        viewModel.onRescan()
        assertEquals(1, scans)

        dispatcher.scheduler.advanceTimeBy(OnboardingViewModel.SCAN_WINDOW_MS + 1)
        assertFalse(viewModel.state.value.scanning)

        found.tryEmit(listOf(home))
        viewModel.onRescan()
        assertEquals(2, scans)
        assertTrue(viewModel.state.value.scanning)
        // The fake replays its last list to every new collector, like a fresh discovery finding the server again.
        assertEquals(listOf(home), viewModel.state.value.servers)
    }
}
