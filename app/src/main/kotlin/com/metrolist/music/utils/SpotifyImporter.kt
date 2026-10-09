/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.utils

import com.metrolist.innertube.YouTube
import com.metrolist.innertube.models.SongItem
import com.metrolist.music.db.DatabaseDao
import com.metrolist.music.db.entities.Playlist
import com.metrolist.music.db.entities.PlaylistEntity
import com.metrolist.music.models.toMediaMetadata
import java.time.LocalDateTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import timber.log.Timber
import java.util.concurrent.TimeUnit

object SpotifyImporter {
    private val client by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .build()
    }

    fun extractPlaylistId(input: String): String? {
        val trimmed = input.trim()
        if (trimmed.startsWith("spotify:playlist:")) {
            return trimmed.removePrefix("spotify:playlist:").substringBefore('?')
        }
        val match = Regex("""(?:spotify\.com/(?:embed/)?playlist/|spotify:playlist:)([a-zA-Z0-9]+)""").find(trimmed)
        if (match != null) return match.groupValues[1]

        if (trimmed.matches(Regex("""^[a-zA-Z0-9]{15,35}$"""))) {
            return trimmed
        }
        return null
    }

    data class SpotifyTrack(
        val title: String,
        val artist: String,
        val durationMs: Long,
    )

    suspend fun importPlaylist(
        urlOrId: String,
        database: DatabaseDao,
        coversDir: java.io.File? = null,
        onProgress: (done: Int, total: Int, currentSong: String) -> Unit,
    ): Result<Pair<PlaylistEntity, Int>> = withContext(Dispatchers.IO) {
        try {
            val playlistId = extractPlaylistId(urlOrId)
                ?: return@withContext Result.failure(IllegalArgumentException("Invalid Spotify URL or playlist ID"))

            val embedUrl = "https://open.spotify.com/embed/playlist/$playlistId"
            val request = Request.Builder()
                .url(embedUrl)
                .header(
                    "User-Agent",
                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
                )
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("Failed to fetch Spotify playlist: HTTP ${response.code}"))
            }

            val html = response.body?.string().orEmpty()
            val regex = Regex("""<script id="__NEXT_DATA__"[^>]*>(.*?)</script>""")
            val jsonMatch = regex.find(html)?.groupValues?.get(1)
                ?: return@withContext Result.failure(Exception("Could not extract Spotify metadata from page"))

            val root = JSONObject(jsonMatch)
            val entity = root.optJSONObject("props")
                ?.optJSONObject("pageProps")
                ?.optJSONObject("state")
                ?.optJSONObject("data")
                ?.optJSONObject("entity")
                ?: return@withContext Result.failure(Exception("Could not find playlist data"))

            val title = entity.optString("title", "Spotify Playlist")
            val coverUrl = extractCoverUrl(entity, html, playlistId)

            val trackListJson = entity.optJSONArray("trackList") ?: return@withContext Result.failure(Exception("Empty track list"))
            val total = trackListJson.length()

            if (total == 0) {
                return@withContext Result.failure(Exception("No tracks found in Spotify playlist"))
            }

            val tracks = mutableListOf<SpotifyTrack>()
            for (i in 0 until total) {
                val item = trackListJson.getJSONObject(i)
                val trackTitle = item.optString("title")
                val artist = item.optString("subtitle")
                val duration = item.optLong("duration", 0L)
                if (trackTitle.isNotBlank()) {
                    tracks.add(SpotifyTrack(trackTitle, artist, duration))
                }
            }

            // La playlist local se crea con el mismo nombre que la de Spotify. Debe llevar bookmarkedAt,
            // que es lo que usa la biblioteca para listar las playlists; sin él quedaba oculta.
            val newPlaylistId = PlaylistEntity.generatePlaylistId()
            // La portada se descarga y se guarda en local: así no depende de la red ni de que caduque el enlace,
            // y la app la trata como portada personalizada (igual que las que eliges tú).
            val savedCover = coverUrl?.takeIf { it.isNotBlank() }?.let { downloadCover(it, coversDir, newPlaylistId) }
            val playlistEntity = PlaylistEntity(
                id = newPlaylistId,
                name = title.ifBlank { "Spotify Playlist" },
                thumbnailUrl = savedCover ?: coverUrl?.takeIf { it.isNotBlank() },
                bookmarkedAt = LocalDateTime.now(),
                isEditable = true,
            )

            val matchedSongPairs = mutableListOf<Pair<String, String?>>()
            val seenIds = HashSet<String>()

            tracks.forEachIndexed { index, track ->
                val query = if (track.artist.isNotBlank()) "${track.title} ${track.artist}" else track.title
                onProgress(index, tracks.size, query)

                try {
                    val searchResult = YouTube.search(query, YouTube.SearchFilter.FILTER_SONG)
                        .getOrNull()?.items?.filterIsInstance<SongItem>()?.firstOrNull()

                    if (searchResult != null && seenIds.add(searchResult.id)) {
                        database.insert(searchResult.toMediaMetadata())
                        matchedSongPairs.add(searchResult.id to null)
                    }
                } catch (e: Exception) {
                    Timber.w(e, "Error finding song on YouTube: $query")
                }
            }
            onProgress(tracks.size, tracks.size, "")

            if (matchedSongPairs.isEmpty()) {
                return@withContext Result.failure(Exception("No tracks found"))
            }

            database.insert(playlistEntity)
            database.addSongsToPlaylist(
                Playlist(playlist = playlistEntity, songCount = 0, songThumbnails = emptyList()),
                matchedSongPairs,
            )
            val foundCount = matchedSongPairs.size

            Result.success(playlistEntity to foundCount)
        } catch (e: Exception) {
            Timber.e(e, "Error importing Spotify playlist")
            Result.failure(e)
        }
    }


    /** Busca recursivamente la primera URL de imagen de Spotify (i.scdn.co / mosaic) dentro del JSON. */
    private fun findImageUrl(node: Any?): String? {
        when (node) {
            is JSONObject -> {
                for (key in listOf("coverArt", "visualIdentity", "images", "image", "cover", "sources")) {
                    if (node.has(key)) findImageUrl(node.opt(key))?.let { return it }
                }
                val direct = node.optString("url")
                if (direct.startsWith("http") && (direct.contains("scdn.co") || direct.contains("spotifycdn") || direct.contains("mosaic") || direct.contains("image"))) {
                    return direct
                }
                val keys = node.keys()
                while (keys.hasNext()) {
                    val k = keys.next()
                    if (k == "trackList") continue
                    findImageUrl(node.opt(k))?.let { return it }
                }
            }
            is org.json.JSONArray -> {
                // Se prefiere la imagen más grande disponible.
                var best: String? = null
                var bestSize = -1
                for (i in 0 until node.length()) {
                    val item = node.opt(i)
                    val url = findImageUrl(item) ?: continue
                    val size = (item as? JSONObject)?.let { maxOf(it.optInt("width"), it.optInt("maxWidth")) } ?: 0
                    if (best == null || size > bestSize) { best = url; bestSize = size }
                }
                return best
            }
        }
        return null
    }

    /** Obtiene la portada: JSON embebido -> og:image (cualquier orden de atributos) -> oEmbed. */
    private fun extractCoverUrl(entity: JSONObject, html: String, playlistId: String): String? {
        findImageUrl(entity)?.takeIf { it.isNotBlank() }?.let { return it }
        val og1 = Regex("""<meta[^>]+property=["']og:image["'][^>]+content=["']([^"']+)["']""")
        val og2 = Regex("""<meta[^>]+content=["']([^"']+)["'][^>]+property=["']og:image["']""")
        (og1.find(html) ?: og2.find(html))
            ?.groupValues?.get(1)?.replace("&amp;", "&")?.takeIf { it.startsWith("http") }?.let { return it }
        // Último recurso: cualquier imagen de i.scdn.co / spotifycdn que aparezca en la página.
        Regex("""https://(?:i\.scdn\.co/image|image-cdn[a-z-]*\.spotifycdn\.com/image|mosaic\.scdn\.co)/[A-Za-z0-9/_\-.]+""")
            .find(html)?.value?.let { return it }
        return try {
            val url = "https://open.spotify.com/oembed?url=" +
                java.net.URLEncoder.encode("https://open.spotify.com/playlist/$playlistId", "UTF-8")
            val (_, body) = httpGet(url)
            JSONObject(body).optString("thumbnail_url").takeIf { it.startsWith("http") }
        } catch (e: Exception) {
            Timber.w(e, "Error fetching Spotify cover")
            null
        }
    }

    /** Descarga la portada a `playlist_covers/playlist_<id>_<ts>.jpg` y devuelve `file://...`, o null si falla. */
    private fun downloadCover(url: String, coversDir: java.io.File?, playlistId: String): String? {
        if (coversDir == null) return null
        return try {
            if (!coversDir.exists()) coversDir.mkdirs()
            val request = Request.Builder().url(url).header("User-Agent", browserUa).build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                val bytes = response.body?.bytes() ?: return null
                if (bytes.size < 512) return null
                val file = java.io.File(coversDir, "playlist_${playlistId}_${System.currentTimeMillis()}.jpg")
                file.writeBytes(bytes)
                "file://${file.absolutePath}"
            }
        } catch (e: Exception) {
            Timber.w(e, "Error downloading Spotify cover")
            null
        }
    }

    // ---------------------------------------------------------------------------------------
    // Jam de Spotify
    // ---------------------------------------------------------------------------------------

    /** Devuelve true si el texto parece un enlace de Jam (spotify.link o socialsession). */
    fun looksLikeJamLink(input: String): Boolean {
        val t = input.trim().lowercase()
        return t.contains("socialsession") || t.contains("spotify.link/") || t.contains("spotify.app.link/") ||
            t.contains("spotify.com/jam")
    }

    private val browserUa =
        "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"

    private fun httpGet(url: String): Pair<String, String> {
        val request = Request.Builder().url(url).header("User-Agent", browserUa).build()
        client.newCall(request).execute().use { response ->
            val finalUrl = response.request.url.toString()
            return finalUrl to response.body?.string().orEmpty()
        }
    }

    /** Lee título y artistas de una pista a través de su página embed pública. */
    private fun fetchTrack(trackId: String): SpotifyTrack? =
        try {
            val (_, html) = httpGet("https://open.spotify.com/embed/track/$trackId")
            val json = Regex("""<script id="__NEXT_DATA__"[^>]*>(.*?)</script>""").find(html)?.groupValues?.get(1)
            val entity = json?.let {
                JSONObject(it).optJSONObject("props")?.optJSONObject("pageProps")
                    ?.optJSONObject("state")?.optJSONObject("data")?.optJSONObject("entity")
            }
            val title = entity?.optString("title").orEmpty().ifBlank { entity?.optString("name").orEmpty() }
            val artists = entity?.optJSONArray("artists")?.let { arr ->
                (0 until arr.length()).joinToString(", ") { arr.getJSONObject(it).optString("name") }
            }.orEmpty().ifBlank { entity?.optString("subtitle").orEmpty() }
            if (title.isBlank()) null else SpotifyTrack(title, artists, entity?.optLong("duration", 0L) ?: 0L)
        } catch (e: Exception) {
            Timber.w(e, "Error reading Spotify track $trackId")
            null
        }

    /**
     * Une a una Jam de Spotify y guarda su cola como playlist local.
     * Spotify no ofrece API pública para las Jams: se resuelve el enlace, se leen las pistas que la
     * página compartida expone (enlaces /track/ID) y se buscan en YouTube. Si la Jam no expone su
     * cola sin iniciar sesión, devuelve un error explicativo.
     */
    suspend fun importJam(
        link: String,
        database: DatabaseDao,
        onProgress: (done: Int, total: Int, currentSong: String) -> Unit,
    ): Result<Pair<PlaylistEntity, Int>> = withContext(Dispatchers.IO) {
        try {
            val url = Regex("""https?://\S+""").find(link.trim())?.value
                ?: return@withContext Result.failure(IllegalArgumentException("invalid_jam_link"))

            val (finalUrl, html) = httpGet(url)

            // Si el enlace acaba en una playlist o pista normal, se trata como tal.
            extractPlaylistId(finalUrl)?.let { return@withContext importPlaylist(finalUrl, database, onProgress = onProgress) }

            val ids = Regex("""(?:/track/|spotify:track:|spotify%3Atrack%3A)([a-zA-Z0-9]{22})""")
                .findAll(html + " " + finalUrl)
                .map { it.groupValues[1] }
                .distinct()
                .toList()
            if (ids.isEmpty()) {
                return@withContext Result.failure(Exception("jam_queue_hidden"))
            }

            val tracks = ids.mapIndexedNotNull { i, id ->
                onProgress(i, ids.size, "Spotify · ${i + 1}/${ids.size}")
                fetchTrack(id)
            }
            if (tracks.isEmpty()) {
                return@withContext Result.failure(Exception("jam_queue_hidden"))
            }

            val stamp = java.text.SimpleDateFormat("dd/MM HH:mm", java.util.Locale.getDefault()).format(java.util.Date())
            val playlistEntity = PlaylistEntity(
                name = "Jam · $stamp",
                bookmarkedAt = LocalDateTime.now(),
                isEditable = true,
            )

            val matched = mutableListOf<Pair<String, String?>>()
            val seenJamIds = HashSet<String>()
            tracks.forEachIndexed { index, track ->
                val query = if (track.artist.isNotBlank()) "${track.title} ${track.artist}" else track.title
                onProgress(matched.size, tracks.size, query)
                try {
                    val hit = YouTube.search(query, YouTube.SearchFilter.FILTER_SONG)
                        .getOrNull()?.items?.filterIsInstance<SongItem>()?.firstOrNull()
                    if (hit != null && seenJamIds.add(hit.id)) {
                        database.insert(hit.toMediaMetadata())
                        matched.add(hit.id to null)
                    }
                } catch (e: Exception) {
                    Timber.w(e, "Error finding Jam song on YouTube: $query")
                }
            }
            if (matched.isEmpty()) {
                return@withContext Result.failure(Exception("No tracks found"))
            }
            database.insert(playlistEntity)
            database.addSongsToPlaylist(
                Playlist(playlist = playlistEntity, songCount = 0, songThumbnails = emptyList()),
                matched,
            )
            Result.success(playlistEntity to matched.size)
        } catch (e: Exception) {
            Timber.e(e, "Error joining Spotify Jam")
            Result.failure(e)
        }
    }
}
