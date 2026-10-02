package com.stacknoise.haac.feature.entities.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.stacknoise.haac.core.common.ui.theme.HaacColors

/**
 * The four series colours of the history charts for one design. Text never takes a series colour. The accent green
 * `#2F6B4F` is too dark to tell apart from the other series, so the light charts use a lighter green step.
 */
internal class ChartPalette(val green: Color, val blue: Color, val magenta: Color, val yellow: Color) {
    /** Categorical order of timeline states other than on/off. */
    val categorical: List<Color> = listOf(green, blue, magenta, yellow)
}

/**
 * Light design: validated with the dataviz validator on the former dark background; green and yellow are darkened
 * by hand for the light "Salbei" background (contrast against white) and still need a validator run.
 */
internal val LightChartPalette = ChartPalette(
    green = Color(0xFF1F9A4B),
    blue = Color(0xFF3987E5),
    magenta = Color(0xFFD55181),
    yellow = Color(0xFFB27300),
)

/** Dark design: the set that passed the dataviz validator on the former dark background `#161826`. */
internal val DarkChartPalette = ChartPalette(
    green = Color(0xFF22A852),
    blue = Color(0xFF3987E5),
    magenta = Color(0xFFD55181),
    yellow = Color(0xFFC98500),
)

/** The chart colours of the current design; read in the composable and handed to the drawing functions. */
@Composable
internal fun chartPalette(): ChartPalette = if (HaacColors.IsDark) DarkChartPalette else LightChartPalette

/** Values shared by all designs. */
internal object ChartColors {
    /** Opacity of bands and phases behind the lines. */
    const val AREA_ALPHA = 0.22f
}
