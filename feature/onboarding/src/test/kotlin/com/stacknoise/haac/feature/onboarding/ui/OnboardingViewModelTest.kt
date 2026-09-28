package com.stacknoise.haac.feature.onboarding.ui

import com.stacknoise.haac.core.error.ErrorCode
import com.stacknoise.haac.core.error.NetworkException
import com.stacknoise.haac.feature.onboarding.domain.DiscoveredServer
import com.stacknoise.haac.feature.onboarding.domain.ServerDiscovery
import com.stacknoise.haac.feature.onboarding.domain.ServerValidator
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
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class OnboardingViewModelTest {
    private val found = MutableSharedFlow<List<DiscoveredServer>>(replay = 1)
    private val validated = mutableListOf<HttpUrl>()
    private var failWith: ErrorCode? = null

    private val discovery = object : ServerDiscovery {
        override fun servers(): Flow<List<DiscoveredServer>> = found
    }
    private val validator = object : ServerValidator {
        override suspend fun validate(url: HttpUrl): ValidatedServer {
            failWith?.let { throw NetworkException(it) }
            validated += url
            return ValidatedServer(url)
        }
    }

    private val home = DiscoveredServer("1", "Home", "192.168.1.10:8123", "http://192.168.1.10:8123", "2026.9.0")

    @BeforeEach
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterEach
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `first discovered server is preselected`() {
        val viewModel = OnboardingViewModel(discovery, validator)
        found.tryEmit(listOf(home))
        assertEquals(listOf(home), viewModel.state.value.servers)
        assertEquals(home.url, viewModel.state.value.selectedUrl)
    }

    @Test
    fun `http to a private server needs confirmation first`() {
        val viewModel = OnboardingViewModel(discovery, validator)
        found.tryEmit(listOf(home))

        viewModel.onContinue()
        assertEquals("http://192.168.1.10:8123/", viewModel.state.value.cleartextWarningFor)
        assertEquals(emptyList<HttpUrl>(), validated)

        viewModel.onCleartextConfirmed()
        assertNull(viewModel.state.value.cleartextWarningFor)
        assertEquals("http://192.168.1.10:8123/", viewModel.state.value.validatedUrl)

        viewModel.onContinue()
        assertEquals(2, validated.size)
        assertNull(viewModel.state.value.cleartextWarningFor)
    }

    @Test
    fun `manual https address is validated directly`() {
        val viewModel = OnboardingViewModel(discovery, validator)
        viewModel.onOtherAddress()
        viewModel.onManualUrlChanged("ha.example.com/lovelace")
        viewModel.onContinue()
        assertEquals("https://ha.example.com/", viewModel.state.value.validatedUrl)
        assertFalse(viewModel.state.value.checking)
    }

    @Test
    fun `errors are shown with their code`() {
        val viewModel = OnboardingViewModel(discovery, validator)
        viewModel.onOtherAddress()
        viewModel.onManualUrlChanged("http://ha.example.com")
        viewModel.onContinue()
        assertEquals(ErrorCode.NET_CLEARTEXT_NOT_ALLOWED, viewModel.state.value.error)

        failWith = ErrorCode.NET_NOT_HOME_ASSISTANT
        viewModel.onManualUrlChanged("https://router.example.com")
        viewModel.onContinue()
        assertEquals(ErrorCode.NET_NOT_HOME_ASSISTANT, viewModel.state.value.error)
    }
}
