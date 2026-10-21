package com.stacknoise.haac.core.common.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.platform.LocalWindowInfo

/** Width class of the app window: phone, tablet in portrait, tablet in landscape (concept 15.7). */
enum class WindowWidthClass { Compact, Medium, Expanded }

/** Smallest window width in dp of the class [WindowWidthClass.Medium] (concept 15.7). */
const val MediumMinWidthDp = 600

/** Smallest window width in dp of the class [WindowWidthClass.Expanded] (concept 15.7). */
const val ExpandedMinWidthDp = 840

/** The width class of a window that is [widthDp] wide (concept 15.7). */
fun windowWidthClassOf(widthDp: Int): WindowWidthClass = when {
    widthDp >= ExpandedMinWidthDp -> WindowWidthClass.Expanded
    widthDp >= MediumMinWidthDp -> WindowWidthClass.Medium
    else -> WindowWidthClass.Compact
}

/** The width class of the current window; screens read it instead of the configuration (concept 15.7). */
val LocalWindowWidthClass = compositionLocalOf { WindowWidthClass.Compact }

/** Provides [LocalWindowWidthClass] for [content]; follows rotation and window resizing (concept 15.7). */
@Composable
fun ProvideWindowWidthClass(content: @Composable () -> Unit) {
    val widthDp = LocalWindowInfo.current.containerDpSize.width.value.toInt()
    CompositionLocalProvider(LocalWindowWidthClass provides windowWidthClassOf(widthDp), content = content)
}
