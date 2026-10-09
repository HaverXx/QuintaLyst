package com.metrolist.music.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.metrolist.music.R

/**
 * Titulares: Fraunces con el eje SOFT al máximo. Da letras blandas, redondeadas y algo retro,
 * como "nubes" de tinta, que contrastan con el texto limpio de DM Sans.
 */
val TitleFamily =
    FontFamily(
        listOf(400, 500, 600, 700).map { w ->
            Font(resId = R.font.fraunces_variable, weight = FontWeight(w))
        },
    )

/** Texto: DM Sans, geométrica y limpia, cercana al aire de SF Pro. */
val BodyFamily =
    FontFamily(
        listOf(400, 500, 600, 700).map { w ->
            Font(resId = R.font.dm_sans_variable, weight = FontWeight(w))
        },
    )

private fun title(size: Int, line: Int, weight: FontWeight = FontWeight.SemiBold, spacing: Float = -0.3f) =
    TextStyle(
        fontFamily = TitleFamily,
        fontWeight = weight,
        fontSize = size.sp,
        lineHeight = line.sp,
        letterSpacing = spacing.sp,
    )

private fun body(size: Float, line: Float, weight: FontWeight = FontWeight.Normal, spacing: Float = 0f) =
    TextStyle(
        fontFamily = BodyFamily,
        fontWeight = weight,
        fontSize = size.sp,
        lineHeight = line.sp,
        letterSpacing = spacing.sp,
    )

val MetrolistTypography =
    Typography(
        displayLarge = title(56, 62, spacing = -1f),
        displayMedium = title(44, 50, spacing = -0.8f),
        displaySmall = title(36, 42, spacing = -0.6f),
        headlineLarge = title(32, 38, spacing = -0.5f),
        headlineMedium = title(28, 34),
        headlineSmall = title(24, 30),
        titleLarge = title(22, 28, FontWeight.Medium, -0.2f),
        titleMedium = body(16f, 22f, FontWeight.SemiBold, 0.05f),
        titleSmall = body(14f, 20f, FontWeight.SemiBold, 0.1f),
        bodyLarge = body(16f, 24f, spacing = 0.1f),
        bodyMedium = body(14f, 20f, spacing = 0.15f),
        bodySmall = body(12f, 16f, spacing = 0.2f),
        labelLarge = body(14f, 20f, FontWeight.SemiBold, 0.2f),
        labelMedium = body(12f, 16f, FontWeight.Medium, 0.3f),
        labelSmall = body(11f, 16f, FontWeight.Medium, 0.4f),
    )

/** Esquinas más generosas y coherentes en toda la app. */
val MetrolistShapes =
    Shapes(
        extraSmall = RoundedCornerShape(10.dp),
        small = RoundedCornerShape(14.dp),
        medium = RoundedCornerShape(20.dp),
        large = RoundedCornerShape(26.dp),
        extraLarge = RoundedCornerShape(32.dp),
    )
