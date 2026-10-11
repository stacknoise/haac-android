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
 * The chart colours of the current design: the tokens `chartGreen`, `chartBlue`, `chartMagenta` and `chartYellow` of
 * [HaacColors]. The light set is darkened by hand for the "Salbei" background and still needs a dataviz validator
 * run; the dark set passed it on the former dark background `#161826`. Read in the composable and handed to the
 * drawing functions.
 */
@Composable
internal fun chartPalette(): ChartPalette =
    ChartPalette(HaacColors.ChartGreen, HaacColors.ChartBlue, HaacColors.ChartMagenta, HaacColors.ChartYellow)

/** Values shared by all designs. */
internal object ChartColors {
    /** Opacity of bands and phases behind the lines. */
    const val AREA_ALPHA = 0.22f
}
