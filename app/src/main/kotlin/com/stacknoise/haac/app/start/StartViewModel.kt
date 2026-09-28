package com.stacknoise.haac.app.start

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stacknoise.haac.app.lock.AppLock
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** A start route and the app lock it belongs to; [lock] 0 is the app start, higher values follow a lock. */
data class StartDecision(val route: StartRoute, val lock: Int)

/** Computes the start route at app start and again after every app lock (concept 4.1, 5.5). */
@HiltViewModel
class StartViewModel @Inject constructor(router: StartRouter, appLock: AppLock) : ViewModel() {
    private val _decision = MutableStateFlow<StartDecision?>(null)

    /** The current decision, or null while the first one is being made. */
    val decision: StateFlow<StartDecision?> = _decision.asStateFlow()

    init {
        viewModelScope.launch {
            appLock.locks.collect { lock -> _decision.value = StartDecision(router.route(), lock) }
        }
    }
}
