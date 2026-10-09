/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.screens.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.metrolist.music.LocalDatabase
import com.metrolist.music.LocalPlayerAwareWindowInsets
import com.metrolist.music.R
import com.metrolist.music.constants.AddToPlaylistPosition
import com.metrolist.music.constants.AddToPlaylistPositionKey
import com.metrolist.music.constants.ShowCachedPlaylistKey
import com.metrolist.music.constants.ShowDownloadedPlaylistKey
import com.metrolist.music.constants.ShowLikedPlaylistKey
import com.metrolist.music.constants.ShowMostStatsPlaylistsKey
import com.metrolist.music.constants.ShowTopPlaylistKey
import com.metrolist.music.constants.ShowUploadedPlaylistKey
import com.metrolist.music.constants.ShufflePlaylistFirstKey
import com.metrolist.music.ui.component.EnumDialog
import com.metrolist.music.ui.component.IconButton
import com.metrolist.music.ui.component.Material3SettingsGroup
import com.metrolist.music.ui.component.Material3SettingsItem
import com.metrolist.music.ui.component.SpotifyImportDialog
import com.metrolist.music.ui.component.SpotifyJamDialog
import com.metrolist.music.ui.utils.backToMain
import com.metrolist.music.utils.rememberEnumPreference
import com.metrolist.music.utils.rememberPreference

private fun switchItem(
    icon: Painter,
    title: String,
    description: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) = Material3SettingsItem(
    icon = icon,
    title = { Text(title) },
    description = description?.let { text -> @Composable { Text(text) } },
    trailingContent = {
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            thumbContent = {
                Icon(
                    painter = painterResource(id = if (checked) R.drawable.check else R.drawable.close),
                    contentDescription = null,
                    modifier = Modifier.size(SwitchDefaults.IconSize),
                )
            },
        )
    },
    onClick = { onCheckedChange(!checked) },
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistSettings(navController: NavController) {
    val database = LocalDatabase.current

    val (addToPlaylistPosition, onAddToPlaylistPositionChange) =
        rememberEnumPreference(AddToPlaylistPositionKey, AddToPlaylistPosition.BEGINNING)
    val (shufflePlaylistFirst, onShufflePlaylistFirstChange) =
        rememberPreference(ShufflePlaylistFirstKey, defaultValue = false)
    val (showLikedPlaylist, onShowLikedPlaylistChange) =
        rememberPreference(ShowLikedPlaylistKey, defaultValue = true)
    val (showDownloadedPlaylist, onShowDownloadedPlaylistChange) =
        rememberPreference(ShowDownloadedPlaylistKey, defaultValue = true)
    val (showTopPlaylist, onShowTopPlaylistChange) =
        rememberPreference(ShowTopPlaylistKey, defaultValue = true)
    val (showCachedPlaylist, onShowCachedPlaylistChange) =
        rememberPreference(ShowCachedPlaylistKey, defaultValue = true)
    val (showUploadedPlaylist, onShowUploadedPlaylistChange) =
        rememberPreference(ShowUploadedPlaylistKey, defaultValue = true)
    val (showMostStatsPlaylists, onShowMostStatsPlaylistsChange) =
        rememberPreference(ShowMostStatsPlaylistsKey, defaultValue = true)

    var showAddToPlaylistPositionDialog by rememberSaveable { mutableStateOf(false) }
    var showSpotifyImport by rememberSaveable { mutableStateOf(false) }
    var showSpotifyJam by rememberSaveable { mutableStateOf(false) }

    // Al desactivar las playlists de estadísticas se borran de la base de datos.
    LaunchedEffect(showMostStatsPlaylists) {
        if (!showMostStatsPlaylists) {
            database.withTransaction {
                clearPlaylist(com.metrolist.music.db.entities.PlaylistEntity.WEEKLY_MOST_PLAYLIST_ID)
                clearPlaylist(com.metrolist.music.db.entities.PlaylistEntity.MONTHLY_MOST_PLAYLIST_ID)
                delete(
                    com.metrolist.music.db.entities.PlaylistEntity(
                        id = com.metrolist.music.db.entities.PlaylistEntity.WEEKLY_MOST_PLAYLIST_ID,
                        name = "",
                    ),
                )
                delete(
                    com.metrolist.music.db.entities.PlaylistEntity(
                        id = com.metrolist.music.db.entities.PlaylistEntity.MONTHLY_MOST_PLAYLIST_ID,
                        name = "",
                    ),
                )
            }
        }
    }

    if (showAddToPlaylistPositionDialog) {
        EnumDialog(
            onDismiss = { showAddToPlaylistPositionDialog = false },
            onSelect = {
                onAddToPlaylistPositionChange(it)
                showAddToPlaylistPositionDialog = false
            },
            title = stringResource(R.string.add_to_playlist_position),
            current = addToPlaylistPosition,
            values = AddToPlaylistPosition.entries,
            valueText = {
                when (it) {
                    AddToPlaylistPosition.BEGINNING -> stringResource(R.string.playlist_position_beginning)
                    AddToPlaylistPosition.END -> stringResource(R.string.playlist_position_end)
                }
            },
            valueDescription = {
                when (it) {
                    AddToPlaylistPosition.BEGINNING -> stringResource(R.string.playlist_position_beginning_desc)
                    AddToPlaylistPosition.END -> stringResource(R.string.playlist_position_end_desc)
                }
            },
        )
    }

    SpotifyImportDialog(
        isVisible = showSpotifyImport,
        onDismiss = { showSpotifyImport = false },
    )
    SpotifyJamDialog(
        isVisible = showSpotifyJam,
        onDismiss = { showSpotifyJam = false },
    )

    Column(
        Modifier
            .windowInsetsPadding(LocalPlayerAwareWindowInsets.current)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
    ) {
        Material3SettingsGroup(
            title = stringResource(R.string.spotify_hub_title),
            items =
                listOf(
                    Material3SettingsItem(
                        icon = painterResource(R.drawable.playlist_add),
                        title = { Text(stringResource(R.string.import_from_spotify)) },
                        description = { Text(stringResource(R.string.spotify_import_card_desc)) },
                        onClick = { showSpotifyImport = true },
                    ),
                    Material3SettingsItem(
                        icon = painterResource(R.drawable.group_add),
                        title = { Text(stringResource(R.string.spotify_jam_card_title)) },
                        description = { Text(stringResource(R.string.spotify_jam_card_desc)) },
                        onClick = { showSpotifyJam = true },
                    ),
                ),
        )

        Spacer(modifier = Modifier.height(27.dp))

        Material3SettingsGroup(
            title = stringResource(R.string.playlists),
            items =
                listOf(
                    Material3SettingsItem(
                        icon = painterResource(R.drawable.playlist_add),
                        title = { Text(stringResource(R.string.add_to_playlist_position)) },
                        description = {
                            Text(
                                when (addToPlaylistPosition) {
                                    AddToPlaylistPosition.BEGINNING -> stringResource(R.string.playlist_position_beginning)
                                    AddToPlaylistPosition.END -> stringResource(R.string.playlist_position_end)
                                },
                            )
                        },
                        onClick = { showAddToPlaylistPositionDialog = true },
                    ),
                    switchItem(
                        icon = painterResource(R.drawable.shuffle),
                        title = stringResource(R.string.shuffle_playlist_first),
                        description = stringResource(R.string.shuffle_playlist_first_desc),
                        checked = shufflePlaylistFirst,
                        onCheckedChange = onShufflePlaylistFirstChange,
                    ),
                ),
        )

        Spacer(modifier = Modifier.height(27.dp))

        Material3SettingsGroup(
            title = stringResource(R.string.auto_playlists),
            items =
                listOf(
                    switchItem(
                        icon = painterResource(R.drawable.favorite),
                        title = stringResource(R.string.show_liked_playlist),
                        checked = showLikedPlaylist,
                        onCheckedChange = onShowLikedPlaylistChange,
                    ),
                    switchItem(
                        icon = painterResource(R.drawable.offline),
                        title = stringResource(R.string.show_downloaded_playlist),
                        checked = showDownloadedPlaylist,
                        onCheckedChange = onShowDownloadedPlaylistChange,
                    ),
                    switchItem(
                        icon = painterResource(R.drawable.trending_up),
                        title = stringResource(R.string.show_top_playlist),
                        checked = showTopPlaylist,
                        onCheckedChange = onShowTopPlaylistChange,
                    ),
                    switchItem(
                        icon = painterResource(R.drawable.cached),
                        title = stringResource(R.string.show_cached_playlist),
                        checked = showCachedPlaylist,
                        onCheckedChange = onShowCachedPlaylistChange,
                    ),
                    switchItem(
                        icon = painterResource(R.drawable.backup),
                        title = stringResource(R.string.show_uploaded_playlist),
                        checked = showUploadedPlaylist,
                        onCheckedChange = onShowUploadedPlaylistChange,
                    ),
                    switchItem(
                        icon = painterResource(R.drawable.stats),
                        title = stringResource(R.string.show_most_stats_playlists),
                        description = stringResource(R.string.show_most_stats_playlists_desc),
                        checked = showMostStatsPlaylists,
                        onCheckedChange = onShowMostStatsPlaylistsChange,
                    ),
                ),
        )

        Spacer(modifier = Modifier.height(16.dp))
    }

    TopAppBar(
        title = { Text(stringResource(R.string.playlists)) },
        navigationIcon = {
            IconButton(
                onClick = navController::navigateUp,
                onLongClick = navController::backToMain,
            ) {
                Icon(
                    painterResource(R.drawable.arrow_back),
                    contentDescription = null,
                )
            }
        },
    )
}
