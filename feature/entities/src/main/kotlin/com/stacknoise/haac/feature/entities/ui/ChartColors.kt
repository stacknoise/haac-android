package com.stacknoise.haac.feature.entities.ui

import androidx.compose.ui.graphics.Color
import com.stacknoise.haac.core.common.ui.theme.HaacColors

/**
 * Colours of the history charts. The set was validated with the dataviz validator on the former dark background;
 * green and yellow are darkened by hand for the light "Salbei" background (contrast against white) and still need
 * a validator run. The accent green `#2F6B4F` is too dark to tell apart from the other series, so charts use a
 * lighter green step; text never takes a series colour.
 */
internal object ChartColors {
    val Green = Color(0xFF1F9A4B)
    val Blue = Color(0xFF3987E5)
    val Magenta = Color(0xFFD55181)
    val Yellow = Color(0xFFB27300)

    /** Categorical order of timeline states other than on/off. */
    val Categorical = listOf(Green, Blue, Magenta, Yellow)

    /** The 2 dp ring around selected points, in the background colour. */
    val Ring = HaacColors.Background

    /** Opacity of bands and phases behind the lines. */
    const val AREA_ALPHA = 0.22f
}
