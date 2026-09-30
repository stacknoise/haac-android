package com.stacknoise.haac.feature.notifications.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stacknoise.haac.core.database.notification.NotificationType
import com.stacknoise.haac.core.database.server.ServerDao
import com.stacknoise.haac.core.database.settings.ActiveInstanceStore
import com.stacknoise.haac.core.error.ErrorCode
import com.stacknoise.haac.core.error.HaacException
import com.stacknoise.haac.feature.notifications.data.NotificationRepository
import com.stacknoise.haac.feature.notifications.domain.NotificationDay
import com.stacknoise.haac.feature.notifications.domain.NotificationItem
import com.stacknoise.haac.feature.notifications.domain.groupByDay
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.ZoneId
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** What the notification list shows; [detail] is the error entry whose detail sheet is open (concept 17.4). */
data class NotificationsUiState(
    val days: List<NotificationDay> = emptyList(),
    val detail: NotificationItem? = null,
    val instanceName: String? = null,
    val error: ErrorCode? = null,
)

/** The notification list of the active instance (concept 9.1, M-09, 17.4). */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class NotificationsViewModel @Inject constructor(
    active: ActiveInstanceStore,
    private val repository: NotificationRepository,
    servers: ServerDao,
) : ViewModel() {
    private val serverId = active.activeServerId.stateIn(viewModelScope, SharingStarted.Eagerly, null)
    private val detail = MutableStateFlow<NotificationItem?>(null)
    private val failure = MutableStateFlow<ErrorCode?>(null)

    private val days = serverId.flatMapLatest { repository.items(it) }
        .map { groupByDay(it, ZoneId.systemDefault()) }
        .catch { e ->
            if (e !is HaacException) throw e
            failure.value = e.code
            emit(emptyList())
        }

    private val instanceName = serverId.flatMapLatest { id ->
        if (id == null) flowOf(null) else servers.observe(id).map { it?.displayName }
    }

    /** The current screen state. */
    val state: StateFlow<NotificationsUiState> = combine(days, instanceName, detail, failure) { list, name, open, e ->
        NotificationsUiState(list, open, name, e)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), NotificationsUiState())

    /** A tap marks the entry read; an error entry opens its detail sheet. */
    fun onOpen(item: NotificationItem) {
        if (item.type == NotificationType.ERROR) detail.value = item
        if (item.unread) write { repository.markRead(item.id) }
    }

    /** Closes the detail sheet. */
    fun onCloseDetail() {
        detail.value = null
    }

    /** *Mark all read*. */
    fun onMarkAllRead() = write { repository.markAllRead(serverId.value) }

    /** *Dismiss* or *Keep*. */
    fun onResolve(item: NotificationItem) = write { repository.resolve(item.id) }

    /** Swipe: deletes one entry. */
    fun onDelete(item: NotificationItem) = write { repository.delete(item.id) }

    /** *Delete all*: empties the list of the active instance. */
    fun onDeleteAll() = write { repository.deleteAll(serverId.value) }

    /** *Remove tile(s)*: takes the removed entities out of every room (concept 7.4, 9.1). */
    fun onRemoveTiles(item: NotificationItem) = write { repository.removeTiles(item) }

    /** Runs a write; a failed one is shown with its code. */
    private fun write(block: suspend () -> Unit) {
        viewModelScope.launch {
            try {
                block()
            } catch (e: HaacException) {
                failure.value = e.code
            }
        }
    }

    /** Sharing timeout. */
    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
