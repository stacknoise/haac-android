package com.stacknoise.haac.feature.layout.data

import com.stacknoise.haac.core.database.DatabaseTransactions
import com.stacknoise.haac.core.database.layout.LayoutTrashDao
import com.stacknoise.haac.core.error.ErrorFactory
import com.stacknoise.haac.core.error.database
import com.stacknoise.haac.feature.layout.domain.Deletion
import com.stacknoise.haac.core.database.layout.Floor
import com.stacknoise.haac.core.database.layout.Home
import com.stacknoise.haac.feature.layout.domain.PlaceKind
import com.stacknoise.haac.core.database.layout.Room
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Deletes homes, levels and rooms with a 5 second undo window (concept 6.2): a deletion marks the rows, *Undo*
 * restores them, and they are purged when the window ends or the next deletion starts. Only the latest deletion
 * can be undone.
 */
@Singleton
class PlaceTrash internal constructor(
    private val daos: LayoutDaos,
    private val trash: LayoutTrashDao,
    private val transactions: DatabaseTransactions,
    private val errors: ErrorFactory,
    private val clock: () -> Long,
) {
    /** Uses the wall clock. */
    @Inject
    constructor(daos: LayoutDaos, trash: LayoutTrashDao, transactions: DatabaseTransactions, errors: ErrorFactory) :
        this(daos, trash, transactions, errors, System::currentTimeMillis)

    private val lock = Mutex()
    private val current = MutableStateFlow<Deletion?>(null)
    private var lastMark = 0L

    /** The deletion that can still be undone; the Places screen shows it as a snackbar. */
    val pending: StateFlow<Deletion?> = current.asStateFlow()

    /** Deletes [home] with its levels and rooms. */
    suspend fun deleteHome(home: Home) = delete(PlaceKind.HOME, home.name) { at ->
        daos.homes.markDeleted(home.id, at)
        daos.floors.markDeletedOfHome(home.id, at)
        daos.rooms.markDeletedOfHome(home.id, at)
    }

    /** Deletes [floor]; its rooms are deleted too if [withRooms], else they stay directly in the home. */
    suspend fun deleteFloor(floor: Floor, withRooms: Boolean) = delete(PlaceKind.FLOOR, floor.name) { at ->
        daos.floors.markDeleted(floor.id, at)
        if (withRooms) daos.rooms.markDeletedOfFloor(floor.id, at)
    }

    /** Deletes [room]. */
    suspend fun deleteRoom(room: Room) = delete(PlaceKind.ROOM, room.name) { at -> daos.rooms.markDeleted(room.id, at) }

    /** *Undo*: restores everything [deletion] marked, if it is still the pending one. */
    suspend fun undo(deletion: Deletion) = lock.withLock {
        if (current.value == deletion) {
            errors.database {
                transactions.run {
                    trash.restoreHomes(deletion.at)
                    trash.restoreFloors(deletion.at)
                    trash.restoreRooms(deletion.at)
                }
            }
            current.value = null
        }
    }

    /** The undo window of [deletion] ended: it is purged for good. */
    suspend fun expire(deletion: Deletion) = lock.withLock {
        if (current.value == deletion) current.value = null
        purge(deletion.at)
    }

    /** Purges deletions whose undo window has ended, e.g. when the app was closed during it. */
    suspend fun purgeExpired() = lock.withLock {
        purge(minOf(clock() - UNDO_MS, (current.value?.at ?: Long.MAX_VALUE) - 1))
    }

    /** Ends the pending deletion, then marks the rows with [mark] under a new time and makes it pending. */
    private suspend fun delete(kind: PlaceKind, name: String, mark: suspend (Long) -> Unit) = lock.withLock {
        val now = clock()
        purge(maxOf(current.value?.at ?: 0L, now - UNDO_MS))
        val at = maxOf(now, lastMark + 1)
        lastMark = at
        errors.database { transactions.run { mark(at) } }
        current.value = Deletion(kind, name, at)
    }

    /** Deletes the rows marked at or before [before]; rooms first, so a level's purge only frees kept rooms. */
    private suspend fun purge(before: Long) = errors.database {
        transactions.run {
            trash.purgeRooms(before)
            trash.purgeFloors(before)
            trash.purgeHomes(before)
        }
    }

    /** Length of the undo window. */
    companion object {
        /** Milliseconds a deletion can be undone (concept 6.2). */
        const val UNDO_MS = 5_000L
    }
}
