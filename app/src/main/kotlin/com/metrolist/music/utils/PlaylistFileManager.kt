/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.utils

import android.content.Context
import android.net.Uri
import com.metrolist.innertube.YouTube
import com.metrolist.innertube.models.SongItem
import com.metrolist.music.db.DatabaseDao
import com.metrolist.music.db.entities.ArtistEntity
import com.metrolist.music.db.entities.Playlist
import com.metrolist.music.db.entities.PlaylistEntity
import com.metrolist.music.db.entities.PlaylistSong
import com.metrolist.music.db.entities.SongArtistMap
import com.metrolist.music.db.entities.SongEntity
import com.metrolist.music.models.toMediaMetadata
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import timber.log.Timber

object PlaylistFileManager {

    suspend fun exportPlaylistToJson(
        context: Context,
        playlist: Playlist,
        songs: List<PlaylistSong>,
        uri: Uri,
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val root = JSONObject()
            root.put("version", 1)
            root.put("name", playlist.playlist.name)

            val songsArray = JSONArray()
            songs.forEach { s ->
                val songObj = JSONObject()
                songObj.put("id", s.song.song.id)
                songObj.put("title", s.song.song.title)
                songObj.put("duration", s.song.song.duration)
                val artistsArray = JSONArray()
                s.song.artists.forEach { a ->
                    val aObj = JSONObject()
                    aObj.put("id", a.id)
                    aObj.put("name", a.name)
                    artistsArray.put(aObj)
                }
                songObj.put("artists", artistsArray)
                songsArray.put(songObj)
            }
            root.put("songs", songsArray)

            context.contentResolver.openOutputStream(uri)?.use { out ->
                out.write(root.toString(2).toByteArray(Charsets.UTF_8))
            } ?: return@withContext Result.failure(Exception("Failed to open output stream for export"))

            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Error exporting playlist to JSON")
            Result.failure(e)
        }
    }

    suspend fun importPlaylistFromFile(
        context: Context,
        uri: Uri,
        database: DatabaseDao,
    ): Result<Pair<PlaylistEntity, Int>> = withContext(Dispatchers.IO) {
        try {
            val content = context.contentResolver.openInputStream(uri)?.use {
                it.bufferedReader().readText()
            } ?: return@withContext Result.failure(Exception("Could not read file"))

            val trimmed = content.trim()
            if (trimmed.startsWith("{")) {
                // JSON format
                val root = JSONObject(trimmed)
                val name = root.optString("name", "Imported Playlist")
                val songsArray = root.optJSONArray("songs") ?: JSONArray()

                val playlistEntity = PlaylistEntity(name = name)
                database.insert(playlistEntity)

                val songPairs = mutableListOf<Pair<String, String?>>()
                for (i in 0 until songsArray.length()) {
                    val sObj = songsArray.getJSONObject(i)
                    val id = sObj.optString("id")
                    val title = sObj.optString("title")
                    val duration = sObj.optInt("duration", 0)

                    if (id.isNotBlank()) {
                        val existing = database.getSongByIdBlocking(id)
                        if (existing == null && title.isNotBlank()) {
                            val songEntity = SongEntity(
                                id = id,
                                title = title,
                                duration = duration,
                            )
                            database.insert(songEntity)
                            val artArray = sObj.optJSONArray("artists")
                            if (artArray != null) {
                                for (j in 0 until artArray.length()) {
                                    val aObj = artArray.getJSONObject(j)
                                    val aId = aObj.optString("id", "")
                                    val aName = aObj.optString("name", "")
                                    if (aName.isNotBlank()) {
                                        val artistEntity = ArtistEntity(
                                            id = aId.ifBlank { ArtistEntity.generateArtistId() },
                                            name = aName,
                                        )
                                        database.insert(artistEntity)
                                        database.insert(SongArtistMap(songId = id, artistId = artistEntity.id, position = j))
                                    }
                                }
                            }
                        }
                        songPairs.add(id to null)
                    }
                }

                if (songPairs.isNotEmpty()) {
                    database.addSongsToPlaylist(Playlist(playlist = playlistEntity, songCount = 0, songThumbnails = emptyList()), songPairs)
                }

                Result.success(playlistEntity to songPairs.size)
            } else {
                // M3U format
                val lines = trimmed.lines()
                val playlistName = lines.firstOrNull { it.startsWith("#PLAYLIST:") }
                    ?.removePrefix("#PLAYLIST:")?.trim()
                    ?: "Imported Playlist"

                val playlistEntity = PlaylistEntity(name = playlistName)
                database.insert(playlistEntity)

                val songPairs = mutableListOf<Pair<String, String?>>()
                var lastTitle: String? = null

                for (line in lines) {
                    val l = line.trim()
                    if (l.startsWith("#EXTINF:")) {
                        val comma = l.indexOf(',')
                        if (comma != -1) {
                            lastTitle = l.substring(comma + 1).trim()
                        }
                    } else if (l.isNotBlank() && !l.startsWith("#")) {
                        val videoId = when {
                            l.contains("v=") -> l.substringAfter("v=").substringBefore('&')
                            l.contains("youtu.be/") -> l.substringAfter("youtu.be/").substringBefore('?')
                            l.length == 11 && !l.contains(" ") -> l
                            else -> null
                        }

                        if (videoId != null) {
                            songPairs.add(videoId to null)
                        } else if (!lastTitle.isNullOrBlank()) {
                            try {
                                val searchResult = YouTube.search(lastTitle, YouTube.SearchFilter.FILTER_SONG)
                                    .getOrNull()?.items?.filterIsInstance<SongItem>()?.firstOrNull()
                                if (searchResult != null) {
                                    database.insert(searchResult.toMediaMetadata())
                                    songPairs.add(searchResult.id to null)
                                }
                            } catch (e: Exception) {
                                Timber.w(e, "Could not resolve M3U track: $lastTitle")
                            }
                        }
                        lastTitle = null
                    }
                }

                if (songPairs.isNotEmpty()) {
                    database.addSongsToPlaylist(Playlist(playlist = playlistEntity, songCount = 0, songThumbnails = emptyList()), songPairs)
                }

                Result.success(playlistEntity to songPairs.size)
            }
        } catch (e: Exception) {
            Timber.e(e, "Error importing playlist from file")
            Result.failure(e)
        }
    }
}
