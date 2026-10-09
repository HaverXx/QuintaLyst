/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.screens.settings

import android.net.Uri
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.ui.text.style.TextOverflow
import com.metrolist.music.ui.component.GlassFilterChip
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.metrolist.music.R
import com.metrolist.music.constants.AlbumBgOpacityKey
import com.metrolist.music.constants.AppBackgroundColorKey
import com.metrolist.music.constants.AppBackgroundMode
import com.metrolist.music.constants.AppBackgroundModeKey
import com.metrolist.music.constants.DynamicBackgroundKey
import com.metrolist.music.constants.DynamicBackgroundStyle
import com.metrolist.music.constants.DynamicBackgroundStyleKey
import com.metrolist.music.constants.DynamicBgBlurKey
import com.metrolist.music.constants.DynamicBgIntensityKey
import com.metrolist.music.constants.DynamicBgSpeedKey
import com.metrolist.music.constants.HomeWallpaperBlurKey
import com.metrolist.music.constants.HomeWallpaperGalleryKey
import com.metrolist.music.constants.HomeWallpaperSourcesKey
import com.metrolist.music.constants.WallpaperSourceType
import com.metrolist.music.constants.joinWallpaperSources
import com.metrolist.music.constants.parseWallpaperSources
import com.metrolist.music.utils.DeviceGalleryScanner
import androidx.compose.material3.AlertDialog
import com.metrolist.music.constants.HomeWallpaperRandomKey
import com.metrolist.music.constants.WALLPAPER_GALLERY_DIR
import com.metrolist.music.constants.WALLPAPER_GALLERY_MAX_ITEMS
import com.metrolist.music.constants.joinWallpaperGallery
import com.metrolist.music.constants.parseWallpaperGallery
import com.metrolist.music.constants.HomeWallpaperKey
import com.metrolist.music.constants.HomeWallpaperOpacityKey
import com.metrolist.music.constants.parseBackgroundColor
import com.metrolist.music.constants.resolveAppBackgroundMode
import com.metrolist.music.constants.toHexString
import com.metrolist.music.ui.component.Material3SettingsGroup
import com.metrolist.music.ui.component.Material3SettingsItem
import com.metrolist.music.ui.component.isVideoWallpaper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.metrolist.music.utils.rememberEnumPreference
import com.metrolist.music.utils.rememberPreference

private val SolidPalette: List<Color> =
    listOf(
        // Neutros
        Color(0xFF000000), Color(0xFF121212), Color(0xFF1E1E1E), Color(0xFF2B2B2B),
        Color(0xFF9E9E9E), Color(0xFFE0E0E0), Color(0xFFF5F5FA), Color(0xFFFFFFFF),
        // Tonos oscuros con color
        Color(0xFF1A1A2E), Color(0xFF16213E), Color(0xFF0F3460), Color(0xFF2D142C),
        Color(0xFF1B2E1F), Color(0xFF2E1A1A), Color(0xFF2A2140), Color(0xFF102A2A),
        // Colores vivos
        Color(0xFFB71C1C), Color(0xFFE53935), Color(0xFFF4511E), Color(0xFFFB8C00),
        Color(0xFFFDD835), Color(0xFF7CB342), Color(0xFF43A047), Color(0xFF00897B),
        Color(0xFF00ACC1), Color(0xFF039BE5), Color(0xFF1E88E5), Color(0xFF3949AB),
        Color(0xFF5E35B1), Color(0xFF8E24AA), Color(0xFFD81B60), Color(0xFF6D4C41),
    )

/**
 * Ajustes del fondo de la app. Solo hay un modo activo y cada modo despliega únicamente sus propias opciones:
 * Color sólido, Gradiente animado, Fondo dinámico (foto del álbum) y Fondo personalizado (tu foto).
 */
@Composable
fun AppBackgroundSettingsGroup() {
    val context = LocalContext.current

    val (modeRaw, onModeRawChange) = rememberPreference(AppBackgroundModeKey, defaultValue = "")
    val (legacyDynamic) = rememberPreference(DynamicBackgroundKey, defaultValue = true)
    val (legacyStyle) = rememberEnumPreference(DynamicBackgroundStyleKey, defaultValue = DynamicBackgroundStyle.BLUR_ALBUM)
    val (colorHex, onColorHexChange) = rememberPreference(AppBackgroundColorKey, defaultValue = "")

    val (gradientIntensity, onGradientIntensityChange) = rememberPreference(DynamicBgIntensityKey, defaultValue = 0.35f)
    val (gradientSpeed, onGradientSpeedChange) = rememberPreference(DynamicBgSpeedKey, defaultValue = 1.0f)

    val (albumOpacity, onAlbumOpacityChange) = rememberPreference(AlbumBgOpacityKey, defaultValue = 0.85f)
    val (albumBlur, onAlbumBlurChange) = rememberPreference(DynamicBgBlurKey, defaultValue = 0.15f)

    val (photoPath, onPhotoPathChange) = rememberPreference(HomeWallpaperKey, defaultValue = "")
    // Se guarda como "velo" (0 = foto sin velo), pero se muestra como opacidad de la foto.
    val (photoVeil, onPhotoVeilChange) = rememberPreference(HomeWallpaperOpacityKey, defaultValue = 0.35f)
    val (photoBlur, onPhotoBlurChange) = rememberPreference(HomeWallpaperBlurKey, defaultValue = 0f)

    val (randomEnabled, onRandomEnabledChange) = rememberPreference(HomeWallpaperRandomKey, defaultValue = false)
    val (galleryRaw, onGalleryRawChange) = rememberPreference(HomeWallpaperGalleryKey, defaultValue = "")
    val (sourcesRaw, onSourcesRawChange) = rememberPreference(HomeWallpaperSourcesKey, defaultValue = "MANUAL")
    val sources = remember(sourcesRaw) { parseWallpaperSources(sourcesRaw) }
    var showDevicePrivacyDialog by remember { mutableStateOf(false) }
    val galleryPaths = remember(galleryRaw) { parseWallpaperGallery(galleryRaw).filter { java.io.File(it).exists() } }
    val coroutineScope = rememberCoroutineScope()

    val mode =
        resolveAppBackgroundMode(
            saved = modeRaw,
            legacyDynamic = legacyDynamic,
            legacyStyle = legacyStyle,
            hasWallpaper = photoPath.isNotBlank(),
        )
    val setMode: (AppBackgroundMode) -> Unit = { onModeRawChange(it.name) }

    val pickPhotoLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? ->
            if (uri != null) {
                val isVideo = context.contentResolver.getType(uri).orEmpty().startsWith("video")
                if (isVideo && videoDurationMs(context, uri) > MAX_VIDEO_WALLPAPER_MS) {
                    Toast.makeText(context, "El vídeo no puede durar más de 20 segundos", Toast.LENGTH_LONG).show()
                } else {
                    val saved = saveAppBackgroundPhoto(context, uri)
                    if (saved != null) {
                        onPhotoPathChange(saved)
                        setMode(AppBackgroundMode.CUSTOM)
                        Toast.makeText(context, context.getString(R.string.home_wallpaper_set), Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    // Selección múltiple para el modo "fondos aleatorios": se añaden a la galería de la app.
    val pickManyLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(20)) { uris: List<Uri> ->
            if (uris.isNotEmpty()) {
                coroutineScope.launch {
                    val currentList = galleryPaths
                    val room = (WALLPAPER_GALLERY_MAX_ITEMS - currentList.size).coerceAtLeast(0)
                    var tooLong = 0
                    val added =
                        withContext(Dispatchers.IO) {
                            val result = mutableListOf<String>()
                            for ((i, uri) in uris.withIndex()) {
                                if (result.size >= room) break
                                val isVideo = context.contentResolver.getType(uri).orEmpty().startsWith("video")
                                if (isVideo && videoDurationMs(context, uri) > MAX_VIDEO_WALLPAPER_MS) {
                                    tooLong++
                                    continue
                                }
                                copyToWallpaperGallery(context, uri, i)?.let { result.add(it) }
                            }
                            result
                        }
                    if (added.isNotEmpty()) {
                        onGalleryRawChange(joinWallpaperGallery(currentList + added))
                        setMode(AppBackgroundMode.CUSTOM)
                    }
                    val msg =
                        buildString {
                            append("Añadidos: ${added.size}")
                            if (tooLong > 0) append(" · $tooLong vídeo(s) de más de 20 s omitidos")
                            if (added.size < uris.size - tooLong) append(" · máximo $WALLPAPER_GALLERY_MAX_ITEMS archivos")
                        }
                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                }
            }
        }
    // Permiso de lectura de la galería del móvil (solo para el tipo "Fotos y vídeos aleatorios de tu galería").
    val devicePermissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { _ ->
            if (DeviceGalleryScanner.hasPermission(context)) {
                onSourcesRawChange(joinWallpaperSources(sources + WallpaperSourceType.DEVICE))
                setMode(AppBackgroundMode.CUSTOM)
            } else {
                Toast
                    .makeText(context, "Sin permiso de galería no se pueden usar tus fotos como fondo aleatorio", Toast.LENGTH_LONG)
                    .show()
            }
        }
    val toggleSource: (WallpaperSourceType) -> Unit = { type ->
        when {
            type == WallpaperSourceType.DEVICE && type !in sources -> showDevicePrivacyDialog = true
            type in sources && sources.size == 1 ->
                Toast.makeText(context, "Tiene que quedar al menos un tipo de fondo activado", Toast.LENGTH_SHORT).show()
            type in sources -> onSourcesRawChange(joinWallpaperSources(sources - type))
            else -> onSourcesRawChange(joinWallpaperSources(sources + type))
        }
    }
    if (showDevicePrivacyDialog) {
        AlertDialog(
            onDismissRequest = { showDevicePrivacyDialog = false },
            title = { Text("Acceso a tu galería") },
            text = {
                Text(
                    "Para usar fotos y vídeos aleatorios de tu galería, QuintaLyst necesita permiso para leerla.\n\n" +
                        "Esas fotos y vídeos solo se muestran como fondo dentro de tu móvil: no se copian, no se suben " +
                        "y no tienen ningún contacto con internet. Puedes desactivarlo cuando quieras.",
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDevicePrivacyDialog = false
                        if (DeviceGalleryScanner.hasPermission(context)) {
                            onSourcesRawChange(joinWallpaperSources(sources + WallpaperSourceType.DEVICE))
                        } else {
                            devicePermissionLauncher.launch(DeviceGalleryScanner.requiredPermissions())
                        }
                    },
                ) { Text("Continuar") }
            },
            dismissButton = { TextButton(onClick = { showDevicePrivacyDialog = false }) { Text("Cancelar") } },
        )
    }
    val launchMultiPicker: () -> Unit = {
        pickManyLauncher.launch(
            androidx.activity.result.PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo),
        )
    }
    val launchPicker: () -> Unit = {
        pickPhotoLauncher.launch(
            androidx.activity.result.PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo),
        )
    }

    val hasSinglePhoto = photoPath.isNotBlank() && java.io.File(photoPath).exists()
    val hasPhoto = if (randomEnabled) (galleryPaths.isNotEmpty() || WallpaperSourceType.DEVICE in sources) else hasSinglePhoto

    Column(modifier = Modifier.fillMaxWidth()) {
        Material3SettingsGroup(
            title = "Fondo de la app",
            items =
                listOf(
                    Material3SettingsItem(
                        icon = painterResource(R.drawable.palette),
                        title = { Text("Estilo de fondo") },
                        description = { BackgroundModeSelector(mode = mode, onSelect = setMode) },
                    ),
                ),
        )
        AnimatedContent(
            targetState = mode,
            modifier = Modifier.fillMaxWidth(),
            transitionSpec = {
                (fadeIn(tween(280, delayMillis = 90)) togetherWith fadeOut(tween(140))) using
                    SizeTransform(clip = false) { _, _ -> tween(320, easing = FastOutSlowInEasing) }
            },
            label = "backgroundModeOptions",
        ) { current ->
            Column(
                modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                when (current) {
                    AppBackgroundMode.SOLID ->
                        Material3SettingsGroup(
                            title = "Color",
                            items =
                                listOf(
                                    Material3SettingsItem(
                                        icon = painterResource(R.drawable.palette),
                                        title = { Text("Elige un color") },
                                        description = {
                                            SolidColorPicker(currentHex = colorHex, onColorChange = onColorHexChange)
                                        },
                                    ),
                                ),
                        )

                    AppBackgroundMode.ANIMATED_GRADIENT ->
                        Material3SettingsGroup(
                            title = "Animación",
                            items =
                                listOf(
                                    sliderItem(
                                        iconRes = R.drawable.contrast,
                                        title = "Intensidad: ${(gradientIntensity * 100).toInt()}%",
                                        value = gradientIntensity,
                                        range = 0.05f..0.8f,
                                        onChange = onGradientIntensityChange,
                                    ),
                                    sliderItem(
                                        iconRes = R.drawable.speed,
                                        title = "Velocidad de animación: ${String.format("%.1f", gradientSpeed)}x",
                                        value = gradientSpeed,
                                        range = 0.2f..3.0f,
                                        onChange = onGradientSpeedChange,
                                    ),
                                ),
                        )

                    AppBackgroundMode.ALBUM ->
                        Material3SettingsGroup(
                            title = "Portada",
                            items =
                                listOf(
                                    sliderItem(
                                        iconRes = R.drawable.contrast,
                                        title = "Opacidad: ${(albumOpacity * 100).toInt()}%",
                                        value = albumOpacity,
                                        range = 0.1f..1f,
                                        onChange = onAlbumOpacityChange,
                                    ),
                                    sliderItem(
                                        iconRes = R.drawable.blur_on,
                                        title = "Desenfoque: ${(albumBlur * 100).toInt()}%",
                                        value = albumBlur,
                                        range = 0f..1f,
                                        onChange = onAlbumBlurChange,
                                    ),
                                ),
                        )

                    AppBackgroundMode.CUSTOM -> {
                        fun sourceItem(
                            type: WallpaperSourceType,
                            icon: androidx.compose.ui.graphics.painter.Painter,
                            title: String,
                            description: String,
                        ) = Material3SettingsItem(
                            icon = icon,
                            title = { Text(title) },
                            description = { Text(description) },
                            trailingContent = { Switch(checked = type in sources, onCheckedChange = null) },
                            onClick = { toggleSource(type) },
                        )
                        Material3SettingsGroup(
                            title = "Origen del fondo",
                            items =
                                buildList {
                                    add(
                                        Material3SettingsItem(
                                            icon = painterResource(R.drawable.shuffle),
                                            title = { Text("Fondos aleatorios") },
                                            description = { Text("El fondo cambia al azar cada vez que cambia la canción y se mantiene mientras suene la misma") },
                                            trailingContent = { Switch(checked = randomEnabled, onCheckedChange = null) },
                                            onClick = { onRandomEnabledChange(!randomEnabled) },
                                        ),
                                    )
                                    if (randomEnabled) {
                                        add(
                                            sourceItem(
                                                WallpaperSourceType.MANUAL,
                                                painterResource(R.drawable.insert_photo),
                                                "Fotos y vídeos que elijas",
                                                "Los que añadas a continuación",
                                            ),
                                        )
                                        add(
                                            sourceItem(
                                                WallpaperSourceType.DEVICE,
                                                painterResource(R.drawable.shuffle),
                                                "Aleatorios de tu galería",
                                                "Fotos y vídeos de tu móvil, elegidos al azar. Nunca salen de tu dispositivo",
                                            ),
                                        )
                                        add(
                                            sourceItem(
                                                WallpaperSourceType.GRADIENT,
                                                painterResource(R.drawable.gradient),
                                                "Intercalar gradiente animado",
                                                "Se mezcla de vez en cuando con los fondos anteriores",
                                            ),
                                        )
                                        add(
                                            sourceItem(
                                                WallpaperSourceType.ALBUM,
                                                painterResource(R.drawable.album),
                                                "Intercalar foto de álbum",
                                                "La portada de la canción desenfocada como fondo",
                                            ),
                                        )
                                        add(
                                            sourceItem(
                                                WallpaperSourceType.SOLID,
                                                painterResource(R.drawable.palette),
                                                "Intercalar color sólido",
                                                "Usa el color elegido en «Color sólido» (o el del tema)",
                                            ),
                                        )
                                        if (WallpaperSourceType.MANUAL in sources) {
                                            add(
                                                Material3SettingsItem(
                                                    icon = painterResource(R.drawable.add),
                                                    title = { Text("Añadir fotos o vídeos") },
                                                    description = {
                                                        Text("Selecciona varias a la vez (${galleryPaths.size}/$WALLPAPER_GALLERY_MAX_ITEMS). Vídeos de hasta 20 segundos")
                                                    },
                                                    onClick = launchMultiPicker,
                                                ),
                                            )
                                            if (galleryPaths.isNotEmpty()) {
                                                add(
                                                    Material3SettingsItem(
                                                        icon = painterResource(R.drawable.insert_photo),
                                                        title = { Text("Tu galería de fondos") },
                                                        description = {
                                                            WallpaperGalleryRow(
                                                                paths = galleryPaths,
                                                                onRemove = { path ->
                                                                    java.io.File(path).delete()
                                                                    onGalleryRawChange(joinWallpaperGallery(galleryPaths - path))
                                                                },
                                                            )
                                                        },
                                                    ),
                                                )
                                                add(
                                                    Material3SettingsItem(
                                                        icon = painterResource(R.drawable.delete),
                                                        title = { Text("Vaciar galería") },
                                                        onClick = {
                                                            galleryPaths.forEach { java.io.File(it).delete() }
                                                            java.io.File(context.filesDir, WALLPAPER_GALLERY_DIR).listFiles()?.forEach { it.delete() }
                                                            onGalleryRawChange("")
                                                        },
                                                    ),
                                                )
                                            }
                                        }
                                    } else {
                                        add(
                                            Material3SettingsItem(
                                                icon = painterResource(R.drawable.insert_photo),
                                                title = { Text("Cambiar fondo") },
                                                description = { Text("Elige una foto o un vídeo de hasta 20 segundos") },
                                                onClick = launchPicker,
                                            ),
                                        )
                                    }
                                },
                        )
                        if (hasPhoto) {
                            val photoOpacity = (1f - photoVeil).coerceIn(0.1f, 1f)
                            Material3SettingsGroup(
                                title = "Ajustes de la imagen",
                                items =
                                    buildList {
                                        add(
                                            sliderItem(
                                                iconRes = R.drawable.contrast,
                                                title = "Opacidad: ${(photoOpacity * 100).toInt()}%",
                                                value = photoOpacity,
                                                range = 0.1f..1f,
                                                onChange = { onPhotoVeilChange((1f - it).coerceIn(0f, 0.9f)) },
                                            ),
                                        )
                                        add(
                                            sliderItem(
                                                iconRes = R.drawable.blur_on,
                                                title = "Desenfoque: ${(photoBlur * 100).toInt()}%",
                                                value = photoBlur,
                                                range = 0f..1f,
                                                onChange = onPhotoBlurChange,
                                            ),
                                        )
                                        if (!randomEnabled) {
                                            add(
                                                Material3SettingsItem(
                                                    icon = painterResource(R.drawable.delete),
                                                    title = { Text("Quitar fondo") },
                                                    onClick = {
                                                        java.io.File(photoPath).delete()
                                                        onPhotoPathChange("")
                                                        Toast
                                                            .makeText(context, context.getString(R.string.home_wallpaper_removed), Toast.LENGTH_SHORT)
                                                            .show()
                                                    },
                                                ),
                                            )
                                        }
                                    },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BackgroundModeSelector(
    mode: AppBackgroundMode,
    onSelect: (AppBackgroundMode) -> Unit,
) {
    val options =
        remember {
            listOf(
                Triple(AppBackgroundMode.SOLID, R.drawable.palette, "Color sólido"),
                Triple(AppBackgroundMode.ANIMATED_GRADIENT, R.drawable.gradient, "Gradiente"),
                Triple(AppBackgroundMode.ALBUM, R.drawable.album, "Foto de álbum"),
                Triple(AppBackgroundMode.CUSTOM, R.drawable.insert_photo, "Personalizado"),
            )
        }
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        options.chunked(2).forEach { rowOptions ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                rowOptions.forEach { (option, icon, label) ->
                    GlassFilterChip(
                        selected = mode == option,
                        onClick = { onSelect(option) },
                        modifier = Modifier.weight(1f),
                        leadingIcon = { Icon(painter = painterResource(icon), contentDescription = null, modifier = Modifier.size(18.dp)) },
                        label = { Text(text = label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    )
                }
            }
        }
        AnimatedContent(
            targetState = mode,
            transitionSpec = { fadeIn(tween(220, delayMillis = 60)) togetherWith fadeOut(tween(120)) },
            label = "backgroundModeDescription",
        ) { current ->
            Text(
                text =
                    when (current) {
                        AppBackgroundMode.SOLID -> "Un solo color liso para toda la app"
                        AppBackgroundMode.ANIMATED_GRADIENT -> "Degradado en movimiento con los colores de la canción"
                        AppBackgroundMode.ALBUM -> "La portada de la canción que suena, a pantalla completa"
                        AppBackgroundMode.CUSTOM -> "Tu propia foto o vídeo (máx. 20 s, en bucle) como fondo de toda la app"
                    },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private const val MAX_VIDEO_WALLPAPER_MS = 20_000L

private fun videoDurationMs(context: android.content.Context, uri: Uri): Long =
    try {
        val r = android.media.MediaMetadataRetriever()
        try {
            r.setDataSource(context, uri)
            r.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: Long.MAX_VALUE
        } finally {
            r.release()
        }
    } catch (e: Exception) {
        Long.MAX_VALUE
    }

private fun copyToWallpaperGallery(
    context: android.content.Context,
    uri: Uri,
    index: Int,
): String? =
    try {
        val mime = context.contentResolver.getType(uri).orEmpty()
        val ext =
            when {
                mime.startsWith("video/webm") -> "webm"
                mime.startsWith("video") -> "mp4"
                mime.contains("gif") -> "gif"
                mime.contains("webp") -> "webp"
                mime.contains("png") -> "png"
                else -> "jpg"
            }
        val dir = java.io.File(context.filesDir, WALLPAPER_GALLERY_DIR).apply { mkdirs() }
        val out = java.io.File(dir, "wp_${System.currentTimeMillis()}_$index.$ext")
        context.contentResolver.openInputStream(uri)?.use { input ->
            out.outputStream().use { input.copyTo(it) }
        } ?: return null
        out.absolutePath
    } catch (e: Exception) {
        null
    }

@Composable
private fun WallpaperGalleryRow(
    paths: List<String>,
    onRemove: (String) -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
                .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        paths.forEach { path ->
            Box(modifier = Modifier.size(72.dp)) {
                Box(
                    modifier =
                        Modifier
                            .matchParentSize()
                            .clip(MaterialTheme.shapes.small)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center,
                ) {
                    if (isVideoWallpaper(path)) {
                        Icon(
                            painter = painterResource(R.drawable.slow_motion_video),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        AsyncImage(
                            model = java.io.File(path),
                            contentDescription = null,
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                            modifier = Modifier.matchParentSize(),
                        )
                    }
                }
                Box(
                    modifier =
                        Modifier
                            .align(Alignment.TopEnd)
                            .padding(2.dp)
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.6f))
                            .clickable { onRemove(path) },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.close),
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(14.dp),
                    )
                }
            }
        }
    }
}

private fun stringOrDefault(context: android.content.Context, id: Int): String = context.getString(id)

@Composable
private fun sliderItem(
    iconRes: Int,
    title: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onChange: (Float) -> Unit,
): Material3SettingsItem =
    Material3SettingsItem(
        icon = painterResource(iconRes),
        title = { Text(title) },
        description = {
            Slider(
                value = value.coerceIn(range.start, range.endInclusive),
                onValueChange = onChange,
                valueRange = range,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
            )
        },
    )

@Composable
private fun SolidColorPicker(
    currentHex: String,
    onColorChange: (String) -> Unit,
) {
    val current = parseBackgroundColor(currentHex)
    var text by remember(currentHex) { mutableStateOf(currentHex) }
    val parsed = parseBackgroundColor(text)
    val invalid = text.isNotBlank() && parsed == null

    Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
        SolidPalette.chunked(8).forEach { rowColors ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                rowColors.forEach { color ->
                    val selected = current != null && current.toHexString() == color.toHexString()
                    Box(
                        modifier =
                            Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(color)
                                .border(
                                    width = if (selected) 2.5.dp else 1.dp,
                                    color = if (selected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.35f),
                                    shape = CircleShape,
                                ).clickable {
                                    val hex = color.toHexString()
                                    text = hex
                                    onColorChange(hex)
                                },
                        contentAlignment = Alignment.Center,
                    ) {
                        if (selected) {
                            Icon(
                                painter = painterResource(R.drawable.check),
                                contentDescription = null,
                                tint = if (color.luminance() > 0.5f) Color.Black else Color.White,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier =
                    Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(parsed ?: current ?: MaterialTheme.colorScheme.surface)
                        .border(1.dp, Color.White.copy(alpha = 0.35f), CircleShape),
            )
            Spacer(modifier = Modifier.size(12.dp))
            OutlinedTextField(
                value = text,
                onValueChange = { input ->
                    text = input.take(9)
                    val color = parseBackgroundColor(input)
                    if (color != null) onColorChange(color.toHexString())
                },
                label = { Text("Código de color") },
                placeholder = { Text("#1A1A2E") },
                singleLine = true,
                isError = invalid,
                supportingText = if (invalid) ({ Text("Usa formato #RRGGBB") }) else null,
                modifier = Modifier.weight(1f),
            )
        }

        if (currentHex.isNotBlank()) {
            TextButton(
                onClick = {
                    text = ""
                    onColorChange("")
                },
            ) {
                Text("Usar el color del tema")
            }
        }
    }
}

private fun saveAppBackgroundPhoto(
    context: android.content.Context,
    uri: Uri,
): String? =
    try {
        val mime = context.contentResolver.getType(uri).orEmpty()
        val ext =
            when {
                mime.startsWith("video/webm") -> "webm"
                mime.startsWith("video") -> "mp4"
                mime.contains("gif") -> "gif"
                mime.contains("webp") -> "webp"
                mime.contains("png") -> "png"
                else -> "jpg"
            }
        context.filesDir.listFiles { f -> f.name.startsWith("home_wallpaper") }?.forEach { it.delete() }
        val out = java.io.File(context.filesDir, "home_wallpaper_${System.currentTimeMillis()}.$ext")
        context.contentResolver.openInputStream(uri)?.use { input ->
            out.outputStream().use { input.copyTo(it) }
        } ?: return null
        out.absolutePath
    } catch (e: Exception) {
        null
    }
