/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.component

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.metrolist.music.LocalDatabase
import com.metrolist.music.R
import com.metrolist.music.utils.SpotifyImporter
import kotlinx.coroutines.launch

@Composable
fun SpotifyImportDialog(
    isVisible: Boolean,
    onDismiss: () -> Unit,
    onSuccess: (playlistName: String, songCount: Int) -> Unit = { _, _ -> },
) {
    if (!isVisible) return

    val context = LocalContext.current
    val database = LocalDatabase.current
    val scope = rememberCoroutineScope()

    var inputUrl by remember { mutableStateOf("") }
    var isImporting by remember { mutableStateOf(false) }
    var currentSongTitle by remember { mutableStateOf("") }
    var progressDone by remember { mutableIntStateOf(0) }
    var progressTotal by remember { mutableIntStateOf(0) }

    DefaultDialog(
        onDismiss = {
            if (!isImporting) onDismiss()
        },
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(
                    painter = painterResource(R.drawable.playlist_add),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
                Text(stringResource(R.string.import_from_spotify))
            }
        },
        content = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp),
            ) {
                Text(
                    text = stringResource(R.string.spotify_playlist_url),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = inputUrl,
                    onValueChange = { inputUrl = it },
                    placeholder = { Text(stringResource(R.string.spotify_import_hint)) },
                    singleLine = true,
                    enabled = !isImporting,
                    modifier = Modifier.fillMaxWidth(),
                )

                AnimatedVisibility(visible = isImporting) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        if (progressTotal > 0) {
                            LinearProgressIndicator(
                                progress = { (progressDone.toFloat() / progressTotal.toFloat()).coerceIn(0f, 1f) },
                                modifier = Modifier.fillMaxWidth(),
                            )
                            Text(
                                text = "$progressDone / $progressTotal",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        } else {
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        }

                        if (currentSongTitle.isNotBlank()) {
                            Text(
                                text = currentSongTitle,
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        },
        buttons = {
            TextButton(
                onClick = onDismiss,
                enabled = !isImporting,
            ) {
                Text(stringResource(android.R.string.cancel))
            }
            Button(
                onClick = {
                    if (inputUrl.isBlank()) return@Button
                    isImporting = true
                    currentSongTitle = context.getString(R.string.spotify_import_searching)
                    progressDone = 0
                    progressTotal = 0

                    scope.launch {
                        val result = SpotifyImporter.importPlaylist(
                            urlOrId = inputUrl,
                            database = database,
                            coversDir = java.io.File(context.filesDir, "playlist_covers"),
                            onProgress = { done, total, song ->
                                progressDone = done
                                progressTotal = total
                                currentSongTitle = song
                            },
                        )

                        isImporting = false
                        result.onSuccess { (entity, count) ->
                            Toast.makeText(
                                context,
                                context.getString(R.string.spotify_import_done, count, progressTotal.coerceAtLeast(count)),
                                Toast.LENGTH_LONG,
                            ).show()
                            onSuccess(entity.name, count)
                            onDismiss()
                        }.onFailure { e ->
                            Toast.makeText(
                                context,
                                context.getString(R.string.playlist_import_error, e.localizedMessage ?: "Error"),
                                Toast.LENGTH_LONG,
                            ).show()
                        }
                    }
                },
                enabled = !isImporting && inputUrl.isNotBlank(),
            ) {
                if (isImporting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                } else {
                    Text(stringResource(R.string.import_playlist))
                }
            }
        },
    )
}
