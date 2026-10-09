package com.metrolist.music.ui.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

object StartupGate {
    private val ready = MutableStateFlow(false)
    val isReady: StateFlow<Boolean> = ready

    @Volatile
    var dismissed = false

    internal val variant = Random.nextInt(3)

    fun markReady() {
        ready.value = true
    }
}

private const val MIN_VISIBLE_MS = 1600L
private const val MAX_VISIBLE_MS = 7000L

private val Ink = Color(0xFFF2F2F2)
private val InkClear = Ink.copy(alpha = 0f)

@Composable
fun LaunchAnimation(modifier: Modifier = Modifier) {
    var gone by remember { mutableStateOf(StartupGate.dismissed) }
    if (gone) return

    val ready by StartupGate.isReady.collectAsState()
    var minElapsed by remember { mutableStateOf(false) }
    val enter = remember { Animatable(0f) }
    val exit = remember { Animatable(0f) }
    val dismiss = ready && minElapsed

    LaunchedEffect(Unit) { enter.animateTo(1f, tween(1000, easing = FastOutSlowInEasing)) }
    LaunchedEffect(Unit) {
        delay(MIN_VISIBLE_MS)
        minElapsed = true
    }
    LaunchedEffect(Unit) {
        delay(MAX_VISIBLE_MS)
        StartupGate.markReady()
    }
    LaunchedEffect(dismiss) {
        if (dismiss) {
            exit.animateTo(1f, tween(900, easing = CubicBezierEasing(0.4f, 0f, 0.2f, 1f)))
            StartupGate.dismissed = true
            gone = true
        }
    }

    val transition = rememberInfiniteTransition(label = "launchLoader")
    val spin by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(4800, easing = LinearEasing)),
        label = "launchSpin",
    )
    val breath by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(3200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "launchBreath",
    )

    Box(
        modifier =
            modifier
                .fillMaxSize()
                .graphicsLayer { alpha = 1f - exit.value }
                .background(Color.Black)
                .pointerInput(Unit) {},
        contentAlignment = Alignment.Center,
    ) {
        Canvas(
            modifier =
                Modifier
                    .size(240.dp)
                    .graphicsLayer {
                        val e = enter.value
                        val x = exit.value
                        val s = (0.9f + 0.1f * e) * (1f + 0.14f * x)
                        scaleX = s
                        scaleY = s
                        alpha = e * (1f - (x * 2.2f).coerceAtMost(1f))
                    },
        ) {
            translate(top = (breath - 0.5f) * 6.dp.toPx()) {
                when (StartupGate.variant) {
                    0 -> drawVinyl(spin, enter.value)
                    1 -> drawCd(spin, enter.value)
                    else -> drawTape(spin, breath)
                }
            }
        }
    }
}

private fun DrawScope.drawVinyl(spin: Float, enter: Float) {
    val c = center
    val r = size.minDimension * 0.40f
    drawCircle(Color(0xFF0C0C0E), r, c)
    for (i in 0 until 10) {
        val reveal = (enter * 1.7f - i * 0.07f).coerceIn(0f, 1f)
        drawCircle(
            Ink.copy(alpha = (0.05f + (i % 3) * 0.02f) * reveal),
            r * (0.44f + i * 0.052f),
            c,
            style = Stroke(0.6.dp.toPx()),
        )
    }
    drawCircle(Ink.copy(alpha = 0.6f), r, c, style = Stroke(1.2.dp.toPx()))
    drawCircle(
        brush =
            Brush.sweepGradient(
                0f to InkClear,
                0.07f to Ink.copy(alpha = 0.16f),
                0.14f to InkClear,
                0.5f to InkClear,
                0.57f to Ink.copy(alpha = 0.16f),
                0.64f to InkClear,
                1f to InkClear,
                center = c,
            ),
        radius = r,
        center = c,
    )
    rotate(spin, c) {
        val lr = r * 0.34f
        drawCircle(Color(0xFF17171A), lr, c)
        drawCircle(Ink.copy(alpha = 0.55f), lr, c, style = Stroke(1.dp.toPx()))
        drawCircle(Ink.copy(alpha = 0.22f), lr * 0.72f, c, style = Stroke(0.6.dp.toPx()))
        drawCircle(Ink.copy(alpha = 0.85f), 2.2.dp.toPx(), Offset(c.x + lr * 0.5f, c.y))
        drawLine(
            Ink.copy(alpha = 0.35f),
            Offset(c.x - lr * 0.55f, c.y),
            Offset(c.x - lr * 0.2f, c.y),
            strokeWidth = 1.dp.toPx(),
            cap = StrokeCap.Round,
        )
    }
    drawCircle(Color.Black, r * 0.04f, c)
    drawCircle(Ink.copy(alpha = 0.7f), r * 0.04f, c, style = Stroke(0.8.dp.toPx()))
}

private fun DrawScope.drawCd(spin: Float, enter: Float) {
    val c = center
    val r = size.minDimension * 0.40f
    rotate(spin * 0.7f, c) {
        drawCircle(
            brush =
                Brush.sweepGradient(
                    colors =
                        listOf(
                            Color(0xFF7FDBFF).copy(alpha = 0.5f),
                            Color(0xFFB794FF).copy(alpha = 0.5f),
                            Color(0xFFFF8FC7).copy(alpha = 0.45f),
                            Color(0xFFFFE08F).copy(alpha = 0.4f),
                            Color(0xFF8FFFC9).copy(alpha = 0.45f),
                            Color(0xFF7FDBFF).copy(alpha = 0.5f),
                        ),
                    center = c,
                ),
            radius = r,
            center = c,
        )
    }
    drawCircle(
        brush =
            Brush.radialGradient(
                0f to Color.Black.copy(alpha = 0f),
                0.7f to Color.Black.copy(alpha = 0.12f),
                1f to Color.Black.copy(alpha = 0.4f),
                center = c,
                radius = r,
            ),
        radius = r,
        center = c,
    )
    for (i in 0 until 3) {
        val reveal = (enter * 1.6f - i * 0.12f).coerceIn(0f, 1f)
        drawCircle(Ink.copy(alpha = 0.08f * reveal), r * (0.5f + i * 0.17f), c, style = Stroke(0.6.dp.toPx()))
    }
    drawCircle(
        brush =
            Brush.sweepGradient(
                0f to InkClear,
                0.08f to Ink.copy(alpha = 0.22f),
                0.16f to InkClear,
                0.5f to InkClear,
                0.58f to Ink.copy(alpha = 0.22f),
                0.66f to InkClear,
                1f to InkClear,
                center = c,
            ),
        radius = r,
        center = c,
    )
    drawCircle(Color.Black.copy(alpha = 0.85f), r * 0.3f, c)
    drawCircle(Ink.copy(alpha = 0.45f), r * 0.3f, c, style = Stroke(1.dp.toPx()))
    drawCircle(Ink.copy(alpha = 0.2f), r * 0.2f, c, style = Stroke(0.6.dp.toPx()))
    drawCircle(Color.Black, r * 0.085f, c)
    drawCircle(Ink.copy(alpha = 0.7f), r * 0.085f, c, style = Stroke(0.8.dp.toPx()))
    drawCircle(Ink.copy(alpha = 0.6f), r, c, style = Stroke(1.2.dp.toPx()))
}

private fun DrawScope.drawTape(spin: Float, progress: Float) {
    val w = size.width * 0.9f
    val h = w * 0.64f
    val x0 = (size.width - w) / 2f
    val y0 = (size.height - h) / 2f
    val hair = 1.dp.toPx()
    fun px(f: Float) = x0 + w * f
    fun py(f: Float) = y0 + h * f

    drawRoundRect(Ink.copy(alpha = 0.85f), Offset(x0, y0), Size(w, h), CornerRadius(h * 0.07f), style = Stroke(1.4.dp.toPx()))
    drawRoundRect(
        Ink.copy(alpha = 0.45f),
        Offset(px(0.07f), py(0.09f)),
        Size(w * 0.86f, h * 0.5f),
        CornerRadius(h * 0.04f),
        style = Stroke(hair),
    )
    drawLine(Ink.copy(alpha = 0.3f), Offset(px(0.11f), py(0.14f)), Offset(px(0.52f), py(0.14f)), strokeWidth = hair)
    drawLine(Ink.copy(alpha = 0.18f), Offset(px(0.11f), py(0.19f)), Offset(px(0.36f), py(0.19f)), strokeWidth = hair)

    val left = Offset(px(0.37f), py(0.395f))
    val right = Offset(px(0.63f), py(0.395f))
    val rLeft = h * (0.125f - 0.05f * progress)
    val rRight = h * (0.075f + 0.05f * progress)
    drawCircle(Ink.copy(alpha = 0.1f), rLeft, left)
    drawCircle(Ink.copy(alpha = 0.5f), rLeft, left, style = Stroke(hair))
    drawCircle(Ink.copy(alpha = 0.1f), rRight, right)
    drawCircle(Ink.copy(alpha = 0.5f), rRight, right, style = Stroke(hair))
    drawLine(
        Ink.copy(alpha = 0.55f),
        Offset(left.x, left.y + rLeft),
        Offset(right.x, right.y + rRight),
        strokeWidth = hair,
    )
    val hubR = h * 0.06f
    val spokeIn = h * 0.02f
    for (hub in listOf(left, right)) {
        rotate(spin, hub) {
            drawCircle(Ink.copy(alpha = 0.85f), hubR, hub, style = Stroke(hair))
            for (k in 0 until 6) {
                val a = k * (PI.toFloat() / 3f)
                drawLine(
                    Ink.copy(alpha = 0.85f),
                    Offset(hub.x + cos(a) * spokeIn, hub.y + sin(a) * spokeIn),
                    Offset(hub.x + cos(a) * hubR, hub.y + sin(a) * hubR),
                    strokeWidth = hair,
                )
            }
        }
    }
    drawRoundRect(
        Ink.copy(alpha = 0.8f),
        Offset(px(0.24f), py(0.25f)),
        Size(w * 0.52f, h * 0.29f),
        CornerRadius(h * 0.05f),
        style = Stroke(hair),
    )

    val base =
        Path().apply {
            moveTo(px(0.2f), py(0.66f))
            lineTo(px(0.8f), py(0.66f))
            lineTo(px(0.74f), py(0.94f))
            lineTo(px(0.26f), py(0.94f))
            close()
        }
    drawPath(base, Ink.copy(alpha = 0.6f), style = Stroke(hair))
    drawCircle(Ink.copy(alpha = 0.6f), h * 0.028f, Offset(px(0.34f), py(0.8f)), style = Stroke(hair))
    drawCircle(Ink.copy(alpha = 0.6f), h * 0.028f, Offset(px(0.66f), py(0.8f)), style = Stroke(hair))
    drawRoundRect(
        Ink.copy(alpha = 0.45f),
        Offset(px(0.44f), py(0.74f)),
        Size(w * 0.12f, h * 0.1f),
        CornerRadius(2.dp.toPx()),
        style = Stroke(hair),
    )
    for (corner in listOf(Offset(0.055f, 0.07f), Offset(0.945f, 0.07f), Offset(0.055f, 0.93f), Offset(0.945f, 0.93f))) {
        drawCircle(Ink.copy(alpha = 0.5f), 1.5.dp.toPx(), Offset(px(corner.x), py(corner.y)))
    }
}
