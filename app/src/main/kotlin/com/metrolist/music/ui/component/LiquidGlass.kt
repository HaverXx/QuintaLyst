/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.component

import android.graphics.RuntimeShader
import android.graphics.Shader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonElevation
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardElevation
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.draw.alpha
import androidx.compose.material3.SelectableChipColors
import androidx.compose.material3.SelectableChipElevation
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlin.math.ceil
import kotlin.math.min

/**
 * Liquid Glass, en tres piezas:
 *  1. [liquidGlassSource] graba el contenido de la app en un [GraphicsLayer].
 *  2. [liquidGlass] vuelve a dibujar ese contenido dentro de una forma, doblándolo
 *     en el borde (refracción), separando canales de color (aberración cromática),
 *     saturando y desenfocando un poco (frosting).
 *  3. Encima se pintan el tinte, el brillo superior y el reflejo especular del canto.
 *
 * Refracción completa: Android 13+ (AGSL). Android 12: solo desenfoque. Antes: color sólido.
 */
@Stable
class LiquidBackdrop internal constructor(
    internal val layer: GraphicsLayer,
) {
    internal var coordinates: LayoutCoordinates? = null
    var drawVersion by androidx.compose.runtime.mutableIntStateOf(0)
}

val LocalLiquidBackdrop = compositionLocalOf<LiquidBackdrop?> { null }

/** Backdrop que graba SOLO el fondo de la app (malla / fondo de pantalla), sin pestañas ni botones. Lo usa el panel de "Reproduciendo". */
val LocalPlayerBackdrop = compositionLocalOf<LiquidBackdrop?> { null }

/**
 * true cuando lo que hay detrás del cristal es un fondo plano (color sólido o degradado animado).
 * Con el arcoíris (aberración cromática) sobre un fondo liso solo salen franjas de color estáticas, así que
 * el shader lo apaga en las zonas lisas y lo mantiene donde hay contenido (portadas, texto, bordes).
 */
val LocalGlassFlatBackground = compositionLocalOf { false }

@Composable
fun rememberLiquidBackdrop(): LiquidBackdrop {
    val layer = rememberGraphicsLayer()
    return remember(layer) { LiquidBackdrop(layer) }
}

fun Modifier.liquidGlassSource(
    backdrop: LiquidBackdrop,
    background: Color,
): Modifier =
    this
        .onGloballyPositioned { backdrop.coordinates = it }
        .drawWithContent {
            backdrop.layer.record {
                if (background != Color.Transparent) {
                    drawRect(background)
                }
                this@drawWithContent.drawContent()
            }
            backdrop.drawVersion++
            drawLayer(backdrop.layer)
        }

private class GlassCoordinatesHolder {
    var coordinates: LayoutCoordinates? = null
}

/**
 * Cristal esmerilado con volumen: relleno degradado, reflejo especular en la esquina superior izquierda,
 * brillo interior en el canto y borde de luz. Se usa cuando no hay fondo que refractar.
 */
@Composable
fun Modifier.frostedGlass(shape: Shape): Modifier {
    if (LocalRetroStyle.current) return this.retroPanel(shape)
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val fill =
        Brush.verticalGradient(
            listOf(
                Color.White.copy(alpha = if (isDark) 0.18f else 0.55f),
                Color.White.copy(alpha = if (isDark) 0.07f else 0.26f),
            ),
        )
    val rim =
        Brush.verticalGradient(
            listOf(Color.White.copy(alpha = if (isDark) 0.55f else 0.90f), Color.White.copy(alpha = 0.10f)),
        )
    return this
        .clip(shape)
        .background(fill)
        .drawWithCache {
            val specular =
                Brush.radialGradient(
                    colors = listOf(Color.White.copy(alpha = if (isDark) 0.22f else 0.45f), Color.Transparent),
                    center = Offset(size.width * 0.15f, 0f),
                    radius = size.maxDimension * 0.85f,
                )
            val bottomGlow =
                Brush.verticalGradient(
                    0f to Color.Transparent,
                    0.7f to Color.Transparent,
                    1f to Color.White.copy(alpha = if (isDark) 0.07f else 0.14f),
                )
            onDrawBehind {
                drawRect(specular)
                drawRect(bottomGlow)
            }
        }.border(4.dp, Color.White.copy(alpha = if (isDark) 0.06f else 0.12f), shape)
        .border(1.dp, rim, shape)
}

@Composable
fun Modifier.frostedGlass(cornerRadius: Dp): Modifier = frostedGlass(RoundedCornerShape(cornerRadius))

@Composable
fun Modifier.liquidGlass(
    cornerRadius: Dp,
    fallbackColor: Color,
    refraction: Dp = 10.dp,
    bezel: Dp = 16.dp,
    blur: Dp = 0.5.dp,
    chromaticAberration: Float = 0.35f,
    saturation: Float = 1.55f,
    tint: Color? = null,
    edges: Boolean = true,
): Modifier =
    liquidGlassImpl(cornerRadius, null, fallbackColor, refraction, bezel, blur, chromaticAberration, saturation, tint, edges)

/** Igual que [liquidGlass] con radio fijo, pero deduce el radio de cualquier [Shape] (círculo, píldora, esquinas redondeadas). */
@Composable
fun Modifier.liquidGlass(
    shape: Shape,
    fallbackColor: Color,
    refraction: Dp = 10.dp,
    bezel: Dp = 16.dp,
    blur: Dp = 0.5.dp,
    chromaticAberration: Float = 0.35f,
    saturation: Float = 1.55f,
    tint: Color? = null,
    edges: Boolean = true,
): Modifier =
    liquidGlassImpl(null, shape, fallbackColor, refraction, bezel, blur, chromaticAberration, saturation, tint, edges)

private fun resolveCornerRadiusPx(
    shape: Shape,
    size: androidx.compose.ui.geometry.Size,
    direction: androidx.compose.ui.unit.LayoutDirection,
    density: androidx.compose.ui.unit.Density,
): Float =
    when (val outline = shape.createOutline(size, direction, density)) {
        is androidx.compose.ui.graphics.Outline.Rounded -> outline.roundRect.topLeftCornerRadius.x
        is androidx.compose.ui.graphics.Outline.Rectangle -> 0f
        is androidx.compose.ui.graphics.Outline.Generic -> min(size.width, size.height) / 2f
    }

@Composable
private fun Modifier.liquidGlassImpl(
    cornerRadius: Dp?,
    shapeArg: Shape?,
    fallbackColor: Color,
    refraction: Dp,
    bezel: Dp,
    blur: Dp,
    chromaticAberration: Float,
    saturation: Float,
    tint: Color?,
    edges: Boolean,
): Modifier {
    val backdrop = LocalLiquidBackdrop.current
    val flatBackground = LocalGlassFlatBackground.current
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val shape: Shape = shapeArg ?: RoundedCornerShape(cornerRadius!!)

    if (backdrop == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
        return this.frostedGlass(shape)
    }

    val glassLayer = rememberGraphicsLayer()
    val holder = remember { GlassCoordinatesHolder() }
    // Posición del cristal respecto al fondo. Al hacer scroll el botón solo cambia de posición (no se vuelve a dibujar
    // por sí mismo), así que sin esto se quedaba con el recorte del fondo de la primera vez. Al ser estado leído en el
    // dibujo, cada cambio de posición fuerza a regrabar el cristal.
    var trackedPosition by remember { mutableStateOf(Offset.Zero) }
    val shader: Any? = remember { if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) createShader() else null }
    val fill = tint ?: if (isDark) Color.Black.copy(alpha = 0.18f) else Color.White.copy(alpha = 0.25f)

    return this
        .onGloballyPositioned { self ->
            holder.coordinates = self
            val source = backdrop.coordinates
            if (source != null && source.isAttached && self.isAttached) {
                try {
                    val pos = source.localPositionOf(self, Offset.Zero)
                    if (pos != trackedPosition) trackedPosition = pos
                } catch (e: IllegalArgumentException) {
                    // otra ventana: sin posición relativa
                }
            }
        }
        .drawWithCache {
            val w = size.width
            val h = size.height
            val radius =
                if (cornerRadius != null) {
                    min(cornerRadius.toPx(), min(w, h) / 2f)
                } else {
                    resolveCornerRadiusPx(shapeArg!!, size, layoutDirection, this)
                }
            val padPx = ceil(refraction.toPx() * 1.8f).toInt()
            val layerSize = IntSize(ceil(w).toInt() + 2 * padPx, ceil(h).toInt() + 2 * padPx)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                glassLayer.renderEffect =
                    glassEffect(
                        shader = shader,
                        width = w,
                        height = h,
                        pad = padPx.toFloat(),
                        radius = radius,
                        bezel = bezel.toPx(),
                        strength = refraction.toPx(),
                        chroma = chromaticAberration,
                        saturation = saturation,
                        blurPx = if (shader != null) blur.toPx() else 18.dp.toPx(),
                        flatBackground = flatBackground,
                    )
            }

            val clipShape =
                Path().apply { addRoundRect(RoundRect(0f, 0f, w, h, CornerRadius(radius))) }
            val inset = 0.75.dp.toPx()
            val rimShape =
                Path().apply {
                    addRoundRect(
                        RoundRect(inset, inset, w - inset, h - inset, CornerRadius((radius - inset).coerceAtLeast(0f))),
                    )
                }
            val center = Offset(w / 2f, h / 2f)
            // Luz desde arriba a la izquierda: el canto brilla en esa esquina y, más suave, en la opuesta.
            val rimBrush =
                Brush.sweepGradient(
                    0f to Color.White.copy(alpha = 0.45f),
                    0.125f to Color.White.copy(alpha = 0.60f),
                    0.30f to Color.White.copy(alpha = 0.05f),
                    0.50f to Color.White.copy(alpha = 0.05f),
                    0.625f to Color.White.copy(alpha = 0.95f),
                    0.75f to Color.White.copy(alpha = 0.15f),
                    0.90f to Color.White.copy(alpha = 0.06f),
                    1f to Color.White.copy(alpha = 0.45f),
                    center = center,
                )
            val sheenBrush =
                Brush.verticalGradient(
                    0f to Color.White.copy(alpha = if (isDark) 0.10f else 0.22f),
                    0.4f to Color.Transparent,
                    0.75f to Color.Transparent,
                    1f to Color.White.copy(alpha = if (isDark) 0.05f else 0.10f),
                )
            val frostBrush =
                Brush.verticalGradient(
                    listOf(
                        Color.White.copy(alpha = if (isDark) 0.16f else 0.50f),
                        Color.White.copy(alpha = if (isDark) 0.06f else 0.24f),
                    ),
                )
            val innerGlowBrush = SolidColor(Color.White.copy(alpha = 0.10f))
            val innerGlowWidth = 5.dp.toPx()
            val rimWidth = 1.5.dp.toPx()

            onDrawWithContent {
                val version = backdrop.drawVersion // Force invalidation when source redraws
                val tracked = trackedPosition // Force invalidation when the glass moves (scroll)
                val source = backdrop.coordinates
                val self = holder.coordinates
                // Si el cristal está en otra ventana (diálogo, menú emergente) no comparte jerarquía con la fuente.
                val rel =
                    if (source != null && self != null && source.isAttached && self.isAttached) {
                        try {
                            source.localPositionOf(self, Offset.Zero)
                        } catch (e: IllegalArgumentException) {
                            null
                        }
                    } else {
                        null
                    }
                if (rel != null) {
                    glassLayer.record(size = layerSize) {
                        translate(padPx - rel.x, padPx - rel.y) { drawLayer(backdrop.layer) }
                    }
                    clipPath(clipShape) {
                        translate(-padPx.toFloat(), -padPx.toFloat()) { drawLayer(glassLayer) }
                        drawRect(fill)
                        if (edges) {
                            drawPath(clipShape, sheenBrush)
                            drawPath(rimShape, innerGlowBrush, style = Stroke(innerGlowWidth))
                        }
                    }
                } else {
                    clipPath(clipShape) { drawRect(frostBrush) }
                }
                if (edges) drawPath(rimShape, rimBrush, style = Stroke(rimWidth))
                drawContent()
            }
        }
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private fun createShader(): Any = RuntimeShader(LIQUID_GLASS_AGSL)

@RequiresApi(Build.VERSION_CODES.S)
private fun glassEffect(
    shader: Any?,
    width: Float,
    height: Float,
    pad: Float,
    radius: Float,
    bezel: Float,
    strength: Float,
    chroma: Float,
    saturation: Float,
    blurPx: Float,
    flatBackground: Boolean,
): androidx.compose.ui.graphics.RenderEffect {
    val blurEffect =
        android.graphics.RenderEffect.createBlurEffect(blurPx, blurPx, Shader.TileMode.CLAMP)
    if (shader == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
        return blurEffect.asComposeRenderEffect()
    }
    return refractionEffect(shader, blurEffect, width, height, pad, radius, bezel, strength, chroma, saturation, flatBackground)
        .asComposeRenderEffect()
}

// El desenfoque va primero (inner) y la refracción después (outer).
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private fun refractionEffect(
    shader: Any,
    blurEffect: android.graphics.RenderEffect,
    width: Float,
    height: Float,
    pad: Float,
    radius: Float,
    bezel: Float,
    strength: Float,
    chroma: Float,
    saturation: Float,
    flatBackground: Boolean,
): android.graphics.RenderEffect {
    val runtimeShader = shader as RuntimeShader
    runtimeShader.setFloatUniform("flatBg", if (flatBackground) 1f else 0f)
    runtimeShader.setFloatUniform("size", width, height)
    runtimeShader.setFloatUniform("pad", pad)
    runtimeShader.setFloatUniform("radius", radius)
    runtimeShader.setFloatUniform("bezel", bezel)
    runtimeShader.setFloatUniform("strength", strength)
    runtimeShader.setFloatUniform("chroma", chroma)
    runtimeShader.setFloatUniform("sat", saturation)
    val refract = android.graphics.RenderEffect.createRuntimeShaderEffect(runtimeShader, "content")
    return android.graphics.RenderEffect.createChainEffect(refract, blurEffect)
}

// Refracción fluida: el borde curva la luz con menisco acentuado y el interior actúa como lente líquida distorsionando caracteres detrás.
private const val LIQUID_GLASS_AGSL = """
uniform shader content;
uniform float2 size;
uniform float pad;
uniform float radius;
uniform float bezel;
uniform float strength;
uniform float chroma;
uniform float sat;
uniform float flatBg;

half4 main(float2 fc) {
    float2 p = fc - float2(pad);
    float2 c = size * 0.5;
    float2 q = abs(p - c) - (c - float2(radius));
    float2 mq = max(q, float2(0.0));
    float depth = radius - (length(mq) + min(max(q.x, q.y), 0.0));
    
    float2 s = sign(p - c);
    float2 n = float2(0.0);
    if (mq.x > 0.0 || mq.y > 0.0) {
        n = normalize(mq) * s;
    } else if (q.x > q.y) {
        n = float2(s.x, 0.0);
    } else {
        n = float2(0.0, s.y);
    }

    // 1. Refracción de bisel/menisco exterior muy marcada
    float edgeT = clamp(depth / max(bezel, 1.0), 0.0, 1.0);
    float rimFactor = pow(1.0 - edgeT, 1.4);
    float2 edgeD = n * (rimFactor * strength * 1.85);

    // 2. Refracción de lente y superficie líquida interior (distorsiona texto y elementos detrás)
    float2 normP = (p - c) / max(c, float2(1.0));
    float rLen = length(normP);
    // Magnificación convexa de lente: amplía y curva las letras detrás del cristal
    float2 lensMagnify = -normP * (rLen * strength * 0.45);

    // Ondulación de fluido para refractar letras y contornos de caracteres detrás del cristal
    float wave1 = sin(p.y * 0.045 + p.x * 0.02) * cos(p.x * 0.035 - p.y * 0.02);
    float wave2 = cos(p.x * 0.05 + p.y * 0.03) * sin(p.y * 0.04);
    float2 fluidWarp = float2(wave1, wave2) * (strength * 0.40);

    float2 interiorD = lensMagnify + fluidWarp;
    float2 d = edgeD + interiorD;

    // Dispersión cromática (arcoíris): las muestras se acotan al área real del contenido para que no
    // entren bordes transparentes (eso era lo que parpadeaba). Sobre un fondo plano (sólido o degradado)
    // se reduce a menos de la mitad donde el contenido es liso y se mantiene entero donde hay detalle.
    float2 lo = float2(pad + 0.5);
    float2 hi = float2(pad) + size - float2(0.5);
    float chromaAmt = min(chroma, 0.6) * (0.45 + 0.55 * rimFactor);
    float2 c0 = clamp(fc + d, lo, hi);
    half4 g = content.eval(c0);

    if (flatBg > 0.5) {
        float gl = float(dot(g.rgb, half3(0.299, 0.587, 0.114)));
        float2 ox = float2(9.0, 0.0);
        float2 oy = float2(0.0, 9.0);
        float contrast = 0.0;
        contrast = max(contrast, abs(float(dot(content.eval(clamp(c0 + ox, lo, hi)).rgb, half3(0.299, 0.587, 0.114))) - gl));
        contrast = max(contrast, abs(float(dot(content.eval(clamp(c0 - ox, lo, hi)).rgb, half3(0.299, 0.587, 0.114))) - gl));
        contrast = max(contrast, abs(float(dot(content.eval(clamp(c0 + oy, lo, hi)).rgb, half3(0.299, 0.587, 0.114))) - gl));
        contrast = max(contrast, abs(float(dot(content.eval(clamp(c0 - oy, lo, hi)).rgb, half3(0.299, 0.587, 0.114))) - gl));
        chromaAmt *= 0.45 + 0.55 * smoothstep(0.02, 0.12, contrast);
    }

    float2 c1 = clamp(fc + d * (1.0 + chromaAmt * 3.4), lo, hi);
    float2 c2 = clamp(fc + d * (1.0 - chromaAmt * 3.4), lo, hi);
    half r = content.eval(c1).r;
    half b = content.eval(c2).b;
    half3 rgb = clamp(half3(r, g.g, b), half3(0.0), half3(g.a));

    // Realce de saturación
    half l = dot(rgb, half3(0.299, 0.587, 0.114));
    rgb = mix(half3(l), rgb, half(sat));
    return half4(rgb, g.a);
}
"""

/**
 * Modern Apple Liquid Glass button with specular rim lighting and spring bounce interaction.
 */
@Composable
fun LiquidGlassButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = CircleShape,
    isActive: Boolean = false,
    activeTint: Color = MaterialTheme.colorScheme.primary,
    baseTint: Color? = null,
    borderWidth: Dp = 1.dp,
    cornerRadius: Dp? = null,
    isAccented: Boolean = false,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    content: @Composable BoxScope.() -> Unit,
) {
    // Cristal real (refracta lo que hay detrás) siempre que haya fondo y el botón no esté activo;
    // el radio sale de cornerRadius o, si no se da, de la propia forma, así todos los botones casan.
    // isAccented: cristal normal (sin color) con un poco más de cuerpo y refracción, para el botón principal.
    val useGlass = LocalLiquidBackdrop.current != null
    val retro = LocalRetroStyle.current
    val accentDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val glassTint =
        if (isActive) {
            activeTint.copy(alpha = 0.40f)
        } else if (isAccented) {
            Color.White.copy(alpha = if (accentDark) 0.16f else 0.42f)
        } else {
            null
        }
    val glassRefraction = if (isAccented) 13.dp else 10.dp
    val glassModifier =
        if (!useGlass) {
            Modifier
        } else if (cornerRadius != null) {
            Modifier.liquidGlass(
                cornerRadius = cornerRadius,
                fallbackColor = Color.White.copy(alpha = 0.12f),
                refraction = glassRefraction,
                bezel = 16.dp,
                blur = 0.5.dp,
                tint = glassTint,
            )
        } else {
            Modifier.liquidGlass(
                shape = shape,
                fallbackColor = Color.White.copy(alpha = 0.12f),
                refraction = glassRefraction,
                bezel = 16.dp,
                blur = 0.5.dp,
                tint = glassTint,
            )
        }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 600f),
        label = "glassBtnScale",
    )

    val backgroundBrush = remember(isActive, activeTint, baseTint, isAccented) {
        if (isActive) {
            Brush.linearGradient(
                listOf(
                    activeTint.copy(alpha = 0.55f),
                    activeTint.copy(alpha = 0.28f),
                )
            )
        } else {
            Brush.linearGradient(
                listOf(
                    (baseTint ?: Color.White).copy(alpha = if (isAccented) 0.30f else 0.20f),
                    (baseTint ?: Color.White).copy(alpha = if (isAccented) 0.11f else 0.06f),
                )
            )
        }
    }

    val rimBrush = remember(isActive, activeTint, isAccented) {
        if (isActive) {
            Brush.verticalGradient(
                listOf(
                    activeTint.copy(alpha = 0.85f),
                    Color.White.copy(alpha = 0.25f),
                )
            )
        } else {
            Brush.verticalGradient(
                listOf(
                    Color.White.copy(alpha = if (isAccented) 0.65f else 0.45f),
                    Color.White.copy(alpha = if (isAccented) 0.14f else 0.08f),
                )
            )
        }
    }

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .then(
                if (useGlass) {
                    glassModifier
                } else if (retro) {
                    Modifier.retroKey(shape, isPressed, isActive)
                } else {
                    Modifier
                        .clip(shape)
                        .background(backgroundBrush)
                        .border(borderWidth, rimBrush, shape)
                },
            )
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
        content = content,
    )
}

/**
 * Modern Liquid Glass container box with specular rim.
 */
@Composable
fun LiquidGlassBox(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(16.dp),
    borderWidth: Dp = 1.dp,
    frostedAlpha: Float = 0.14f,
    contentAlignment: Alignment = Alignment.Center,
    content: @Composable BoxScope.() -> Unit,
) {
    val backgroundBrush = remember(frostedAlpha) {
        Brush.linearGradient(
            listOf(
                Color.White.copy(alpha = frostedAlpha),
                Color.White.copy(alpha = frostedAlpha * 0.4f),
            )
        )
    }
    val rimBrush = remember {
        Brush.verticalGradient(
            listOf(
                Color.White.copy(alpha = 0.35f),
                Color.White.copy(alpha = 0.06f),
            )
        )
    }

    Box(
        modifier = modifier.then(
            if (LocalRetroStyle.current) {
                Modifier.retroPanel(shape)
            } else {
                Modifier
                    .clip(shape)
                    .background(backgroundBrush)
                    .border(borderWidth, rimBrush, shape)
            },
        ),
        contentAlignment = contentAlignment,
        content = content,
    )
}


/**
 * Convenience overload of [LiquidGlassButton] for icon-only buttons.
 * Accepts [iconRes] drawable resource, [size] for the button, [iconSize] for the icon,
 * and an optional [iconTint] to override the default tint.
 */
@Composable
fun LiquidGlassButton(
    onClick: () -> Unit,
    iconRes: Int,
    size: androidx.compose.ui.unit.Dp,
    iconSize: androidx.compose.ui.unit.Dp = size * 0.5f,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isActive: Boolean = false,
    iconTint: Color? = null,
    activeTint: Color = MaterialTheme.colorScheme.primary,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
) {
    LiquidGlassButton(
        onClick = onClick,
        modifier = modifier.size(size),
        enabled = enabled,
        isActive = isActive,
        activeTint = activeTint,
        shape = CircleShape,
        cornerRadius = size / 2,
        interactionSource = interactionSource,
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            tint = iconTint ?: if (isActive) Color.White else Color.White.copy(alpha = 0.85f),
            modifier = Modifier.size(iconSize),
        )
    }
}


/**
 * Contenedor de cristal uniforme (mismo radio en todas las esquinas) para tarjetas y filas.
 * Por defecto refracta el fondo real si lo hay (si no, cae a cristal esmerilado). Con [refractive] = false
 * usa siempre cristal esmerilado, más barato para listas muy largas.
 */
@Composable
fun Modifier.glassCard(
    cornerRadius: Dp = 24.dp,
    refractive: Boolean = true,
): Modifier {
    val shape = RoundedCornerShape(cornerRadius)
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    return if (refractive && LocalLiquidBackdrop.current != null) {
        this
            .liquidGlass(
                cornerRadius = cornerRadius,
                fallbackColor = Color.White.copy(alpha = if (isDark) 0.08f else 0.30f),
                tint = if (isDark) Color.White.copy(alpha = 0.04f) else Color.White.copy(alpha = 0.26f),
            ).clip(shape)
    } else {
        this.frostedGlass(shape)
    }
}

/**
 * Icono de playlist automática en cristal líquido transparente: refracta el fondo de la app
 * (o, sin fondo, cae a cristal esmerilado), con un resplandor suave del color [tint] por dentro,
 * canto de luz y el icono en blanco con halo de color.
 * El radio por defecto es el de las miniaturas, para que encaje con el resto de la cuadrícula.
 */
@Composable
fun GlassPlaylistIcon(
    iconRes: Int,
    tint: Color,
    modifier: Modifier = Modifier,
    iconSize: Dp = 32.dp,
    cornerRadius: Dp = com.metrolist.music.constants.LocalThumbnailCornerRadius.current,
) {
    val shape = RoundedCornerShape(cornerRadius)
    if (LocalRetroStyle.current) {
        Box(modifier = modifier.retroPanel(shape), contentAlignment = Alignment.Center) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                tint = RetroPalette.Phosphor,
                modifier = Modifier.size(iconSize),
            )
        }
        return
    }
    val glass =
        if (LocalLiquidBackdrop.current != null) {
            Modifier.liquidGlass(
                cornerRadius = cornerRadius,
                fallbackColor = Color.White.copy(alpha = 0.10f),
                refraction = 8.dp,
                bezel = 12.dp,
                tint = Color.White.copy(alpha = 0.06f),
            )
        } else {
            Modifier.frostedGlass(shape)
        }
    Box(
        modifier =
            modifier
                .drawWithContent {
                    drawContent()
                    val stroke = 1.dp.toPx()
                    val r = min(cornerRadius.toPx(), min(size.width, size.height) / 2f)
                    drawRoundRect(
                        brush =
                            Brush.verticalGradient(
                                listOf(Color.White.copy(alpha = 0.75f), Color.White.copy(alpha = 0.10f), Color.White.copy(alpha = 0.30f)),
                            ),
                        topLeft = Offset(stroke / 2f, stroke / 2f),
                        size = androidx.compose.ui.geometry.Size(size.width - stroke, size.height - stroke),
                        cornerRadius = CornerRadius(r),
                        style = Stroke(stroke),
                    )
                }.then(glass)
                .clip(shape)
                .drawBehind {
                    // Solo luz: reflejo especular arriba a la izquierda y un destello tenue abajo.
                    drawRect(
                        Brush.radialGradient(
                            0f to Color.White.copy(alpha = 0.34f),
                            1f to Color.Transparent,
                            center = Offset(size.width * 0.2f, size.height * 0.1f),
                            radius = size.maxDimension * 0.65f,
                        ),
                    )
                    drawRect(
                        Brush.radialGradient(
                            0f to Color.White.copy(alpha = 0.10f),
                            1f to Color.Transparent,
                            center = Offset(size.width * 0.85f, size.height * 0.95f),
                            radius = size.maxDimension * 0.6f,
                        ),
                    )
                },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.55f),
            modifier = Modifier.size(iconSize).blur(6.dp),
        )
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.95f),
            modifier = Modifier.size(iconSize),
        )
    }
}

/**
 * Ficha circular de cristal para iconos (ajustes, listas): tinte suave del color, reflejo especular
 * arriba a la izquierda y canto de luz. Sustituye a los cuadrados de color con esquinas sueltas.
 */
@Composable
fun GlassIconTile(
    tint: Color,
    modifier: Modifier = Modifier,
    strong: Boolean = false,
    content: @Composable BoxScope.() -> Unit,
) {
    // Sin contenedor: el icono flota sobre el cristal; solo un halo muy suave si está destacado.
    Box(
        modifier =
            modifier.drawBehind {
                if (strong) {
                    drawCircle(
                        Brush.radialGradient(
                            0f to tint.copy(alpha = 0.22f),
                            1f to Color.Transparent,
                            center = center,
                            radius = size.maxDimension * 0.62f,
                        ),
                    )
                }
            },
        contentAlignment = Alignment.Center,
        content = content,
    )
}

/**
 * Superficie de cristal para contenedores: refracta el fondo real cuando hay [LocalLiquidBackdrop]
 * y, si no, cae al cristal esmerilado con volumen. [tint] colorea el cristal (p. ej. error, seleccionado).
 */
@Composable
fun Modifier.glassSurface(
    shape: Shape,
    tint: Color? = null,
): Modifier {
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    return if (LocalLiquidBackdrop.current != null) {
        this
            .liquidGlass(
                shape = shape,
                fallbackColor = Color.White.copy(alpha = if (isDark) 0.08f else 0.30f),
                tint = tint?.copy(alpha = 0.30f) ?: if (isDark) Color.White.copy(alpha = 0.05f) else Color.White.copy(alpha = 0.28f),
            ).clip(shape)
    } else {
        this.frostedGlass(shape).then(if (tint != null) Modifier.background(tint.copy(alpha = 0.28f)) else Modifier)
    }
}

/** Sustituto de [androidx.compose.material3.Card] en cristal líquido (mismos parámetros; el color lo pone el cristal). */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(24.dp),
    colors: CardColors? = null,
    elevation: CardElevation? = null,
    border: BorderStroke? = null,
    tint: Color? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurface) {
        Column(modifier = modifier.glassSurface(shape, tint), content = content)
    }
}

/** Igual que [GlassCard] pero clicable (sustituye a `Card(onClick = ...)`). */
@Composable
fun GlassCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = RoundedCornerShape(24.dp),
    colors: CardColors? = null,
    elevation: CardElevation? = null,
    border: BorderStroke? = null,
    interactionSource: MutableInteractionSource? = null,
    tint: Color? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val source = interactionSource ?: remember { MutableInteractionSource() }
    CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurface) {
        Column(
            modifier =
                modifier
                    .glassSurface(shape, tint)
                    .clickable(
                        enabled = enabled,
                        interactionSource = source,
                        indication = null,
                        onClick = onClick,
                    ),
            content = content,
        )
    }
}

@Composable
private fun GlassTextButtonImpl(
    onClick: () -> Unit,
    modifier: Modifier,
    enabled: Boolean,
    shape: Shape,
    accent: Boolean,
    tint: Color?,
    contentPadding: PaddingValues,
    interactionSource: MutableInteractionSource?,
    content: @Composable RowScope.() -> Unit,
) {
    val source = interactionSource ?: remember { MutableInteractionSource() }
    val contentColor = if (accent) Color.White else MaterialTheme.colorScheme.onSurface
    LiquidGlassButton(
        onClick = onClick,
        modifier = modifier.defaultMinSize(minHeight = 40.dp).alpha(if (enabled) 1f else 0.4f),
        enabled = enabled,
        shape = shape,
        isActive = accent,
        activeTint = tint ?: MaterialTheme.colorScheme.primary,
        baseTint = if (accent) null else MaterialTheme.colorScheme.onSurface,
        interactionSource = source,
    ) {
        CompositionLocalProvider(LocalContentColor provides contentColor) {
            ProvideTextStyle(MaterialTheme.typography.labelLarge) {
                Row(
                    modifier = Modifier.padding(contentPadding),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
                    content = content,
                )
            }
        }
    }
}

private val GlassButtonPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp)

/** Sustituto de [androidx.compose.material3.Button]: botón de cristal con el color de acento. */
@Composable
fun GlassButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = CircleShape,
    colors: ButtonColors? = null,
    elevation: ButtonElevation? = null,
    border: BorderStroke? = null,
    contentPadding: PaddingValues = GlassButtonPadding,
    interactionSource: MutableInteractionSource? = null,
    tint: Color? = null,
    content: @Composable RowScope.() -> Unit,
) = GlassTextButtonImpl(onClick, modifier, enabled, shape, true, tint, contentPadding, interactionSource, content)

/** Sustituto de [androidx.compose.material3.OutlinedButton] / FilledTonalButton: cristal neutro. */
@Composable
fun GlassOutlinedButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = CircleShape,
    colors: ButtonColors? = null,
    elevation: ButtonElevation? = null,
    border: BorderStroke? = null,
    contentPadding: PaddingValues = GlassButtonPadding,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable RowScope.() -> Unit,
) = GlassTextButtonImpl(onClick, modifier, enabled, shape, false, null, contentPadding, interactionSource, content)

/** Sustituto de [androidx.compose.material3.FilterChip]: chip de cristal líquido (acento cuando está seleccionado). */
@Composable
fun GlassFilterChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    shape: Shape = RoundedCornerShape(16.dp),
    colors: SelectableChipColors? = null,
    elevation: SelectableChipElevation? = null,
    border: BorderStroke? = null,
    interactionSource: MutableInteractionSource? = null,
) {
    val source = interactionSource ?: remember { MutableInteractionSource() }
    val contentColor = if (selected) Color.White else MaterialTheme.colorScheme.onSurface
    LiquidGlassButton(
        onClick = onClick,
        modifier = modifier.defaultMinSize(minHeight = 32.dp).alpha(if (enabled) 1f else 0.4f),
        enabled = enabled,
        shape = shape,
        isActive = selected,
        baseTint = if (selected) null else MaterialTheme.colorScheme.onSurface,
        interactionSource = source,
    ) {
        CompositionLocalProvider(LocalContentColor provides contentColor) {
            ProvideTextStyle(MaterialTheme.typography.labelLarge) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (leadingIcon != null) {
                        leadingIcon()
                        Spacer(Modifier.width(6.dp))
                    }
                    label()
                    if (trailingIcon != null) {
                        Spacer(Modifier.width(6.dp))
                        trailingIcon()
                    }
                }
            }
        }
    }
}
