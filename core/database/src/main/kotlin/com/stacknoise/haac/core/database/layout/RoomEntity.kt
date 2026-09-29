package com.stacknoise.haac.core.database.layout

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "room",
    foreignKeys = [
        ForeignKey(
            entity = HomeEntity::class,
            parentColumns = ["id"],
            childColumns = ["home_id"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = FloorEntity::class,
            parentColumns = ["id"],
            childColumns = ["floor_id"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [Index("home_id"), Index("floor_id")],
)
/**
 * A room of home [homeId], on floor [floorId] or directly in the home (table `room`, concept 6.1, 12). A trigger
 * ([LayoutTriggers]) rejects a floor of another home. [deletedAt] as in [HomeEntity].
 */
data class RoomEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "home_id") val homeId: String,
    @ColumnInfo(name = "floor_id") val floorId: String?,
    val name: String,
    val icon: String? = null,
    @ColumnInfo(name = "sort_order") val sortOrder: Int,
    @ColumnInfo(name = "deleted_at") val deletedAt: Long? = null,
)
