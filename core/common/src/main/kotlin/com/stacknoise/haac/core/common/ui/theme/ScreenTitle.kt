package com.stacknoise.haac.core.common.ui.theme

import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.sp

/** Smallest size a [HaacScreenTitle] shrinks to. */
private val TitleMinSize = 24.sp

/**
 * A screen title in the large headline style that stays on one line: a long word such as the German
 * *Benachrichtigungen* shrinks the text (down to 24 sp) instead of breaking in the middle of the word.
 */
@Composable
fun HaacScreenTitle(text: String, modifier: Modifier = Modifier) {
    val style = MaterialTheme.typography.headlineLarge
    Text(
        text,
        style = style,
        maxLines = 1,
        autoSize = TextAutoSize.StepBased(minFontSize = TitleMinSize, maxFontSize = style.fontSize),
        modifier = modifier,
    )
}
