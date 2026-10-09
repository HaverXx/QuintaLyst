/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import kotlin.math.cos
import kotlin.math.sin

import androidx.compose.ui.graphics.toArgb

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.request.crossfade
import com.metrolist.music.constants.DynamicBackgroundStyle

private enum class BackgroundLayer { SOLID, FLAT, GRADIENT, ALBUM }

@Composable
fun MeshBackground(
    themeColor: Color,
    pureBlack: Boolean,
    modifier: Modifier = Modifier,
    intensity: Float = 0.5f,
    speed: Float = 1f,
    enabled: Boolean = true,
    style: DynamicBackgroundStyle = DynamicBackgroundStyle.GRADIENT,
    thumbnailUrl: String? = null,
    coverBlur: Float = 0.7f,
    backdrop: LiquidBackdrop? = null,
    solidColor: Color? = null,
    albumOpacity: Float = 1f,
    paused: Boolean = false,
    content: @Composable () -> Unit,
) {
    val isDark = isSystemInDarkTheme()
    val baseBg = if (pureBlack) Color.Black else if (isDark) Color(0xFF0D0D12) else Color(0xFFF5F5FA)
    val solid = solidColor ?: if (pureBlack) Color.Black else MaterialTheme.colorScheme.surface
    val layer =
        when {
            !enabled -> BackgroundLayer.SOLID
            paused -> BackgroundLayer.FLAT
            style == DynamicBackgroundStyle.GRADIENT -> BackgroundLayer.GRADIENT
            else -> BackgroundLayer.ALBUM
        }
    val canvasColor by animateColorAsState(
        targetValue = if (layer == BackgroundLayer.SOLID) solid else baseBg,
        animationSpec = tween(450),
        label = "meshCanvasColor",
    )

    Box(modifier = modifier) {
        Box(
            modifier =
                Modifier
                    .matchParentSize()
                    .then(if (backdrop != null) Modifier.liquidGlassSource(backdrop, Color.Transparent) else Modifier)
                    .background(canvasColor),
        ) {
            Crossfade(
                targetState = layer,
                modifier = Modifier.fillMaxSize(),
                animationSpec = tween(450),
                label = "meshLayer",
            ) { current ->
                when (current) {
                    BackgroundLayer.SOLID, BackgroundLayer.FLAT -> Box(Modifier.fillMaxSize())
                    BackgroundLayer.GRADIENT ->
                        AnimatedDynamicMesh(
                            themeColor = themeColor,
                            modifier = Modifier.fillMaxSize(),
                            intensity = intensity,
                            speed = speed,
                            baseColor = baseBg,
                        )
                    BackgroundLayer.ALBUM ->
                        LiquidGlassAlbumMesh(
                            thumbnailUrl = thumbnailUrl,
                            modifier = Modifier.fillMaxSize(),
                            pureBlack = pureBlack,
                            intensity = 0.8f,
                            baseColor = baseBg,
                            blurAmount = coverBlur,
                            imageAlpha = albumOpacity,
                        )
                }
            }
        }
        content()
    }
}

/**
 * Full-screen blurred liquid glass album art with subtle specular lighting
 * and breathing depth animation.
 */
@Composable
fun LiquidGlassAlbumMesh(
    thumbnailUrl: String?,
    modifier: Modifier = Modifier,
    pureBlack: Boolean = false,
    intensity: Float = 0.5f,
    baseColor: Color = Color(0xFF0D0D12),
    blurAmount: Float = 0.7f,
    imageAlpha: Float = 1f,
) {
    val isDark = isSystemInDarkTheme()
    val context = LocalContext.current
    // 0..1 -> 0..110dp. Se anima para que arrastrar el slider se vea fluido.
    val blurRadius by animateDpAsState(
        targetValue = (blurAmount.coerceIn(0f, 1f) * 110f).dp,
        animationSpec = tween(250),
        label = "coverBlurRadius",
    )

    val infinite = rememberInfiniteTransition(label = "liquidAlbumTransition")
    val breathScale by infinite.animateFloat(
        initialValue = 1.25f,
        targetValue = 1.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 8000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "breathScale",
    )
    val specularAngle by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 16000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "specularAngle",
    )

    Box(modifier = modifier.background(baseColor)) {
        if (!thumbnailUrl.isNullOrBlank()) {
            AnimatedContent(
                targetState = thumbnailUrl,
                transitionSpec = {
                    fadeIn(animationSpec = tween(900)) togetherWith fadeOut(animationSpec = tween(900))
                },
                label = "albumArtCrossfade",
                modifier = Modifier.fillMaxSize().graphicsLayer { alpha = imageAlpha.coerceIn(0f, 1f) },
            ) { url ->
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(url)
                        .size(600, 600)
                        .allowHardware(false)
                        .memoryCachePolicy(CachePolicy.ENABLED)
                        .diskCachePolicy(CachePolicy.ENABLED)
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            scaleX = breathScale
                            scaleY = breathScale
                        }
                        .blur(blurRadius, edgeTreatment = BlurredEdgeTreatment.Unbounded),
                )
            }
        }

        // Liquid glass specular sweep and readability scrim
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val rad = Math.toRadians(specularAngle.toDouble())
            val sx = w * 0.5f + cos(rad).toFloat() * w * 0.45f
            val sy = h * 0.5f + sin(rad).toFloat() * h * 0.45f

            // Specular sheen
            drawRect(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = if (isDark) 0.08f else 0.18f),
                        Color.Transparent,
                        Color.White.copy(alpha = if (isDark) 0.04f else 0.10f),
                    ),
                    start = Offset(sx, sy),
                    end = Offset(w - sx, h - sy),
                )
            )

            // Scrim to guarantee foreground readability
            val scrimColor = if (pureBlack) {
                Color.Black.copy(alpha = (0.50f + (1f - intensity) * 0.35f).coerceIn(0.40f, 0.90f))
            } else if (isDark) {
                Color(0xFF0A0A0F).copy(alpha = (0.45f + (1f - intensity) * 0.35f).coerceIn(0.35f, 0.85f))
            } else {
                Color.White.copy(alpha = (0.35f + (1f - intensity) * 0.35f).coerceIn(0.25f, 0.75f))
            }
            // El velo se atenúa junto con la opacidad de la portada para que 0 % deje solo el color base.
            drawRect(color = scrimColor.copy(alpha = scrimColor.alpha * imageAlpha.coerceIn(0f, 1f)))
        }
    }
}

/**
 * Reusable animated mesh gradient canvas driven by a theme color.
 */
@Composable
fun AnimatedDynamicMesh(
    themeColor: Color,
    modifier: Modifier = Modifier,
    intensity: Float = 0.5f,
    speed: Float = 1f,
    baseColor: Color = Color(0xFF0D0D12),
) {
    // Boost luminance and saturation for blobs so they pop vividly even on dark backgrounds
    val vibrantBase = remember(themeColor) {
        try {
            val hsv = FloatArray(3)
            android.graphics.Color.colorToHSV(themeColor.toArgb(), hsv)
            hsv[1] = hsv[1].coerceAtLeast(0.65f) // minimum 65% saturation
            hsv[2] = hsv[2].coerceAtLeast(0.70f) // minimum 70% brightness
            Color(android.graphics.Color.HSVToColor(hsv))
        } catch (_: Exception) {
            themeColor
        }
    }

    // Vibrant color harmonics
    val effIntensity = intensity.coerceIn(0.20f, 0.95f)

    // Smooth color transitions when song changes
    val blob1Color by animateColorAsState(
        targetValue = vibrantBase.copy(alpha = (effIntensity * 0.75f).coerceIn(0.20f, 0.90f)),
        animationSpec = tween(durationMillis = 1400, easing = FastOutSlowInEasing),
        label = "blob1Color",
    )
    val blob2Color by animateColorAsState(
        targetValue = vibrantBase
            .copy(
                alpha = (effIntensity * 0.60f).coerceIn(0.18f, 0.80f),
                red = (vibrantBase.red * 0.55f + 0.45f).coerceIn(0f, 1f),
                blue = (vibrantBase.blue * 1.45f).coerceIn(0f, 1f),
            ),
        animationSpec = tween(durationMillis = 1800, easing = FastOutSlowInEasing),
        label = "blob2Color",
    )
    val blob3Color by animateColorAsState(
        targetValue = vibrantBase
            .copy(
                alpha = (effIntensity * 0.50f).coerceIn(0.15f, 0.70f),
                green = (vibrantBase.green * 1.35f).coerceIn(0f, 1f),
                blue = (vibrantBase.blue * 0.70f).coerceIn(0f, 1f),
            ),
        animationSpec = tween(durationMillis = 2000, easing = FastOutSlowInEasing),
        label = "blob3Color",
    )

    // Dynamic, visibly fluid animation intervals (6s - 12s)
    val duration1 = (7_000 / speed).toInt().coerceAtLeast(1_800)
    val duration2 = (9_500 / speed).toInt().coerceAtLeast(2_200)
    val duration3 = (12_000 / speed).toInt().coerceAtLeast(2_600)
    val durationSweep = (14_000 / speed).toInt().coerceAtLeast(3_500)
    val durationPulse = (4_000 / speed).toInt().coerceAtLeast(1_200)

    val infinite = rememberInfiniteTransition(label = "meshBg")

    val angle1 by infinite.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(duration1, easing = LinearEasing), RepeatMode.Restart),
        label = "a1",
    )
    val angle2 by infinite.animateFloat(
        initialValue = 120f, targetValue = 480f,
        animationSpec = infiniteRepeatable(tween(duration2, easing = LinearEasing), RepeatMode.Restart),
        label = "a2",
    )
    val angle3 by infinite.animateFloat(
        initialValue = 240f, targetValue = 600f,
        animationSpec = infiniteRepeatable(tween(duration3, easing = LinearEasing), RepeatMode.Restart),
        label = "a3",
    )
    val sweepAngle by infinite.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(durationSweep, easing = LinearEasing), RepeatMode.Restart),
        label = "sw",
    )

    val pulse1 by infinite.animateFloat(
        initialValue = 0.75f, targetValue = 1.25f,
        animationSpec = infiniteRepeatable(tween(durationPulse, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "p1",
    )
    val pulse2 by infinite.animateFloat(
        initialValue = 1.20f, targetValue = 0.80f,
        animationSpec = infiniteRepeatable(tween((durationPulse * 1.3f).toInt(), easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "p2",
    )
    val pulse3 by infinite.animateFloat(
        initialValue = 0.85f, targetValue = 1.20f,
        animationSpec = infiniteRepeatable(tween((durationPulse * 1.6f).toInt(), easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "p3",
    )

    Box(
        modifier = modifier
            .background(baseColor)
            .drawBehind {
                val w = size.width
                val h = size.height

                // Continuous rotating liquid angle wash
                val radSw = Math.toRadians(sweepAngle.toDouble())
                val swX = w * 0.5f + cos(radSw).toFloat() * w * 0.55f
                val swY = h * 0.5f + sin(radSw).toFloat() * h * 0.55f
                drawRect(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            blob1Color.copy(alpha = blob1Color.alpha * 0.40f),
                            Color.Transparent,
                            blob2Color.copy(alpha = blob2Color.alpha * 0.35f),
                        ),
                        start = Offset(swX, swY),
                        end = Offset(w - swX, h - swY),
                    ),
                )

                // Blob 1 — top-right flowing orbit with breathing scale
                val r1 = Math.toRadians(angle1.toDouble())
                val cx1 = w * 0.55f + cos(r1).toFloat() * w * 0.35f
                val cy1 = h * 0.32f + sin(r1).toFloat() * h * 0.28f
                val rad1 = maxOf(w, h) * 0.75f * pulse1
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(blob1Color, Color.Transparent),
                        center = Offset(cx1, cy1),
                        radius = rad1,
                    ),
                    radius = rad1,
                    center = Offset(cx1, cy1),
                )

                // Blob 2 — bottom-left flowing orbit with breathing scale
                val r2 = Math.toRadians(angle2.toDouble())
                val cx2 = w * 0.35f + cos(r2).toFloat() * w * 0.32f
                val cy2 = h * 0.68f + sin(r2).toFloat() * h * 0.26f
                val rad2 = maxOf(w, h) * 0.65f * pulse2
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(blob2Color, Color.Transparent),
                        center = Offset(cx2, cy2),
                        radius = rad2,
                    ),
                    radius = rad2,
                    center = Offset(cx2, cy2),
                )

                // Blob 3 — centre floating accent with breathing scale
                val r3 = Math.toRadians(angle3.toDouble())
                val cx3 = w * 0.50f + cos(r3).toFloat() * w * 0.38f
                val cy3 = h * 0.50f + sin(r3).toFloat() * h * 0.32f
                val rad3 = maxOf(w, h) * 0.55f * pulse3
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(blob3Color, Color.Transparent),
                        center = Offset(cx3, cy3),
                        radius = rad3,
                    ),
                    radius = rad3,
                    center = Offset(cx3, cy3),
                )
            },
    )
}

