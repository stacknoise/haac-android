package com.stacknoise.haac.feature.schedules.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stacknoise.haac.core.database.settings.ActiveInstanceStore
import com.stacknoise.haac.core.error.ErrorCode
import com.stacknoise.haac.core.network.connection.LiveConnection
import com.stacknoise.haac.core.network.connection.connected
import com.stacknoise.haac.core.network.connection.stale
import com.stacknoise.haac.feature.schedules.data.Candidates
import com.stacknoise.haac.feature.schedules.data.ScheduleCandidates
import com.stacknoise.haac.feature.schedules.data.ScheduleCommands
import com.stacknoise.haac.feature.schedules.data.ScheduleFailures
import com.stacknoise.haac.feature.schedules.data.ScheduleRepository
import com.stacknoise.haac.feature.schedules.domain.ScheduleAction
import com.stacknoise.haac.feature.schedules.domain.ScheduleDraft
import com.stacknoise.haac.feature.schedules.domain.ScheduleItem
import com.stacknoise.haac.feature.schedules.domain.WhenType
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalTime
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** What the editor changes: the [draft], the schedule it started from ([original], null for a new one) and the save. */
data class EditorForm(
    val draft: ScheduleDraft = ScheduleDraft(),
    val original: ScheduleItem? = null,
    val loaded: Boolean = false,
    val saving: Boolean = false,
    val saved: Boolean = false,
    val error: ErrorCode? = null,
)

/**
 * What the editor shows (M-12, M-18). Saving needs an open connection ([editable]) and a valid draft. [ownEntities]
 * is false for a schedule of another user, whose entities only the owner changes (concept 19.4).
 */
data class ScheduleEditorUiState(
    val form: EditorForm = EditorForm(),
    val candidates: Candidates = Candidates(),
    val connected: Boolean = false,
    val stale: Boolean = false,
) {
    /** True if the schedule can be saved right now. */
    val editable: Boolean get() = connected && !stale

    /** True while a new schedule or one of the user's own is edited, so the entities can be changed. */
    val ownEntities: Boolean get() = form.original?.own ?: true

    /** The name of another user whose schedule is edited, or null. */
    val owner: String? get() = form.original?.takeIf { !it.own }?.let { it.ownerName.orEmpty() }

    /** True if *Save* is enabled. */
    val canSave: Boolean get() = editable && form.loaded && !form.saving && form.draft.isValid(ownEntities)
}

/** The editor of a schedule (M-12 to M-14, M-18, concept 19.7): a new one, or the one named by the route argument. */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
@Suppress("TooManyFunctions") // one small function per editable field
class ScheduleEditorViewModel @Inject constructor(
    savedState: SavedStateHandle,
    active: ActiveInstanceStore,
    private val repository: ScheduleRepository,
    candidates: ScheduleCandidates,
    live: LiveConnection,
    private val commands: ScheduleCommands,
    private val failures: ScheduleFailures,
) : ViewModel() {
    private val scheduleId: String? = savedState[ID_ARG]
    private val serverId = active.activeServerId.stateIn(viewModelScope, SharingStarted.Eagerly, null)
    private val form = MutableStateFlow(EditorForm(loaded = scheduleId == null))

    private val choices: Flow<Candidates> = active.activeServerId.flatMapLatest { id ->
        if (id == null) flowOf(Candidates()) else candidates.of(id)
    }

    private val link: Flow<Pair<Boolean, Boolean>> = combine(live.connected, live.stale) { c, s -> c to s }

    /** The state of the screen. */
    val state: StateFlow<ScheduleEditorUiState> = combine(form, choices, link) { form, choices, link ->
        ScheduleEditorUiState(form, choices, link.first, link.second)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), ScheduleEditorUiState())

    init {
        if (scheduleId != null) viewModelScope.launch { load(scheduleId) }
    }

    /** Reads the cached schedule into the editor, once. */
    private suspend fun load(id: String) {
        val server = serverId.filterNotNull().first()
        val item = repository.view(server, id).first()?.item
        form.update { it.copy(draft = item?.let(ScheduleDraft::of) ?: it.draft, original = item, loaded = true) }
    }

    /** The name field. */
    fun onName(name: String) = change { it.copy(name = name.take(ScheduleDraft.MAX_NAME)) }

    /** *Run at*: a fixed time, sunrise or sunset. */
    fun onType(type: WhenType) = change { it.copy(whenType = type) }

    /** The time of a fixed-time schedule. */
    fun onTime(time: LocalTime) = change { it.copy(time = time) }

    /** The offset of a sun event in minutes. */
    fun onOffset(minutes: Int) = change { it.copy(offsetMin = minutes) }

    /** Selects or deselects [day] (0 = Monday). */
    fun onDay(day: Int) = change { it.copy(days = if (day in it.days) it.days - day else it.days + day) }

    /** One of the presets *Weekdays*, *Weekend*, *Every day*. */
    fun onDays(days: Set<Int>) = change { it.copy(days = days) }

    /** *Turn on*, *Turn off* or *Toggle*. */
    fun onAction(action: ScheduleAction) = change { it.copy(action = action) }

    /** The entities chosen in the picker, in the order they were picked. */
    fun onEntities(ids: List<String>) = change { it.copy(entityIds = ids) }

    /** Removes [entityId] from the schedule. */
    fun onRemoveEntity(entityId: String) = change { it.copy(entityIds = it.entityIds - entityId) }

    /** *Save*: sends a new schedule or the changes; the screen closes when [EditorForm.saved]. */
    fun onSave() {
        val current = form.value
        val server = serverId.value?.takeIf { state.value.canSave } ?: return
        form.update { it.copy(saving = true, error = null) }
        viewModelScope.launch {
            val code = failures.run(server) { commands.save(server, current.original, current.draft) }
            form.update { it.copy(saving = false, saved = code == null, error = code) }
        }
    }

    /**
     * After *HAAC-SCH-004*: reads the schedule again and keeps what the user changed, everything else follows the
     * server's version (concept 19.7). A schedule that is gone ends the edit.
     */
    fun onReload() {
        val server = serverId.value
        val id = scheduleId
        if (server == null || id == null) return
        viewModelScope.launch {
            val fresh = repository.view(server, id).first()?.item
            form.update { current ->
                val base = current.original?.let(ScheduleDraft::of)
                if (fresh == null || base == null) {
                    current.copy(error = ErrorCode.SCH_REMOVED)
                } else {
                    val merged = current.draft.rebase(base, ScheduleDraft.of(fresh))
                    current.copy(draft = merged, original = fresh, error = null)
                }
            }
        }
    }

    /** The error message was shown. */
    fun onErrorShown() = form.update { it.copy(error = null) }

    /** Applies [edit] to the draft. */
    private fun change(edit: (ScheduleDraft) -> ScheduleDraft) = form.update { it.copy(draft = edit(it.draft)) }

    /** Argument and timing. */
    companion object {
        /** Name of the optional navigation argument that holds the schedule id. */
        const val ID_ARG = "scheduleId"
        private const val STOP_TIMEOUT_MS = 5_000L
    }
}
