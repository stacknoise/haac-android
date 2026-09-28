package com.stacknoise.haac.app.start

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Computes the start route once per app start; null while it is being decided. */
@HiltViewModel
class StartViewModel @Inject constructor(router: StartRouter) : ViewModel() {
    private val _route = MutableStateFlow<StartRoute?>(null)

    /** The start route, or null while loading. */
    val route: StateFlow<StartRoute?> = _route.asStateFlow()

    init {
        viewModelScope.launch { _route.value = router.route() }
    }
}
