package com.stacknoise.haac.feature.notifications.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stacknoise.haac.core.database.settings.ActiveInstanceStore
import com.stacknoise.haac.feature.notifications.data.NotificationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/** Whether the active instance has unread notifications (concept 9.1: unread dot on the bell). */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class NotificationBellViewModel @Inject constructor(
    active: ActiveInstanceStore,
    repository: NotificationRepository,
) : ViewModel() {
    /** True while at least one entry is unread. */
    val unread: StateFlow<Boolean> = active.activeServerId
        .flatMapLatest { repository.unreadCount(it) }
        .map { it > 0 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), false)

    /** Sharing timeout. */
    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
