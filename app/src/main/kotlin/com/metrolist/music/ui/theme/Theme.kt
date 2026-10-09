/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.theme

import android.graphics.Bitmap
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.SaverScope
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.palette.graphics.Palette
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamiccolor.ColorSpec
import com.materialkolor.rememberDynamicColorScheme
import com.materialkolor.score.Score

val DefaultThemeColor = Color(0xFF8C9EFF)

@Composable
fun MetrolistTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    pureBlack: Boolean = false,
    themeColor: Color = DefaultThemeColor,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    // Determine if system dynamic colors should be used (Android S+ and default theme color)
    val useSystemDynamicColor = false

    // Select the appropriate color scheme generation method
    val baseColorScheme = if (useSystemDynamicColor) {
        // Use standard Material 3 dynamic color functions for system wallpaper colors
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else {
        // Use materialKolor only when a specific seed color is provided
        rememberDynamicColorScheme(
            seedColor = themeColor, // themeColor is guaranteed non-default here
            isDark = darkTheme,
            specVersion = ColorSpec.SpecVersion.SPEC_2025,
            style = PaletteStyle.TonalSpot // Keep existing style
        )
    }

    // Neutros propios: fondos suaves, casi blancos o casi negros con un toque frío, al estilo Apple.
    val styledScheme = remember(baseColorScheme, darkTheme) {
        if (darkTheme) {
            baseColorScheme.copy(
                background = Color(0xFF0B0B10),
                surface = Color(0xFF0B0B10),
                surfaceDim = Color(0xFF08080C),
                surfaceBright = Color(0xFF26262F),
                surfaceContainerLowest = Color(0xFF07070B),
                surfaceContainerLow = Color(0xFF111117),
                surfaceContainer = Color(0xFF16161D),
                surfaceContainerHigh = Color(0xFF1D1D25),
                surfaceContainerHighest = Color(0xFF25252E),
                onSurfaceVariant = Color(0xFFB4B4C2),
                outlineVariant = Color(0xFF34343F),
            )
        } else {
            baseColorScheme.copy(
                background = Color(0xFFF6F6FA),
                surface = Color(0xFFF6F6FA),
                surfaceDim = Color(0xFFE6E6EC),
                surfaceBright = Color(0xFFFFFFFF),
                surfaceContainerLowest = Color(0xFFFFFFFF),
                surfaceContainerLow = Color(0xFFF1F1F6),
                surfaceContainer = Color(0xFFECECF2),
                surfaceContainerHigh = Color(0xFFE6E6ED),
                surfaceContainerHighest = Color(0xFFE0E0E8),
                onSurfaceVariant = Color(0xFF5E5E6C),
                outlineVariant = Color(0xFFD3D3DD),
            )
        }
    }

    val colorScheme = remember(styledScheme, pureBlack, darkTheme) {
        if (darkTheme && pureBlack) styledScheme.pureBlack(true) else styledScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = MetrolistTypography,
        shapes = MetrolistShapes,
        content = content,
    )
}

fun Bitmap.extractThemeColor(): Color = Color(
    Palette.from(this)
        .maximumColorCount(8)
        .generate()
        .rankedColors(1, DefaultThemeColor.toArgb())
        .first()
)

internal fun Palette.rankedColors(
    desiredColorCount: Int,
    fallbackColor: Int,
): List<Int> = Score.score(
    swatches.associate { it.rgb to it.population },
    desiredColorCount,
    fallbackColor,
    true,
)

fun ColorScheme.pureBlack(apply: Boolean) =
    if (apply) copy(
        surface = Color.Black,
        background = Color.Black
    ) else this

val ColorSaver = object : Saver<Color, Int> {
    override fun restore(value: Int): Color = Color(value)
    override fun SaverScope.save(value: Color): Int = value.toArgb()
}
