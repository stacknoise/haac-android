package com.stacknoise.haac.core.common.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.stacknoise.haac.core.common.R

/** Figtree (variable font) in the four weights of the design handoff: 400, 500, 600 and 700. */
@OptIn(ExperimentalTextApi::class)
val BrandFontFamily = FontFamily(
    listOf(FontWeight.Normal, FontWeight.Medium, FontWeight.SemiBold, FontWeight.Bold).map { weight ->
        Font(R.font.figtree, weight, variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)))
    },
)

/** Font for `entity_id`, IP addresses, ports and error codes. */
val MonoFontFamily: FontFamily = FontFamily.Monospace

/** Material 3 type scale: hero, screen title, dialog title, row and tile names, body and label sizes of the handoff. */
internal val HaacTypography: Typography = Typography(
    displayLarge = brand(72, FontWeight.Bold, tracking = -0.035),
    displayMedium = brand(48, FontWeight.Bold, tracking = -0.03),
    displaySmall = brand(40, FontWeight.Bold, tracking = -0.025),
    headlineLarge = brand(38, FontWeight.Bold, tracking = -0.025, lineHeight = 1.1),
    headlineMedium = brand(26, FontWeight.Bold, lineHeight = 1.2),
    headlineSmall = brand(26, FontWeight.Bold, lineHeight = 1.2),
    titleLarge = brand(20, FontWeight.Bold),
    titleMedium = brand(17, FontWeight.SemiBold),
    titleSmall = brand(15, FontWeight.SemiBold),
    bodyLarge = brand(15, FontWeight.Normal, lineHeight = 1.45),
    bodyMedium = brand(14, FontWeight.Normal, lineHeight = 1.45),
    bodySmall = brand(13, FontWeight.Normal, lineHeight = 1.4),
    labelLarge = brand(15, FontWeight.SemiBold),
    labelMedium = brand(12, FontWeight.SemiBold),
    labelSmall = brand(12, FontWeight.Medium),
)

/** Eyebrow / section label: 12 sp semibold, uppercase by the caller, 12 % tracking. */
val SectionLabelStyle = TextStyle(
    fontFamily = BrandFontFamily,
    fontWeight = FontWeight.SemiBold,
    fontSize = 12.sp,
    letterSpacing = 0.12.em,
    color = HaacColors.OnSurfaceVariant,
)

/** A Figtree style: [size] in sp, [tracking] in em, [lineHeight] as a multiple of the size (0 keeps the default). */
private fun brand(size: Int, weight: FontWeight, tracking: Double = 0.0, lineHeight: Double = 0.0): TextStyle =
    TextStyle(
        fontFamily = BrandFontFamily,
        fontWeight = weight,
        fontSize = size.sp,
        letterSpacing = tracking.em,
        lineHeight = if (lineHeight > 0) (size * lineHeight).sp else TextStyle.Default.lineHeight,
    )
