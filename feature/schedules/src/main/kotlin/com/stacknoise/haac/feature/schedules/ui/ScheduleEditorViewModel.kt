package com.stacknoise.haac.feature.schedules.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stacknoise.haac.core.database.settings.ActiveInstanceStore
import com.stacknoise.haac.core.error.ErrorCode
import com.stacknoise.haac.core.error.HaacException
import com.stacknoise.haac.core.network.connection.LiveConnection
import com.stacknoise.haac.core.network.connection.connected
import com.stacknoise.haac.core.network.connection.stale
import com.stacknoise.haac.feature.schedules.data.Candidates
import com.stacknoise.haac.feature.schedules.data.ScheduleCandidates
import com.stacknoise.haac.feature.schedules.data.ScheduleCommands
import com.stacknoise.haac.feature.schedules.data.ScheduleFailures
import com.stacknoise.haac.feature.schedules.data.ScheduleFeatures
import com.stacknoise.haac.feature.schedules.data.SchedulePreviews
import com.stacknoise.haac.feature.schedules.data.ScheduleRepository
import com.stacknoise.haac.feature.schedules.domain.RepeatFreq
import com.stacknoise.haac.feature.schedules.domain.ScheduleAction
import com.stacknoise.haac.feature.schedules.domain.ScheduleDraft
import com.stacknoise.haac.feature.schedules.domain.ScheduleItem
import com.stacknoise.haac.feature.schedules.domain.WhenType
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * What the editor changes: the [draft], the schedule it started from ([original], null for a new one) and the save.
 * [preview] holds the next runs the bridge computed for the draft (concept 19.10); it is empty while there is none.
 */
data class EditorForm(
    val draft: ScheduleDraft = ScheduleDraft(),
    val original: ScheduleItem? = null,
    val loaded: Boolean = false,
    val saving: Boolean = false,
    val saved: Boolean = false,
    val error: ErrorCode? = null,
    val preview: List<Long> = emptyList(),
)

/**
 * What the editor shows (M-12, M-18, M-23, M-24). Saving needs an open connection ([editable]) and a valid draft.
 * [ownEntities] is false for a schedule of another user, whose entities only the owner changes (concept 19.4).
 * [features] decide whether the date rules and the notification switch are offered (19.10, 19.11).
 */
data class ScheduleEditorUiState(
    val form: EditorForm = EditorForm(),
    val candidates: Candidates = Candidates(),
    val connected: Boolean = false,
    val stale: Boolean = false,
    val features: ScheduleFeatures = ScheduleFeatures(),
) {
    /** True if the schedule can be saved right now. */
    val editable: Boolean get() = connected && !stale

    /** True while a new schedule or one of the user's own is edited, so the entities can be changed. */
    val ownEntities: Boolean get() = form.original?.own ?: true

    /** True if the notification switch is shown: the bridge has run notifications and only the owner changes it. */
    val showNotify: Boolean get() = features.runs && ownEntities

    /** The name of another user whose schedule is edited, or null. */
    val owner: String? get() = form.original?.takeIf { !it.own }?.let { it.ownerName.orEmpty() }

    /** True if *Save* is enabled. */
    val canSave: Boolean get() = editable && form.loaded && !form.saving && form.draft.isValid(ownEntities)
}

/** The draft the preview is for and whether the bridge can preview it ([enabled]). */
private data class PreviewInput(val draft: ScheduleDraft, val enabled: Boolean)

/** The connection as the editor needs it: whether it is open, stale, and which optional features it has. */
private class Link(val connected: Boolean, val stale: Boolean, val features: ScheduleFeatures)

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
    private val previews: SchedulePreviews,
) : ViewModel() {
    private val scheduleId: String? = savedState[ID_ARG]
    private val serverId = active.activeServerId.stateIn(viewModelScope, SharingStarted.Eagerly, null)
    private val form = MutableStateFlow(EditorForm(loaded = scheduleId == null))

    private val choices: Flow<Candidates> = active.activeServerId.flatMapLatest { id ->
        if (id == null) flowOf(Candidates()) else candidates.of(id)
    }

    private val link: Flow<Link> = combine(live.connected, live.stale, live.connection) { connected, stale, channel ->
        Link(connected, stale, ScheduleFeatures.of(channel?.features.orEmpty()))
    }

    /** The state of the screen. */
    val state: StateFlow<ScheduleEditorUiState> = combine(form, choices, link) { form, choices, link ->
        ScheduleEditorUiState(form, choices, link.connected, link.stale, link.features)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), ScheduleEditorUiState())

    /** What the preview needs: the draft, and whether the bridge can be asked about it now. */
    private val previewInput: Flow<PreviewInput> = combine(form, link) { form, link ->
        PreviewInput(form.draft, form.loaded && link.connected && link.features.repeat)
    }.distinctUntilChanged()

    init {
        if (scheduleId != null) viewModelScope.launch { load(scheduleId) }
        viewModelScope.launch { previewInput.collectLatest(::refreshPreview) }
    }

    /**
     * Asks the bridge for the next runs of the draft in [input] after [PREVIEW_DELAY_MS] without a further change
     * (concept 19.10). A draft that cannot be previewed, or one the bridge refuses, leaves the preview empty; saving
     * then reports the problem.
     */
    private suspend fun refreshPreview(input: PreviewInput) {
        val draft = input.draft
        val runs = if (input.enabled && draft.daysValid && draft.repeat.valid) {
            delay(PREVIEW_DELAY_MS)
            try {
                previews.nextRuns(draft)
            } catch (_: HaacException) {
                emptyList()
            }
        } else {
            emptyList()
        }
        form.update { it.copy(preview = runs) }
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

    /** *Once* (true) or *Repeat* (false): a one-off schedule gets a date, a repeating one weekly. */
    fun onOnce(once: Boolean) = onFreq(if (once) RepeatFreq.ONCE else RepeatFreq.WEEKLY)

    /** The frequency chips *Daily*, *Weekly*, *Monthly* and *Yearly*. */
    fun onFreq(freq: RepeatFreq) = change { it.withFreq(freq, LocalDate.now()) }

    /** The interval stepper: [steps] intervals more or less. */
    fun onInterval(steps: Int) = change { it.copy(repeat = it.repeat.steppedBy(steps)) }

    /** The first date of a repeating schedule, or the date of a one-off one. */
    fun onStartDate(date: LocalDate) = change { it.copy(repeat = it.repeat.startingOn(date)) }

    /** The last date of a repeating schedule, or none. */
    fun onEndDate(date: LocalDate?) = change { it.copy(repeat = it.repeat.copy(endDate = date)) }

    /** *Show in notifications*. */
    fun onNotify(notify: Boolean) = change { it.copy(notify = notify) }

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
        private const val PREVIEW_DELAY_MS = 500L
    }
}
