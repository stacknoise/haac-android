package com.stacknoise.haac.feature.entities.ui

import androidx.compose.ui.graphics.Color
import com.stacknoise.haac.core.common.ui.theme.HaacColors

/**
 * Colours of the history charts, checked with the dataviz validator against the background `#161826` (lightness
 * band, CVD and normal-vision separation, contrast). The brand green `#4DFF7A` is too light for a chart series,
 * so charts use a darker green step; text never takes a series colour.
 */
internal object ChartColors {
    val Green = Color(0xFF22A852)
    val Blue = Color(0xFF3987E5)
    val Magenta = Color(0xFFD55181)
    val Yellow = Color(0xFFC98500)

    /** Categorical order of timeline states other than on/off. */
    val Categorical = listOf(Green, Blue, Magenta, Yellow)

    /** The 2 dp ring around selected points, in the background colour. */
    val Ring = HaacColors.Background

    /** Opacity of bands and phases behind the lines. */
    const val AREA_ALPHA = 0.22f
}
