/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.FloatState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.metrolist.music.LocalListenTogetherManager
import com.metrolist.music.LocalPlayerConnection
import com.metrolist.music.R
import com.metrolist.music.constants.SwipeSensitivityKey
import com.metrolist.music.constants.SwipeThumbnailKey
import com.metrolist.music.playback.audio.AudioLevelMeter
import com.metrolist.music.utils.rememberPreference
import java.util.Random
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.exp
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

val LocalRetroStyle = compositionLocalOf { false }

val LocalRetroAccent = compositionLocalOf { RetroPalette.Orange }

val LocalRetroSheetProgress = compositionLocalOf<() -> Float> { { 1f } }

object RetroPalette {
    val Screen = Color(0xFF040807)
    val Glow = Color(0xFF123A30)
    val Phosphor = Color(0xFF7CFFB2)
    val Plastic = Color(0xFF2A292E)
    val PlasticLight = Color(0xFF4A484F)
    val PlasticDark = Color(0xFF151417)
    val Cream = Color(0xFFEFE4C8)
    val Orange = Color(0xFFE8712B)
    val Amber = Color(0xFFFFB347)
    val Red = Color(0xFFD9432F)
    val Metal = Color(0xFFA7A9B0)
    val Ink = Color(0xFF1D1B2E)
}

@Stable
class RetroAudioState {
    var level by mutableFloatStateOf(0f)
    var bass by mutableFloatStateOf(0f)
    var beat by mutableFloatStateOf(0f)
    var clock by mutableFloatStateOf(0f)
}

@Composable
fun RetroAudioMeterEffect() {
    DisposableEffect(Unit) {
        AudioLevelMeter.enabled = true
        onDispose { AudioLevelMeter.enabled = false }
    }
}

@Composable
fun rememberRetroAudio(active: Boolean): RetroAudioState {
    val state = remember { RetroAudioState() }
    LaunchedEffect(active) {
        val raw = FloatArray(2)
        var average = 0f
        var last = 0L
        var sinceBeat = 0f
        while (true) {
            val settled =
                withFrameNanos { now ->
                    val dt = if (last == 0L) 0.016f else ((now - last) / 1_000_000_000f).coerceIn(0.001f, 0.1f)
                    last = now
                    if (active) {
                        AudioLevelMeter.read(raw, System.nanoTime())
                    } else {
                        raw[0] = 0f
                        raw[1] = 0f
                    }
                    val targetLevel = sqrt((raw[0] * 2.6f).coerceIn(0f, 1f))
                    val targetBass = (raw[1] * 4.2f).coerceIn(0f, 1f)
                    state.level += (targetLevel - state.level) * (if (targetLevel > state.level) 0.6f else 0.16f)
                    state.bass += (targetBass - state.bass) * (if (targetBass > state.bass) 0.7f else 0.18f)
                    average += (targetBass - average) * (dt * 1.4f)
                    sinceBeat += dt
                    if (active && targetBass > average * 1.3f + 0.05f && sinceBeat > 0.14f) {
                        state.beat = 1f
                        sinceBeat = 0f
                    }
                    state.beat = (state.beat - dt * 4.5f).coerceAtLeast(0f)
                    state.clock += dt
                    !active && state.level < 0.004f && state.bass < 0.004f && state.beat == 0f
                }
            if (settled) break
        }
    }
    return state
}

@Composable
fun rememberReelAngle(
    playing: Boolean,
    audio: RetroAudioState,
): FloatState {
    val angle = remember { mutableFloatStateOf(0f) }
    LaunchedEffect(playing) {
        if (!playing) return@LaunchedEffect
        var last = 0L
        while (true) {
            withFrameNanos { now ->
                if (last != 0L) {
                    val dt = ((now - last) / 1_000_000_000f).coerceAtMost(0.1f)
                    angle.floatValue = (angle.floatValue + dt * (110f + 260f * audio.level)) % 360f
                }
                last = now
            }
        }
    }
    return angle
}

@Composable
fun rememberTapeProgress(): FloatState {
    val connection = LocalPlayerConnection.current
    val progress = remember { mutableFloatStateOf(0f) }
    LaunchedEffect(connection) {
        if (connection == null) return@LaunchedEffect
        while (true) {
            progress.floatValue =
                runCatching {
                    val player = connection.player
                    val duration = player.duration
                    if (duration > 0) (player.currentPosition.toFloat() / duration).coerceIn(0f, 1f) else 0f
                }.getOrDefault(progress.floatValue)
            delay(400)
        }
    }
    return progress
}

fun Modifier.retroPanel(
    shape: Shape,
    fill: Color = RetroPalette.Plastic,
): Modifier =
    this
        .clip(shape)
        .background(Brush.verticalGradient(listOf(lerp(fill, Color.White, 0.08f), fill)))
        .border(
            1.5.dp,
            Brush.verticalGradient(listOf(RetroPalette.Metal.copy(alpha = 0.55f), RetroPalette.PlasticDark)),
            shape,
        )

fun Modifier.retroKey(
    shape: Shape,
    pressed: Boolean,
    lit: Boolean = false,
    litColor: Color = RetroPalette.Orange,
    led: Boolean = false,
): Modifier {
    val face =
        when {
            lit -> listOf(lerp(litColor, Color.White, 0.22f), litColor)
            pressed -> listOf(RetroPalette.PlasticDark, RetroPalette.Plastic)
            else -> listOf(RetroPalette.PlasticLight, RetroPalette.Plastic)
        }
    val edge =
        if (pressed) {
            listOf(Color.Black, RetroPalette.Metal.copy(alpha = 0.35f))
        } else {
            listOf(RetroPalette.Metal.copy(alpha = 0.75f), Color.Black)
        }
    return this
        .clip(shape)
        .background(Brush.verticalGradient(face))
        .border(1.5.dp, Brush.verticalGradient(edge), shape)
        .then(
            if (led) {
                Modifier.drawBehind {
                    drawCircle(
                        RetroPalette.Phosphor,
                        radius = 2.5.dp.toPx(),
                        center = Offset(size.width - 9.dp.toPx(), 9.dp.toPx()),
                    )
                }
            } else {
                Modifier
            },
        )
}

fun Modifier.retroDeck(): Modifier {
    val shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp)
    return this
        .clip(shape)
        .background(
            Brush.verticalGradient(listOf(RetroPalette.PlasticLight, RetroPalette.Plastic, RetroPalette.PlasticDark)),
        ).border(1.5.dp, Brush.verticalGradient(listOf(RetroPalette.Metal, Color.Transparent)), shape)
}

@Composable
fun RetroTvBackground(modifier: Modifier = Modifier) {
    Box(
        modifier =
            modifier
                .background(RetroPalette.Screen)
                .drawWithCache {
                    val glow =
                        Brush.radialGradient(
                            colors = listOf(RetroPalette.Glow, RetroPalette.Screen),
                            center = Offset(size.width * 0.5f, size.height * 0.42f),
                            radius = size.maxDimension * 0.7f,
                        )
                    onDrawBehind { drawRect(glow) }
                },
    )
}

private fun createNoiseTile(): ImageBitmap {
    val side = 96
    val bitmap = ImageBitmap(side, side)
    val random = Random(7)
    val pixels = IntArray(side * side)
    for (i in pixels.indices) {
        val v = random.nextInt(256)
        pixels[i] = android.graphics.Color.argb(random.nextInt(60), v, v, v)
    }
    bitmap.asAndroidBitmap().setPixels(pixels, 0, side, 0, 0, side, side)
    return bitmap
}

@Composable
fun RetroCrtOverlay(modifier: Modifier = Modifier) {
    val noise = remember { ShaderBrush(ImageShader(createNoiseTile(), TileMode.Repeated, TileMode.Repeated)) }
    var shift by remember { mutableIntStateOf(0) }
    val band =
        rememberInfiniteTransition(label = "crtBand").animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(6500, easing = LinearEasing)),
            label = "crtBandPhase",
        )
    LaunchedEffect(Unit) {
        val random = Random()
        while (true) {
            delay(80)
            shift = random.nextInt(96 * 96)
        }
    }
    Box(modifier = modifier.fillMaxSize()) {
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .graphicsLayer { translationY = (band.value * 1.5f - 0.25f) * size.height }
                    .drawBehind {
                        drawRect(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, Color.White.copy(alpha = 0.045f), Color.Transparent),
                            ),
                            size = Size(size.width, size.height * 0.16f),
                        )
                    },
        )
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .drawWithCache {
                        val w = size.width
                        val h = size.height
                        val pitch = 3.dp.toPx().coerceAtLeast(3f)
                        val dark = Color.Black.copy(alpha = 0.2f)
                        val scan =
                            Brush.verticalGradient(
                                0f to dark,
                                0.4f to dark,
                                0.4f to Color.Transparent,
                                1f to Color.Transparent,
                                startY = 0f,
                                endY = pitch,
                                tileMode = TileMode.Repeated,
                            )
                        val vignette =
                            Brush.radialGradient(
                                0f to Color.Transparent,
                                0.6f to Color.Transparent,
                                1f to Color.Black.copy(alpha = 0.62f),
                                center = Offset(w / 2f, h / 2f),
                                radius = hypot(w, h) / 2f,
                            )
                        val glare =
                            Brush.linearGradient(
                                listOf(Color.White.copy(alpha = 0.07f), Color.Transparent),
                                start = Offset.Zero,
                                end = Offset(w * 0.7f, h * 0.45f),
                            )
                        val bezel =
                            Path().apply {
                                fillType = PathFillType.EvenOdd
                                addRect(Rect(0f, 0f, w, h))
                                addRoundRect(RoundRect(0f, 0f, w, h, CornerRadius(24.dp.toPx())))
                            }
                        onDrawBehind {
                            drawRect(scan)
                            drawRect(glare)
                            drawRect(vignette)
                            drawPath(bezel, Color.Black)
                        }
                    },
        )
        Canvas(modifier = Modifier.fillMaxSize().graphicsLayer {}) {
            val s = shift
            translate((s % 96) - 96f, (s / 96) - 96f) {
                drawRect(noise, size = Size(size.width + 96f, size.height + 96f), alpha = 0.55f)
            }
        }
    }
}

@Composable
private fun RetroTransportKey(
    iconRes: Int,
    lit: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val accent = LocalRetroAccent.current
    Box(
        modifier =
            Modifier
                .size(44.dp)
                .retroKey(RoundedCornerShape(12.dp), pressed, lit, accent)
                .clickable(interactionSource = source, indication = null, enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            tint = if (enabled) RetroPalette.Cream else RetroPalette.Cream.copy(alpha = 0.35f),
            modifier = Modifier.size(22.dp),
        )
    }
}

@Composable
fun RetroMiniPlayer(modifier: Modifier = Modifier) {
    val playerConnection = LocalPlayerConnection.current ?: return
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()
    val isPlaying by playerConnection.isPlaying.collectAsState()
    val canSkipNext by playerConnection.canSkipNext.collectAsState()
    val audio = rememberRetroAudio(isPlaying)
    val angle = rememberReelAngle(isPlaying, audio)
    val tape = rememberTapeProgress()
    val swipeSensitivity by rememberPreference(SwipeSensitivityKey, 0.73f)
    val swipeThumbnailPref by rememberPreference(SwipeThumbnailKey, true)
    val listenTogetherManager = LocalListenTogetherManager.current
    val isListenTogetherGuest = listenTogetherManager?.let { it.isInRoom && !it.isHost } ?: false
    val swipeEnabled = swipeThumbnailPref && !isListenTogetherGuest
    val layoutDirection = LocalLayoutDirection.current
    val scope = rememberCoroutineScope()
    val offsetX = remember { Animatable(0f) }
    val settle = remember { spring<Float>(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessLow) }
    val autoThreshold =
        remember(swipeSensitivity) {
            (600 / (1f + exp(-(-11.44748 * swipeSensitivity + 9.04945)))).roundToInt()
        }

    BoxWithConstraints(
        modifier =
            modifier
                .fillMaxSize()
                .clipToBounds()
                .pointerInput(swipeEnabled, swipeSensitivity) {
                    if (!swipeEnabled) return@pointerInput
                    var startTime = 0L
                    var travelled = 0f
                    detectHorizontalDragGestures(
                        onDragStart = {
                            startTime = System.currentTimeMillis()
                            travelled = 0f
                        },
                        onDragCancel = { scope.launch { offsetX.animateTo(0f, settle) } },
                        onHorizontalDrag = { _, amount ->
                            val drag = if (layoutDirection == LayoutDirection.Rtl) -amount else amount
                            val player = playerConnection.player
                            val canPrevious = player.previousMediaItemIndex != -1
                            val canNext = player.nextMediaItemIndex != -1
                            val toRight = drag > 0
                            val toLeft = drag < 0
                            val comingBack =
                                (toRight && !canPrevious && offsetX.value < 0) || (toLeft && !canNext && offsetX.value > 0)
                            if ((toLeft && canNext) || (toRight && canPrevious) || comingBack) {
                                travelled += abs(drag)
                                scope.launch { offsetX.snapTo(offsetX.value + drag) }
                            }
                        },
                        onDragEnd = {
                            val duration = System.currentTimeMillis() - startTime
                            val velocity = if (duration > 0) travelled / duration else 0f
                            val current = offsetX.value
                            val changeSong =
                                (abs(current) > 50f && velocity > (swipeSensitivity * -8.25f) + 8.5f) ||
                                    abs(current) > autoThreshold
                            if (changeSong) {
                                val player = playerConnection.player
                                if (current > 0 && player.previousMediaItemIndex != -1) {
                                    player.seekToPreviousMediaItem()
                                } else if (current <= 0 && player.nextMediaItemIndex != -1) {
                                    player.seekToNext()
                                }
                            }
                            scope.launch { offsetX.animateTo(0f, settle) }
                        },
                    )
                },
    ) {
        val keyColumn = 44.dp
        val cassetteWidth = minOf(maxWidth - 32.dp - keyColumn - 8.dp, 300.dp)
        Row(
            modifier =
                Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 8.dp)
                    .offset { IntOffset(offsetX.value.roundToInt(), 0) },
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Top,
        ) {
            RetroCassette(
                width = cassetteWidth,
                title = mediaMetadata?.title.orEmpty(),
                artist = mediaMetadata?.artists?.joinToString { it.name }.orEmpty(),
                tape = { tape.floatValue },
                reelAngle = { angle.floatValue },
            )
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                RetroTransportKey(
                    iconRes = if (isPlaying) R.drawable.pause else R.drawable.play,
                    lit = isPlaying,
                    enabled = true,
                    onClick = playerConnection::togglePlayPause,
                )
                RetroTransportKey(
                    iconRes = R.drawable.skip_next,
                    lit = false,
                    enabled = canSkipNext,
                    onClick = playerConnection::seekToNext,
                )
            }
        }
    }
}

@Composable
fun RetroCassette(
    width: Dp,
    title: String,
    artist: String,
    tape: () -> Float,
    reelAngle: () -> Float,
    modifier: Modifier = Modifier,
) {
    val height = width / 1.65f
    val density = LocalDensity.current
    val titleSize = with(density) { (width * 0.062f).toSp() }
    val artistSize = with(density) { (width * 0.047f).toSp() }
    val accent = animateColorAsState(LocalRetroAccent.current, tween(600), label = "retroAccent")
    Box(modifier = modifier.size(width, height)) {
        Canvas(modifier = Modifier.fillMaxSize()) { drawCassette(tape(), reelAngle(), accent.value) }
        Column(modifier = Modifier.offset(x = width * 0.10f, y = height * 0.15f).width(width * 0.80f)) {
            Text(
                text = title,
                fontFamily = FontFamily.Cursive,
                fontWeight = FontWeight.Bold,
                fontSize = titleSize,
                color = RetroPalette.Ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = artist,
                fontFamily = FontFamily.Cursive,
                fontSize = artistSize,
                color = RetroPalette.Ink.copy(alpha = 0.75f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

private fun DrawScope.drawCassette(
    tape: Float,
    angle: Float,
    accent: Color,
) {
    val w = size.width
    val h = size.height
    val corner = CornerRadius(w * 0.045f)
    drawRoundRect(
        brush = Brush.verticalGradient(listOf(Color(0xFF3B393F), Color(0xFF1C1B20))),
        cornerRadius = corner,
    )
    drawRoundRect(
        color = RetroPalette.Metal.copy(alpha = 0.4f),
        cornerRadius = corner,
        style = Stroke(w * 0.005f),
    )
    val label =
        Path().apply {
            addRoundRect(RoundRect(w * 0.06f, h * 0.07f, w * 0.94f, h * 0.64f, CornerRadius(w * 0.02f)))
        }
    clipPath(label) {
        drawRect(RetroPalette.Cream)
        drawRect(accent, Offset(0f, h * 0.07f), Size(w, h * 0.065f))
        drawRect(lerp(accent, Color.Black, 0.35f), Offset(0f, h * 0.135f), Size(w, h * 0.012f))
        val ink = RetroPalette.Ink.copy(alpha = 0.22f)
        drawLine(ink, Offset(w * 0.10f, h * 0.28f), Offset(w * 0.90f, h * 0.28f), w * 0.003f)
        drawLine(ink, Offset(w * 0.10f, h * 0.38f), Offset(w * 0.90f, h * 0.38f), w * 0.003f)
    }
    val windowLeft = w * 0.20f
    val windowTop = h * 0.40f
    val windowWidth = w * 0.60f
    val windowHeight = h * 0.34f
    val windowCorner = CornerRadius(windowHeight * 0.45f)
    drawRoundRect(Color(0xFF0D0C10), Offset(windowLeft, windowTop), Size(windowWidth, windowHeight), windowCorner)
    drawRoundRect(
        RetroPalette.Metal.copy(alpha = 0.45f),
        Offset(windowLeft, windowTop),
        Size(windowWidth, windowHeight),
        windowCorner,
        Stroke(w * 0.005f),
    )
    val cy = windowTop + windowHeight / 2f
    val hub = h * 0.075f
    val packMax = windowHeight * 0.47f
    val packMin = hub * 1.12f
    val leftPack = packMin + (packMax - packMin) * (1f - tape)
    val rightPack = packMin + (packMax - packMin) * tape
    val leftX = w * 0.34f
    val rightX = w * 0.66f
    drawLine(Color(0xFF2B1D17), Offset(leftX, cy + leftPack), Offset(rightX, cy + rightPack), w * 0.008f)
    drawReel(Offset(leftX, cy), leftPack, hub, angle)
    drawReel(Offset(rightX, cy), rightPack, hub, angle)
    val plate =
        Path().apply {
            moveTo(w * 0.15f, h * 0.78f)
            lineTo(w * 0.85f, h * 0.78f)
            lineTo(w * 0.91f, h * 0.97f)
            lineTo(w * 0.09f, h * 0.97f)
            close()
        }
    drawPath(plate, Color(0xFF121115))
    drawCircle(Color(0xFF3B393F), w * 0.018f, Offset(w * 0.28f, h * 0.88f))
    drawCircle(Color(0xFF3B393F), w * 0.018f, Offset(w * 0.72f, h * 0.88f))
    drawRect(Color(0xFF3B393F), Offset(w * 0.44f, h * 0.84f), Size(w * 0.04f, h * 0.08f))
    drawRect(Color(0xFF3B393F), Offset(w * 0.52f, h * 0.84f), Size(w * 0.04f, h * 0.08f))
    val screw = Color(0xFF8A8C93)
    drawCircle(screw, w * 0.012f, Offset(w * 0.03f, h * 0.085f))
    drawCircle(screw, w * 0.012f, Offset(w * 0.97f, h * 0.085f))
    drawCircle(screw, w * 0.012f, Offset(w * 0.03f, h * 0.93f))
    drawCircle(screw, w * 0.012f, Offset(w * 0.97f, h * 0.93f))
}

private fun DrawScope.drawReel(
    center: Offset,
    pack: Float,
    hub: Float,
    angle: Float,
) {
    val white = Color(0xFFE9E9EC)
    drawCircle(Color(0xFF2B1D17), pack, center)
    drawCircle(Color.White.copy(alpha = 0.07f), pack * 0.92f, center, style = Stroke(hub * 0.12f))
    drawCircle(white, hub, center)
    drawCircle(Color(0xFF16151A), hub * 0.62f, center)
    repeat(6) { i ->
        rotate(angle + i * 60f, center) {
            drawRect(white, Offset(center.x + hub * 0.34f, center.y - hub * 0.08f), Size(hub * 0.3f, hub * 0.16f))
        }
    }
}

@Composable
fun RetroBoombox(modifier: Modifier = Modifier) {
    val playerConnection = LocalPlayerConnection.current ?: return
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()
    val isPlaying by playerConnection.isPlaying.collectAsState()
    val audio = rememberRetroAudio(isPlaying)
    val angle = rememberReelAngle(isPlaying, audio)
    val tape = rememberTapeProgress()
    val sheetProgress = LocalRetroSheetProgress.current

    BoxWithConstraints(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        val bodyWidth = minOf(maxWidth - 24.dp, maxHeight / 0.64f, 460.dp).coerceAtLeast(180.dp)
        val handleHeight = bodyWidth * 0.06f
        val bodyHeight = bodyWidth * 0.56f
        val windowWidth = bodyWidth * 0.38f
        val windowHeight = bodyWidth * 0.27f
        Box(
            modifier =
                Modifier
                    .size(bodyWidth, handleHeight + bodyHeight)
                    .graphicsLayer {
                        val pulse = 1f + 0.012f * audio.beat
                        scaleX = pulse
                        scaleY = pulse
                    },
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) { drawBoomboxBody(audio, handleHeight.toPx()) }
            Box(
                modifier =
                    Modifier
                        .offset(x = bodyWidth * 0.31f, y = handleHeight + bodyWidth * 0.14f)
                        .size(windowWidth, windowHeight)
                        .clip(RoundedCornerShape(bodyWidth * 0.015f)),
                contentAlignment = Alignment.Center,
            ) {
                RetroCassette(
                    width = windowWidth * 0.96f,
                    title = mediaMetadata?.title.orEmpty(),
                    artist = mediaMetadata?.artists?.joinToString { it.name }.orEmpty(),
                    tape = { tape.floatValue },
                    reelAngle = { angle.floatValue },
                    modifier =
                        Modifier.graphicsLayer {
                            val t = ((sheetProgress() - 0.3f) / 0.7f).coerceIn(0f, 1f)
                            translationY = (1f - t * t * (3f - 2f * t)) * size.height * 1.2f
                        },
                )
            }
            Canvas(modifier = Modifier.fillMaxSize()) { drawBoomboxFront(handleHeight.toPx()) }
        }
    }
}

private fun DrawScope.drawBoomboxBody(
    audio: RetroAudioState,
    handleHeight: Float,
) {
    val w = size.width
    val h = size.height
    val bodyHeight = h - handleHeight
    drawRoundRect(
        color = Color(0xFF101013),
        topLeft = Offset(w * 0.20f, w * 0.012f),
        size = Size(w * 0.60f, handleHeight * 1.8f),
        cornerRadius = CornerRadius(handleHeight),
        style = Stroke(w * 0.022f),
    )
    drawRoundRect(
        brush = Brush.verticalGradient(listOf(Color(0xFFB9BBC1), Color(0xFF6B6D74)), startY = handleHeight, endY = h),
        topLeft = Offset(0f, handleHeight),
        size = Size(w, bodyHeight),
        cornerRadius = CornerRadius(w * 0.05f),
    )
    drawRoundRect(
        color = Color(0xFF25252A),
        topLeft = Offset(0f, handleHeight),
        size = Size(w, bodyHeight),
        cornerRadius = CornerRadius(w * 0.05f),
        style = Stroke(w * 0.008f),
    )
    val panelWidth = w * 0.24f
    val panelTop = handleHeight + w * 0.035f
    val panelHeight = bodyHeight - w * 0.07f
    listOf(w * 0.03f, w * 0.73f).forEach { left ->
        drawRoundRect(Color(0xFF0C0C0F), Offset(left, panelTop), Size(panelWidth, panelHeight), CornerRadius(w * 0.03f))
        val cx = left + panelWidth / 2f
        val wooferY = panelTop + panelHeight * 0.62f
        val wooferCenter = Offset(cx, wooferY)
        val radius = panelWidth * 0.43f * (1f + 0.05f * audio.bass + 0.04f * audio.beat)
        drawCircle(Color(0xFF1B1B20), radius * 1.12f, wooferCenter)
        drawCircle(
            brush = Brush.radialGradient(listOf(Color(0xFF4A4A52), Color(0xFF111114)), center = wooferCenter, radius = radius),
            radius = radius,
            center = wooferCenter,
        )
        drawCircle(Color(0xFF6A6A73).copy(alpha = 0.5f), radius * 0.98f, wooferCenter, style = Stroke(w * 0.006f))
        drawCircle(Color(0xFF2A2A30), radius * 0.34f * (1f + 0.15f * audio.bass), wooferCenter)
        if (audio.beat > 0f) {
            drawCircle(
                RetroPalette.Amber.copy(alpha = 0.55f * audio.beat),
                radius * 1.14f,
                wooferCenter,
                style = Stroke(w * 0.01f),
            )
        }
        val tweeterCenter = Offset(cx, panelTop + panelHeight * 0.2f)
        val tweeterRadius = panelWidth * 0.17f * (1f + 0.1f * audio.level)
        drawCircle(Color(0xFF1B1B20), tweeterRadius * 1.2f, tweeterCenter)
        drawCircle(
            brush =
                Brush.radialGradient(
                    listOf(Color(0xFF5A5A63), Color(0xFF131316)),
                    center = tweeterCenter,
                    radius = tweeterRadius,
                ),
            radius = tweeterRadius,
            center = tweeterCenter,
        )
    }
    val centerLeft = w * 0.30f
    val centerWidth = w * 0.40f
    val displayTop = handleHeight + w * 0.035f
    val displayHeight = w * 0.08f
    drawRoundRect(Color(0xFF07110D), Offset(centerLeft, displayTop), Size(centerWidth, displayHeight), CornerRadius(w * 0.012f))
    val bars = 14
    val gap = centerWidth * 0.02f
    val barWidth = (centerWidth - gap * (bars + 1)) / bars
    val segment = displayHeight * 0.14f
    repeat(bars) { i ->
        val mix = i / (bars - 1f)
        val value =
            ((audio.bass + (audio.level - audio.bass) * mix) * (0.7f + 0.3f * sin(audio.clock * 7f + i * 1.7f)))
                .coerceIn(0f, 1f)
        val lit = ceil(value * 5f).toInt()
        repeat(5) { s ->
            val color =
                when {
                    s >= 4 -> RetroPalette.Red
                    s == 3 -> RetroPalette.Amber
                    else -> RetroPalette.Phosphor
                }
            drawRect(
                color.copy(alpha = if (s < lit) 1f else 0.12f),
                Offset(
                    centerLeft + gap + i * (barWidth + gap),
                    displayTop + displayHeight - displayHeight * 0.1f - (s + 1) * segment * 1.18f,
                ),
                Size(barWidth, segment),
            )
        }
    }
    drawRoundRect(
        Color(0xFF050507),
        Offset(w * 0.31f, handleHeight + w * 0.14f),
        Size(w * 0.38f, w * 0.27f),
        CornerRadius(w * 0.015f),
    )
    val stripTop = handleHeight + w * 0.43f
    val keyWidth = centerWidth / 5f
    repeat(5) { i ->
        drawRoundRect(
            brush =
                Brush.verticalGradient(
                    listOf(Color(0xFF55555C), Color(0xFF26262B)),
                    startY = stripTop,
                    endY = stripTop + w * 0.08f,
                ),
            topLeft = Offset(centerLeft + i * keyWidth + keyWidth * 0.08f, stripTop),
            size = Size(keyWidth * 0.84f, w * 0.08f),
            cornerRadius = CornerRadius(w * 0.008f),
        )
    }
}

private fun DrawScope.drawBoomboxFront(handleHeight: Float) {
    val w = size.width
    val left = w * 0.31f
    val top = handleHeight + w * 0.14f
    val windowWidth = w * 0.38f
    val windowHeight = w * 0.27f
    drawRect(
        Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.55f), Color.Transparent), startY = top, endY = top + windowHeight * 0.18f),
        Offset(left, top),
        Size(windowWidth, windowHeight * 0.18f),
    )
    val glare =
        Path().apply {
            moveTo(left + windowWidth * 0.08f, top)
            lineTo(left + windowWidth * 0.42f, top)
            lineTo(left + windowWidth * 0.14f, top + windowHeight)
            lineTo(left, top + windowHeight)
            close()
        }
    drawPath(glare, Color.White.copy(alpha = 0.08f))
    drawRoundRect(
        RetroPalette.Metal.copy(alpha = 0.8f),
        Offset(left, top),
        Size(windowWidth, windowHeight),
        CornerRadius(w * 0.015f),
        Stroke(w * 0.008f),
    )
}
