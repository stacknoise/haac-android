package com.stacknoise.haac.core.common.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Widest content of a screen without a second pane, in dp (concept 15.7, rule 3). */
val ReadableContentWidth: Dp = 640.dp

/** Width of the card on the sign-in and lock screens from the medium class on (concept 15.7). */
val CardContentWidth: Dp = 480.dp

/**
 * Limits the content to [maxWidth] and centres it; Compact keeps the full width.
 *
 * Put it after `background` so the colour still fills the window (concept 15.7, rule 3).
 */
@Composable
fun Modifier.centeredContent(maxWidth: Dp = ReadableContentWidth): Modifier =
    if (LocalWindowWidthClass.current == WindowWidthClass.Compact) {
        this
    } else {
        wrapContentWidth(Alignment.CenterHorizontally).widthIn(max = maxWidth).fillMaxWidth()
    }
