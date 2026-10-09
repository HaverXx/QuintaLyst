package com.metrolist.music.ui.component

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.metrolist.music.LocalDatabase
import com.metrolist.music.R
import com.metrolist.music.utils.SpotifyImporter
import kotlinx.coroutines.launch

private val SpotifyGreen = Color(0xFF1ED760)

/**
 * Bloque destacado de Spotify: dos tarjetas de cristal, una para importar playlists y otra para
 * unirse a una Jam. Se muestra arriba del todo en Ajustes para que se vea a simple vista.
 */
@Composable
fun SpotifyHubCards(modifier: Modifier = Modifier) {
    var showImport by rememberSaveable { mutableStateOf(false) }
    var showJam by rememberSaveable { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.spotify_hub_title),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 8.dp, top = 8.dp),
        )
        Row(
            modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Max),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            HubTile(
                iconRes = R.drawable.playlist_add,
                title = stringResource(R.string.import_from_spotify),
                description = stringResource(R.string.spotify_import_card_desc),
                onClick = { showImport = true },
            )
            HubTile(
                iconRes = R.drawable.group_add,
                title = stringResource(R.string.spotify_jam_card_title),
                description = stringResource(R.string.spotify_jam_card_desc),
                onClick = { showJam = true },
            )
        }
    }

    SpotifyImportDialog(
        isVisible = showImport,
        onDismiss = { showImport = false },
    )
    SpotifyJamDialog(
        isVisible = showJam,
        onDismiss = { showJam = false },
    )
}

@Composable
private fun RowScope.HubTile(
    iconRes: Int,
    title: String,
    description: String,
    onClick: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .weight(1f)
                .fillMaxHeight()
                .glassCard(24.dp, refractive = true)
                .clickable(onClick = onClick)
                .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        GlassIconTile(tint = SpotifyGreen, strong = true, modifier = Modifier.size(44.dp)) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(26.dp),
            )
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun SpotifyJamDialog(
    isVisible: Boolean,
    onDismiss: () -> Unit,
) {
    if (!isVisible) return

    val context = LocalContext.current
    val database = LocalDatabase.current
    val scope = rememberCoroutineScope()

    var link by remember { mutableStateOf("") }
    var joining by remember { mutableStateOf(false) }
    var done by remember { mutableIntStateOf(0) }
    var total by remember { mutableIntStateOf(0) }
    var current by remember { mutableStateOf("") }

    DefaultDialog(
        onDismiss = { if (!joining) onDismiss() },
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(
                    painter = painterResource(R.drawable.group_add),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
                Text(stringResource(R.string.spotify_jam_dialog_title))
            }
        },
        content = {
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp),
            ) {
                Text(
                    text = stringResource(R.string.spotify_jam_help),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = link,
                    onValueChange = { link = it },
                    placeholder = { Text(stringResource(R.string.spotify_jam_hint)) },
                    singleLine = true,
                    enabled = !joining,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (joining) {
                    Spacer(Modifier.height(16.dp))
                    if (total > 0) {
                        LinearProgressIndicator(
                            progress = { (done.toFloat() / total).coerceIn(0f, 1f) },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Text(
                            text = "$done / $total",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    } else {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }
                    if (current.isNotBlank()) {
                        Text(
                            text = current,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        },
        buttons = {
            TextButton(onClick = onDismiss, enabled = !joining) {
                Text(stringResource(android.R.string.cancel))
            }
            Button(
                enabled = !joining && link.isNotBlank(),
                onClick = {
                    joining = true
                    done = 0
                    total = 0
                    scope.launch {
                        val result =
                            SpotifyImporter.importJam(link, database) { d, t, song ->
                                done = d
                                total = t
                                current = song
                            }
                        joining = false
                        result
                            .onSuccess { (_, count) ->
                                Toast.makeText(
                                    context,
                                    context.getString(R.string.spotify_jam_done, count),
                                    Toast.LENGTH_LONG,
                                ).show()
                                onDismiss()
                            }.onFailure { e ->
                                val msg =
                                    if (e.message == "jam_queue_hidden") {
                                        context.getString(R.string.spotify_jam_hidden)
                                    } else {
                                        context.getString(R.string.playlist_import_error, e.localizedMessage ?: "Error")
                                    }
                                Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                            }
                    }
                },
            ) {
                if (joining) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                } else {
                    Text(stringResource(R.string.spotify_jam_join))
                }
            }
        },
    )
}
