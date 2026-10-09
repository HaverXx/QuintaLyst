/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.constants

import androidx.compose.ui.graphics.Color
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

/** Los cuatro tipos de fondo de la app. Solo uno está activo a la vez. */
enum class AppBackgroundMode {
    SOLID,
    ANIMATED_GRADIENT,
    ALBUM,
    CUSTOM,
}

/** Vacío = aún no elegido: se deduce de los ajustes antiguos (fondo dinámico / foto de fondo). */
val AppBackgroundModeKey = stringPreferencesKey("appBackgroundMode")

/** Color del modo "Color sólido" en hexadecimal (#RRGGBB). Vacío = color del tema. */
val AppBackgroundColorKey = stringPreferencesKey("appBackgroundColor")

/** Opacidad de la portada en el modo "Foto de álbum" (0..1). */
val AlbumBgOpacityKey = floatPreferencesKey("albumBgOpacity")

/** Modo "fondos aleatorios" del fondo personalizado: rota entre varias fotos/vídeos de la galería de la app. */
val HomeWallpaperRandomKey = booleanPreferencesKey("homeWallpaperRandom")

/** Rutas (una por línea) de los archivos de la galería de fondos aleatorios. */
val HomeWallpaperGalleryKey = stringPreferencesKey("homeWallpaperGallery")

/** Obsoleto: antes el fondo aleatorio cambiaba por tiempo. Ahora cambia al cambiar de canción. */
val HomeWallpaperIntervalKey = floatPreferencesKey("homeWallpaperIntervalSec")

/** De dónde salen los fondos del modo aleatorio (tipos separados por coma, ver [WallpaperSourceType]). */
val HomeWallpaperSourcesKey = stringPreferencesKey("homeWallpaperSources")

/** Tipos de fondo que pueden entrar en el sorteo del modo aleatorio. */
enum class WallpaperSourceType {
    /** Fotos/vídeos elegidos manualmente (copiados a la galería de la app). */
    MANUAL,

    /** Fotos/vídeos aleatorios de la galería del móvil (solo se leen en el dispositivo). */
    DEVICE,
    GRADIENT,
    ALBUM,
    SOLID,
}

fun parseWallpaperSources(raw: String): Set<WallpaperSourceType> =
    raw
        .split(',')
        .mapNotNull { n -> WallpaperSourceType.entries.firstOrNull { it.name == n.trim() } }
        .toSet()
        .ifEmpty { setOf(WallpaperSourceType.MANUAL) }

fun joinWallpaperSources(types: Set<WallpaperSourceType>): String =
    WallpaperSourceType.entries.filter { it in types }.joinToString(",") { it.name }

enum class WallpaperSlotKind { MEDIA, GRADIENT, ALBUM, SOLID }

/** Fondo elegido para la canción actual. [source] es una ruta de archivo o un content:// (solo si [kind] es MEDIA). */
data class WallpaperSlot(
    val kind: WallpaperSlotKind,
    val type: WallpaperSourceType,
    val source: String? = null,
)

/**
 * Sortea el fondo para la siguiente canción entre los tipos activados. Intenta no repetir el tipo ni el archivo
 * anterior. Devuelve null si ningún tipo activado puede dar un fondo (p. ej. solo galería del móvil sin permiso).
 */
suspend fun pickWallpaperSlot(
    sources: Set<WallpaperSourceType>,
    manual: List<String>,
    previous: WallpaperSlot?,
    deviceMedia: suspend (exclude: String?) -> String?,
): WallpaperSlot? {
    val types =
        sources
            .filter { it != WallpaperSourceType.MANUAL || manual.isNotEmpty() }
            .shuffled()
            .let { list -> if (list.size > 1) list.sortedBy { if (it == previous?.type) 1 else 0 } else list }
    for (type in types) {
        when (type) {
            WallpaperSourceType.MANUAL -> {
                val options = manual.filter { it != previous?.source }.ifEmpty { manual }
                return WallpaperSlot(WallpaperSlotKind.MEDIA, type, options.random())
            }
            WallpaperSourceType.DEVICE -> {
                val uri = deviceMedia(previous?.source) ?: continue
                return WallpaperSlot(WallpaperSlotKind.MEDIA, type, uri)
            }
            WallpaperSourceType.GRADIENT -> return WallpaperSlot(WallpaperSlotKind.GRADIENT, type)
            WallpaperSourceType.ALBUM -> return WallpaperSlot(WallpaperSlotKind.ALBUM, type)
            WallpaperSourceType.SOLID -> return WallpaperSlot(WallpaperSlotKind.SOLID, type)
        }
    }
    return null
}

const val WALLPAPER_GALLERY_DIR = "wallpaper_gallery"
const val WALLPAPER_GALLERY_MAX_ITEMS = 30

fun parseWallpaperGallery(raw: String): List<String> = raw.split('\n').map { it.trim() }.filter { it.isNotEmpty() }

fun joinWallpaperGallery(paths: List<String>): String = paths.joinToString("\n")

/**
 * Devuelve el modo activo. Si el usuario ya eligió uno se respeta; si no, se migra desde los ajustes anteriores
 * para que quien ya tenía una foto o un degradado siga viendo lo mismo.
 */
fun resolveAppBackgroundMode(
    saved: String,
    legacyDynamic: Boolean,
    legacyStyle: DynamicBackgroundStyle,
    hasWallpaper: Boolean,
): AppBackgroundMode {
    AppBackgroundMode.entries.firstOrNull { it.name == saved }?.let { return it }
    return when {
        hasWallpaper -> AppBackgroundMode.CUSTOM
        legacyDynamic && legacyStyle == DynamicBackgroundStyle.BLUR_ALBUM -> AppBackgroundMode.ALBUM
        legacyDynamic -> AppBackgroundMode.ANIMATED_GRADIENT
        else -> AppBackgroundMode.SOLID
    }
}

/** Acepta #RGB, #RRGGBB y #AARRGGBB (con o sin '#'). Devuelve null si no es un color válido. */
fun parseBackgroundColor(input: String): Color? {
    val hex = input.trim().removePrefix("#")
    if (hex.isEmpty() || !hex.all { it.isDigit() || it.lowercaseChar() in 'a'..'f' }) return null
    val argb: Long =
        when (hex.length) {
            3 -> {
                val r = hex[0].toString().repeat(2)
                val g = hex[1].toString().repeat(2)
                val b = hex[2].toString().repeat(2)
                "FF$r$g$b".toLong(16)
            }
            6 -> "FF$hex".toLong(16)
            8 -> hex.toLong(16)
            else -> return null
        }
    return Color(argb.toInt())
}

fun Color.toHexString(): String {
    val argb = android.graphics.Color.argb(
        (alpha * 255f + 0.5f).toInt(),
        (red * 255f + 0.5f).toInt(),
        (green * 255f + 0.5f).toInt(),
        (blue * 255f + 0.5f).toInt(),
    )
    return "#%06X".format(argb and 0xFFFFFF)
}
