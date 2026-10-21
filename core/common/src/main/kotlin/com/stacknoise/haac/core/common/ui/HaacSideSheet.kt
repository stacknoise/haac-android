package com.stacknoise.haac.core.common.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.stacknoise.haac.core.common.ui.theme.HaacColors

/** Width of a side sheet for a detail (concept 15.7, rule 1). */
val DetailSideSheetWidth = 400.dp

/** Width of a side sheet for an editor (concept 15.7, rule 1). */
val EditorSideSheetWidth = 480.dp

/** Corner radius on the inner side of the side sheet. */
private val SideSheetCorner = 28.dp

/**
 * A modal side sheet from the right edge of the window (concept 15.7, rule 1): the detail of a list item in Medium
 * width. A tap outside and back call [onDismiss]; [width] is [DetailSideSheetWidth] or [EditorSideSheetWidth].
 */
@Composable
fun HaacSideSheet(onDismiss: () -> Unit, width: Dp = DetailSideSheetWidth, content: @Composable () -> Unit) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onDismiss() },
        ) {
            Surface(
                color = HaacColors.Surface,
                shape = RoundedCornerShape(topStart = SideSheetCorner, bottomStart = SideSheetCorner),
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .width(width)
                    .fillMaxHeight()
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {},
            ) {
                content()
            }
        }
    }
}
