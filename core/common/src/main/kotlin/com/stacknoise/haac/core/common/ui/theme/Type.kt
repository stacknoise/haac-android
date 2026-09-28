package com.stacknoise.haac.core.common.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.stacknoise.haac.core.common.R

/** Inter in the two weights the mockups use (concept 15.2). */
val InterFontFamily = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_medium, FontWeight.Medium),
)

/** Font for `entity_id`, IP addresses, ports and error codes. */
val MonoFontFamily: FontFamily = FontFamily.Monospace

/** Material 3 type scale with every style set to Inter. */
internal val HaacTypography: Typography = Typography().run {
    copy(
        displayLarge = displayLarge.inter(),
        displayMedium = displayMedium.inter(),
        displaySmall = displaySmall.inter(),
        headlineLarge = headlineLarge.inter(),
        headlineMedium = headlineMedium.inter(),
        headlineSmall = headlineSmall.inter(),
        titleLarge = titleLarge.inter(FontWeight.Medium),
        titleMedium = titleMedium.inter(FontWeight.Medium),
        titleSmall = titleSmall.inter(FontWeight.Medium),
        bodyLarge = bodyLarge.inter(),
        bodyMedium = bodyMedium.inter(),
        bodySmall = bodySmall.inter(),
        labelLarge = labelLarge.inter(FontWeight.Medium),
        labelMedium = labelMedium.inter(FontWeight.Medium),
        labelSmall = labelSmall.inter(FontWeight.Medium),
    )
}

/** Small uppercase section label ("HOME", "LEVEL") with letter spacing (concept 15.2). */
val SectionLabelStyle = TextStyle(
    fontFamily = InterFontFamily,
    fontWeight = FontWeight.Medium,
    fontSize = 11.sp,
    letterSpacing = 1.2.sp,
    color = HaacColors.OnSurfaceVariant,
)

/** Returns this style with Inter and the given weight (default: regular). */
private fun TextStyle.inter(weight: FontWeight = FontWeight.Normal): TextStyle =
    copy(fontFamily = InterFontFamily, fontWeight = weight)
