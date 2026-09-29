package com.stacknoise.haac.core.database.layout

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "floor",
    foreignKeys = [
        ForeignKey(
            entity = HomeEntity::class,
            parentColumns = ["id"],
            childColumns = ["home_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("home_id")],
)
/**
 * A floor ("Level" in the UI) of home [homeId] (table `floor`, concept 6.1, 12); [level] sorts the floors of a
 * home, e.g. -1 for the basement. [deletedAt] as in [HomeEntity].
 */
data class FloorEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "home_id") val homeId: String,
    val name: String,
    val level: Int,
    val icon: String? = null,
    @ColumnInfo(name = "sort_order") val sortOrder: Int,
    @ColumnInfo(name = "deleted_at") val deletedAt: Long? = null,
)
