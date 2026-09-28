package com.stacknoise.haac.core.common.ui.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/** Corner radii of concept 15.2: 8 dp chips and fields, 12 dp tiles, dialogs and buttons. */
object HaacShapes {
    val Small = RoundedCornerShape(8.dp)
    val Medium = RoundedCornerShape(12.dp)
    val Full = CircleShape
}

/** Material 3 shapes mapped to [HaacShapes]. */
internal val HaacMaterialShapes = Shapes(
    extraSmall = HaacShapes.Small,
    small = HaacShapes.Small,
    medium = HaacShapes.Medium,
    large = HaacShapes.Medium,
    extraLarge = HaacShapes.Medium,
)
