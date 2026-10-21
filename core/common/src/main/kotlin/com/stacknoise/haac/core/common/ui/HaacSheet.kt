package com.stacknoise.haac.core.common.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.stacknoise.haac.core.common.ui.theme.HaacColors

/** Width of a sheet shown as a centred dialog from Medium (concept 15.7, rule 2). */
private val DialogSheetWidth = 560.dp

/** Corner radius of the sheet and its dialog form. */
private val SheetCorner = 32.dp

/** Top padding of the dialog form, where the bottom sheet has its drag handle. */
private val DialogTopPadding = 24.dp

/**
 * A form sheet (concept 15.7): a modal bottom sheet in Compact, a centred 560 dp dialog in Medium and Expanded.
 * [onDismiss] is called for a tap outside, back and swipe down; [content] is the same in both forms.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HaacSheet(onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    if (LocalWindowWidthClass.current == WindowWidthClass.Compact) {
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = HaacColors.Surface,
            shape = RoundedCornerShape(topStart = SheetCorner, topEnd = SheetCorner),
            content = content,
        )
    } else {
        Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
            Surface(
                color = HaacColors.Surface,
                shape = RoundedCornerShape(SheetCorner),
                modifier = Modifier.widthIn(max = DialogSheetWidth).fillMaxWidth().padding(horizontal = 24.dp),
            ) {
                Column(modifier = Modifier.verticalScroll(rememberScrollState()).padding(top = DialogTopPadding)) {
                    content()
                }
            }
        }
    }
}
