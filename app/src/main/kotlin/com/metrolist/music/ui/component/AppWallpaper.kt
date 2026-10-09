/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.component

import android.os.Build
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import java.io.File
import android.view.TextureView
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.delay

private val VIDEO_EXTENSIONS = setOf("mp4", "webm", "mkv", "3gp", "mov")

private fun isContentUri(source: String): Boolean = source.startsWith("content://")

/** Archivo de la app (ruta) o foto/vídeo de la galería del móvil (content://). */
fun isVideoWallpaper(path: String): Boolean =
    if (isContentUri(path)) {
        path.contains("/video/")
    } else {
        File(path).extension.lowercase() in VIDEO_EXTENSIONS
    }

/** Comprueba que el fondo sigue existiendo. Los content:// se validan al cargarlos. */
fun wallpaperSourceExists(source: String): Boolean = isContentUri(source) || File(source).exists()

private fun wallpaperUri(source: String): android.net.Uri =
    if (isContentUri(source)) android.net.Uri.parse(source) else android.net.Uri.fromFile(File(source))

/** Vídeo en bucle, sin sonido, recortado a pantalla completa (TextureView para que el cristal pueda refractarlo). */
@Composable
private fun LoopingVideoWallpaper(source: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var videoSize by remember(source) { mutableStateOf(VideoSize.UNKNOWN) }
    val textureView = remember { TextureView(context) }
    val player =
        remember(source) {
            ExoPlayer.Builder(context).build().apply {
                repeatMode = Player.REPEAT_MODE_ALL
                volume = 0f
                setVideoTextureView(textureView)
                setMediaItem(MediaItem.fromUri(wallpaperUri(source)))
                addListener(object : Player.Listener {
                    override fun onVideoSizeChanged(size: VideoSize) { videoSize = size }
                })
                playWhenReady = true
                prepare()
            }
        }
    DisposableEffect(player, lifecycleOwner) {
        val observer =
            LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_START -> player.play()
                    Lifecycle.Event.ON_STOP -> player.pause()
                    else -> Unit
                }
            }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            player.release()
        }
    }
    BoxWithConstraints(modifier = modifier.fillMaxSize().clipToBounds(), contentAlignment = androidx.compose.ui.Alignment.Center) {
        val density = LocalDensity.current
        val cw = with(density) { maxWidth.toPx() }
        val ch = with(density) { maxHeight.toPx() }
        val vw = videoSize.width.toFloat().coerceAtLeast(1f) * (if (videoSize.pixelWidthHeightRatio > 0f) videoSize.pixelWidthHeightRatio else 1f)
        val vh = videoSize.height.toFloat().coerceAtLeast(1f)
        val scale = maxOf(cw / vw, ch / vh)
        AndroidView(
            factory = { textureView },
            modifier = Modifier.requiredSize(with(density) { (vw * scale).toDp() }, with(density) { (vh * scale).toDp() }),
        )
    }
}

/** Foto o vídeo del fondo (con desenfoque opcional), sin velo ni refracción. */
@Composable
private fun WallpaperMedia(
    path: String,
    blur: Float,
    modifier: Modifier = Modifier,
) {
    val blurDp = (blur.coerceIn(0f, 1f) * 40f).dp
    val canBlur = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && blur > 0.01f
    val mediaModifier =
        modifier
            .fillMaxSize()
            .then(if (canBlur) Modifier.graphicsLayer(scaleX = 1.12f, scaleY = 1.12f).blur(blurDp) else Modifier)
    if (isVideoWallpaper(path)) {
        LoopingVideoWallpaper(source = path, modifier = mediaModifier)
    } else {
        AsyncImage(
            model = if (isContentUri(path)) wallpaperUri(path) else File(path),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = mediaModifier,
        )
    }
}

/** Fondo de pantalla de toda la app. [blur] va de 0f a 1f (0% a 100%). */
@Composable
fun AppWallpaper(
    path: String,
    opacity: Float,
    blur: Float,
    backdrop: LiquidBackdrop?,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .fillMaxSize()
                .then(if (backdrop != null) Modifier.liquidGlassSource(backdrop, Color.Transparent) else Modifier),
    ) {
        WallpaperMedia(path = path, blur = blur)
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background.copy(alpha = opacity.coerceIn(0f, 0.95f))),
        )
    }
}

/**
 * Fondo del modo aleatorio. [path] es el archivo (o content://) elegido para la canción actual: no cambia mientras
 * suena la misma canción, y al pasar a otra se hace un fundido suave de 1 s hacia el nuevo fondo.
 */
@Composable
fun RandomAppWallpaper(
    path: String,
    opacity: Float,
    blur: Float,
    backdrop: LiquidBackdrop?,
    modifier: Modifier = Modifier,
) {
    // La fuente del cristal líquido y el velo son únicos: solo el contenido hace fundido entre archivos.
    Box(
        modifier =
            modifier
                .fillMaxSize()
                .then(if (backdrop != null) Modifier.liquidGlassSource(backdrop, Color.Transparent) else Modifier),
    ) {
        Crossfade(
            targetState = path,
            animationSpec = tween(durationMillis = 1000),
            label = "randomWallpaper",
        ) { current ->
            WallpaperMedia(path = current, blur = blur)
        }
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background.copy(alpha = opacity.coerceIn(0f, 0.95f))),
        )
    }
}
