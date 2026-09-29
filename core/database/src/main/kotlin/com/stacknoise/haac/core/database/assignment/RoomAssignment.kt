package com.stacknoise.haac.core.database.assignment

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import com.stacknoise.haac.core.database.layout.RoomEntity

/**
 * Size of a tile in the two-column room grid (concept 7.2, M-05): [label] is the name the UI shows, [columns] and
 * [rows] the cells it covers. As in the mockups, "2×2" is one column wide and two rows high.
 */
enum class TileSize(val label: String, val columns: Int, val rows: Int) {
    SMALL("1×1", 1, 1),
    WIDE("2×1", 2, 1),
    LARGE("2×2", 1, 2),
}

@Entity(
    tableName = "room_entity",
    primaryKeys = ["room_id", "entity_id"],
    foreignKeys = [
        ForeignKey(
            entity = RoomEntity::class,
            parentColumns = ["id"],
            childColumns = ["room_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("entity_id")],
)
/**
 * Entity [entityId] placed in room [roomId] (table `room_entity`, concept 7.2, 12); the instance follows from
 * the room's home. An entity can be in any number of rooms, once per room.
 */
data class RoomAssignment(
    @ColumnInfo(name = "room_id") val roomId: String,
    @ColumnInfo(name = "entity_id") val entityId: String,
    @ColumnInfo(name = "sort_order") val sortOrder: Int,
    @ColumnInfo(name = "tile_size") val tileSize: TileSize,
    @ColumnInfo(name = "added_at") val addedAt: Long,
)
