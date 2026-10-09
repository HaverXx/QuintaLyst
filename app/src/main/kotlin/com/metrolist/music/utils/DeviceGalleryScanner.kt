/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.utils

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.provider.MediaStore
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.random.Random

/**
 * Lee la galería del propio móvil (MediaStore) para usar fotos y vídeos como fondo.
 * Solo se obtienen URIs locales (content://): nada se copia, se sube ni sale del dispositivo.
 */
object DeviceGalleryScanner {
    private const val MAX_VIDEO_MS = 20_000L

    fun requiredPermissions(): Array<String> =
        when {
            Build.VERSION.SDK_INT >= 34 ->
                arrayOf(
                    Manifest.permission.READ_MEDIA_IMAGES,
                    Manifest.permission.READ_MEDIA_VIDEO,
                    Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED,
                )
            Build.VERSION.SDK_INT >= 33 ->
                arrayOf(Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VIDEO)
            else -> arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }

    fun hasPermission(context: Context): Boolean = requiredPermissions().any {
        ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
    }

    /** Devuelve el content:// de una foto o vídeo (≤ 20 s) al azar, intentando evitar [exclude]. Null si no hay. */
    suspend fun randomMedia(
        context: Context,
        exclude: String?,
    ): String? =
        withContext(Dispatchers.IO) {
            if (!hasPermission(context)) return@withContext null
            try {
                val resolver = context.contentResolver
                val imagesUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                val videosUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                val images = resolver.query(imagesUri, arrayOf(MediaStore.Images.Media._ID), null, null, null)
                val videos =
                    resolver.query(
                        videosUri,
                        arrayOf(MediaStore.Video.Media._ID),
                        "${MediaStore.Video.Media.DURATION} > 0 AND ${MediaStore.Video.Media.DURATION} <= ?",
                        arrayOf(MAX_VIDEO_MS.toString()),
                        null,
                    )
                try {
                    val imageCount = images?.count ?: 0
                    val videoCount = videos?.count ?: 0
                    val total = imageCount + videoCount
                    if (total == 0) return@withContext null
                    var fallback: String? = null
                    repeat(5) {
                        val r = Random.nextInt(total)
                        val cursor = if (r < imageCount) images else videos
                        val base = if (r < imageCount) imagesUri else videosUri
                        val pos = if (r < imageCount) r else r - imageCount
                        if (cursor != null && cursor.moveToPosition(pos)) {
                            val uri = ContentUris.withAppendedId(base, cursor.getLong(0)).toString()
                            if (uri != exclude) return@withContext uri
                            fallback = uri
                        }
                    }
                    fallback
                } finally {
                    images?.close()
                    videos?.close()
                }
            } catch (e: Exception) {
                null
            }
        }
}
