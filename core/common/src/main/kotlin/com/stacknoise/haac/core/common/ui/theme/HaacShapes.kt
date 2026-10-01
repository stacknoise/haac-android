package com.stacknoise.haac.core.common.ui.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/** Corner radii of the Salbei theme: 12 dp chips, 16 dp fields and icon boxes, 20 dp cards and buttons, 26 dp tiles. */
object HaacShapes {
    val Small = RoundedCornerShape(12.dp)
    val Medium = RoundedCornerShape(16.dp)
    val Button = RoundedCornerShape(18.dp)
    val Card = RoundedCornerShape(22.dp)
    val Tile = RoundedCornerShape(26.dp)
    val Dialog = RoundedCornerShape(32.dp)
    val Checkbox = RoundedCornerShape(9.dp)
    val Full = CircleShape
}

/** Material 3 shapes mapped to [HaacShapes]. */
internal val HaacMaterialShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = HaacShapes.Small,
    medium = HaacShapes.Medium,
    large = HaacShapes.Tile,
    extraLarge = HaacShapes.Dialog,
)
