@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.metrolist.music.ui.component

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.Morph

private const val CycleMillis = 4550
private const val ActiveFraction = 0.8f

private object LoadingShapes {
    private val polygons = listOf(
        MaterialShapes.SoftBurst,
        MaterialShapes.Cookie9Sided,
        MaterialShapes.Pentagon,
        MaterialShapes.Pill,
        MaterialShapes.Sunny,
        MaterialShapes.Cookie4Sided,
        MaterialShapes.Oval,
    )

    val morphs = polygons.mapIndexed { index, polygon ->
        Morph(polygon, polygons[(index + 1) % polygons.size])
    }

    val centerX = polygons.map { it.centerX }.average().toFloat()
    val centerY = polygons.map { it.centerY }.average().toFloat()

    val extent = polygons.maxOf {
        val bounds = it.calculateBounds()
        maxOf(bounds[2] - bounds[0], bounds[3] - bounds[1])
    }
}

@Composable
fun MorphLoadingIndicator(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    size: Dp = 36.dp,
) {
    val phase = rememberInfiniteTransition(label = "morphLoading").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = CycleMillis, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "morphLoadingPhase",
    )
    val path = remember { Path() }

    Canvas(
        modifier = modifier
            .size(size)
            .graphicsLayer { rotationZ = phase.value * 360f },
    ) {
        drawMorph(phase.value, path, color)
    }
}

@Composable
fun ContainedMorphLoadingIndicator(
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.primaryContainer,
    indicatorColor: Color = MaterialTheme.colorScheme.onPrimaryContainer,
    size: Dp = 40.dp,
) {
    Box(
        modifier = modifier
            .size(size)
            .background(containerColor, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        MorphLoadingIndicator(color = indicatorColor, size = size)
    }
}

private fun DrawScope.drawMorph(phase: Float, path: Path, color: Color) {
    val morphs = LoadingShapes.morphs
    val position = phase * morphs.size
    val index = position.toInt().coerceAtMost(morphs.lastIndex)
    val local = ((position - index - 0.15f) / 0.7f).coerceIn(0f, 1f)
    val cubics = morphs[index].asCubics(FastOutSlowInEasing.transform(local))

    path.rewind()
    val first = cubics.first()
    path.moveTo(first.anchor0X, first.anchor0Y)
    for (cubic in cubics) {
        path.cubicTo(
            cubic.control0X, cubic.control0Y,
            cubic.control1X, cubic.control1Y,
            cubic.anchor1X, cubic.anchor1Y,
        )
    }
    path.close()

    val factor = size.minDimension * ActiveFraction / LoadingShapes.extent
    withTransform({
        translate(size.width / 2f, size.height / 2f)
        scale(factor, factor, Offset.Zero)
        translate(-LoadingShapes.centerX, -LoadingShapes.centerY)
    }) {
        drawPath(path, color)
    }
}
