package com.stacknoise.haac.core.common.ui.theme

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * The 26 dp checkbox of the Salbei theme: accent square with a white tick when [checked], grey when checked but not
 * [enabled]. Toggles through [onCheckedChange]; null leaves toggling to a clickable parent.
 */
@Composable
fun HaacCheckbox(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val fill = when {
        !checked -> HaacColors.Surface
        enabled -> HaacColors.Accent
        else -> HaacColors.OutlineStrong
    }
    val border = if (checked) fill else HaacColors.OutlineStrong
    val toggle = if (onCheckedChange == null) {
        Modifier
    } else {
        Modifier.toggleable(checked, enabled, Role.Checkbox, onValueChange = onCheckedChange)
    }
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .then(toggle)
            .size(CheckboxSize)
            .background(fill, HaacShapes.Checkbox)
            .border(1.5.dp, border, HaacShapes.Checkbox),
    ) {
        if (checked) {
            Canvas(Modifier.size(14.dp)) {
                val tick = Path().apply {
                    moveTo(size.width * 0.08f, size.height * 0.52f)
                    lineTo(size.width * 0.38f, size.height * 0.82f)
                    lineTo(size.width * 0.92f, size.height * 0.2f)
                }
                val stroke = Stroke(2.2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                drawPath(tick, HaacColors.OnAccent, style = stroke)
            }
        }
    }
}

/** Side of [HaacCheckbox]. */
private val CheckboxSize = 26.dp

/**
 * A list card: white surface with a 1 dp outline and 22 dp corners; [selected] uses the accent tint with a 1.5 dp
 * accent border instead.
 */
@Composable
fun HaacCard(
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    val frame = if (selected) {
        Modifier
            .background(HaacColors.AccentTint, HaacShapes.Card)
            .border(1.5.dp, HaacColors.AccentBorder, HaacShapes.Card)
    } else {
        Modifier
            .background(HaacColors.Surface, HaacShapes.Card)
            .border(1.dp, HaacColors.Outline, HaacShapes.Card)
    }
    Column(modifier = modifier.fillMaxWidth().then(frame).padding(contentPadding), content = content)
}

/**
 * An empty state: an 80 dp accent-tint square holding [icon], the explaining [text] below it and, optionally, an
 * [action] such as a button.
 */
@Composable
fun HaacEmptyState(
    icon: Painter,
    text: String,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.fillMaxWidth().padding(top = 72.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(80.dp).background(HaacColors.AccentTintStrong, RoundedCornerShape(28.dp)),
        ) {
            Icon(icon, contentDescription = null, tint = HaacColors.Accent, modifier = Modifier.size(36.dp))
        }
        Text(
            text,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(start = 20.dp, top = 20.dp, end = 20.dp),
        )
        if (action != null) {
            Spacer(Modifier.height(16.dp))
            action()
        }
    }
}
