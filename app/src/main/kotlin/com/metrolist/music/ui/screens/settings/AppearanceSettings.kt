/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.screens.settings

import com.metrolist.music.ui.component.GlassOutlinedButton
import com.metrolist.music.ui.component.GlassButton
import android.os.Build
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderState
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.metrolist.music.LocalPlayerAwareWindowInsets
import com.metrolist.music.R
import com.metrolist.music.constants.ChipSortTypeKey
import com.metrolist.music.constants.CropAlbumArtKey
import com.metrolist.music.constants.DefaultOpenTabKey
import com.metrolist.music.constants.DensityScale
import com.metrolist.music.constants.DensityScaleKey
import com.metrolist.music.constants.DynamicThemeKey
import com.metrolist.music.constants.EnableDynamicIconKey
import com.metrolist.music.constants.EnableHighRefreshRateKey
import com.metrolist.music.constants.EnableLandscapeScalingKey
import com.metrolist.music.constants.ExperimentalLyricsKey
import com.metrolist.music.constants.ForceBottomNavBarKey
import com.metrolist.music.constants.GridItemSize
import com.metrolist.music.constants.GridItemsSizeKey
import com.metrolist.music.constants.HidePlayerThumbnailKey
import com.metrolist.music.constants.HideStatusBarOnFullscreenKey
import com.metrolist.music.constants.LibraryFilter
import com.metrolist.music.constants.LiquidGlassKey
import com.metrolist.music.constants.RetroDynamicLabelKey
import com.metrolist.music.constants.RetroStyleKey
import com.metrolist.music.constants.PlayerGlassOpacityKey
import com.metrolist.music.constants.ListenTogetherInTopBarKey
import com.metrolist.music.constants.LyricsAnimationStyle
import com.metrolist.music.constants.LyricsAnimationStyleKey
import com.metrolist.music.constants.LyricsClickKey
import com.metrolist.music.constants.LyricsGlowEffectKey
import com.metrolist.music.constants.LyricsLineSpacingKey
import com.metrolist.music.constants.LyricsScrollKey
import com.metrolist.music.constants.LyricsTextPositionKey
import com.metrolist.music.constants.LyricsTextSizeKey
import com.metrolist.music.constants.MiniPlayerBackgroundStyle
import com.metrolist.music.constants.MiniPlayerBackgroundStyleKey
import com.metrolist.music.constants.PlayerBackgroundStyle
import com.metrolist.music.constants.PlayerBackgroundStyleKey
import com.metrolist.music.constants.PlayerButtonsStyle
import com.metrolist.music.constants.PlayerButtonsStyleKey
import com.metrolist.music.constants.PureBlackMiniPlayerKey
import com.metrolist.music.constants.RespectAgentPositioningKey
import com.metrolist.music.constants.SelectedThemeColorKey
import com.metrolist.music.constants.ShowCachedPlaylistKey
import com.metrolist.music.constants.ShowDownloadedPlaylistKey
import com.metrolist.music.constants.ShowLikedPlaylistKey
import com.metrolist.music.constants.ShowTopPlaylistKey
import com.metrolist.music.constants.ShowUploadedPlaylistKey
import com.metrolist.music.constants.SliderStyle
import com.metrolist.music.constants.SliderStyleKey
import com.metrolist.music.constants.SlimNavBarKey
import com.metrolist.music.constants.SquigglySliderKey
import com.metrolist.music.constants.SwipeSensitivityKey
import com.metrolist.music.constants.SwipeThumbnailKey
import com.metrolist.music.constants.SwipeToRemoveSongKey
import com.metrolist.music.constants.SwipeToSongKey
import com.metrolist.music.constants.UseNewMiniPlayerDesignKey
import com.metrolist.music.constants.UseNewPlayerDesignKey
import com.metrolist.music.constants.DynamicBackgroundKey
import com.metrolist.music.constants.DynamicBgBlurKey
import com.metrolist.music.constants.DynamicBgIntensityKey
import com.metrolist.music.constants.DynamicBgSpeedKey
import com.metrolist.music.constants.ThumbnailCornerRadiusKey
import com.metrolist.music.constants.CustomAppTitleKey
import com.metrolist.music.constants.ShowSpeedDialKey
import com.metrolist.music.constants.ShowQuickPicksKey
import com.metrolist.music.constants.ShowDailyDiscoverKey
import com.metrolist.music.constants.ShowKeepListeningKey
import com.metrolist.music.constants.ShowAccountPlaylistsKey
import com.metrolist.music.constants.ShowForgottenFavoritesKey
import com.metrolist.music.constants.ShowFromTheCommunityKey
import com.metrolist.music.constants.ShowMoodAndGenresKey
import com.metrolist.music.constants.DynamicBackgroundStyle
import com.metrolist.music.constants.DynamicBackgroundStyleKey
import com.metrolist.music.constants.CustomAppIconPathKey
import com.metrolist.music.constants.CustomAppIconTimestampKey
import com.metrolist.music.constants.HomeWallpaperKey
import com.metrolist.music.constants.HomeWallpaperOpacityKey
import com.metrolist.music.constants.HomeWallpaperBlurKey
import com.metrolist.music.utils.IconUtils
import java.io.File
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.width
import androidx.compose.ui.layout.ContentScale
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedButton
import coil3.compose.AsyncImage
import com.metrolist.music.ui.component.DefaultDialog
import com.metrolist.music.ui.component.EnumDialog
import com.metrolist.music.ui.component.IconButton
import com.metrolist.music.ui.component.Material3SettingsGroup
import com.metrolist.music.ui.component.Material3SettingsItem
import com.metrolist.music.ui.component.PlayerSliderTrack
import com.metrolist.music.ui.component.SquigglySlider
import com.metrolist.music.ui.component.WavySlider
import com.metrolist.music.ui.theme.DefaultThemeColor
import com.metrolist.music.ui.theme.PlayerSliderColors
import com.metrolist.music.ui.utils.backToMain
import com.metrolist.music.utils.rememberEnumPreference
import com.metrolist.music.utils.rememberPreference
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppearanceSettings(
    navController: NavController,
    snackbarHostState: SnackbarHostState,
) {
    val (dynamicTheme, onDynamicThemeChange) =
        rememberPreference(
            DynamicThemeKey,
            defaultValue = true,
        )
    val (enableDynamicIcon, onEnableDynamicIconPrefChange) =
        rememberPreference(
            EnableDynamicIconKey,
            defaultValue = true,
        )
    val iconContext = LocalContext.current
    val onEnableDynamicIconChange: (Boolean) -> Unit = { newValue ->
        onEnableDynamicIconPrefChange(newValue)
        IconUtils.setIcon(iconContext, newValue)
    }
    val (enableHighRefreshRate, onEnableHighRefreshRateChange) =
        rememberPreference(
            EnableHighRefreshRateKey,
            defaultValue = true,
        )
    val (enableLandscapeScaling, onEnableLandscapeScalingChange) =
        rememberPreference(
            EnableLandscapeScalingKey,
            defaultValue = false,
        )
    val (selectedThemeColorInt, onSelectedThemeColorChange) =
        rememberPreference(
            SelectedThemeColorKey,
            defaultValue = DefaultThemeColor.toArgb(),
        )
    // Check if user has selected a custom color (not the default/dynamic color)
    val isUsingCustomColor = selectedThemeColorInt != DefaultThemeColor.toArgb()

    val (customAppTitle, onCustomAppTitleChange) = rememberPreference(CustomAppTitleKey, defaultValue = "")
    var showCustomAppTitleDialog by rememberSaveable { mutableStateOf(false) }
    var showAppIconDialog by rememberSaveable { mutableStateOf(false) }
    var showMenuSectionsDialog by rememberSaveable { mutableStateOf(false) }

    val (customAppIconPath, onCustomAppIconPathChange) = rememberPreference(CustomAppIconPathKey, defaultValue = "")
    val (customAppIconTimestamp, onCustomAppIconTimestampChange) = rememberPreference(CustomAppIconTimestampKey, defaultValue = 0L)
    val customIconFile = remember(customAppIconPath, customAppIconTimestamp) {
        IconUtils.getCustomIconFile(iconContext, customAppIconPath)?.takeIf { it.exists() }
    }

    val pickImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val savedPath = IconUtils.saveCustomIconFromUri(iconContext, uri)
            if (savedPath != null) {
                val newTimestamp = System.currentTimeMillis()
                onCustomAppIconPathChange(savedPath)
                onCustomAppIconTimestampChange(newTimestamp)
                Toast.makeText(iconContext, iconContext.getString(R.string.custom_app_icon_gallery), Toast.LENGTH_SHORT).show()
            }
        }
    }

    val (showSpeedDial, onShowSpeedDialChange) = rememberPreference(ShowSpeedDialKey, defaultValue = true)
    val (showQuickPicks, onShowQuickPicksChange) = rememberPreference(ShowQuickPicksKey, defaultValue = true)
    val (showDailyDiscover, onShowDailyDiscoverChange) = rememberPreference(ShowDailyDiscoverKey, defaultValue = true)
    val (showKeepListening, onShowKeepListeningChange) = rememberPreference(ShowKeepListeningKey, defaultValue = true)
    val (showAccountPlaylists, onShowAccountPlaylistsChange) = rememberPreference(ShowAccountPlaylistsKey, defaultValue = true)
    val (showForgottenFavorites, onShowForgottenFavoritesChange) = rememberPreference(ShowForgottenFavoritesKey, defaultValue = true)
    val (showFromTheCommunity, onShowFromTheCommunityChange) = rememberPreference(ShowFromTheCommunityKey, defaultValue = true)
    val (showMoodAndGenres, onShowMoodAndGenresChange) = rememberPreference(ShowMoodAndGenresKey, defaultValue = true)

    val (useNewPlayerDesign, onUseNewPlayerDesignChange) =
        rememberPreference(
            UseNewPlayerDesignKey,
            defaultValue = true,
        )
    val (miniPlayerBackground, onMiniPlayerBackgroundChange) =
        rememberEnumPreference(
            MiniPlayerBackgroundStyleKey,
            defaultValue = MiniPlayerBackgroundStyle.DEFAULT,
        )

    val availableMiniPlayerBackgroundStyles =
        MiniPlayerBackgroundStyle.entries.filter {
            it != MiniPlayerBackgroundStyle.BLUR || Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
        }

    var showMiniPlayerBackgroundDialog by rememberSaveable { mutableStateOf(false) }

    val (liquidGlass, onLiquidGlassChange) =
        rememberPreference(
            LiquidGlassKey,
            defaultValue = true,
        )
    val (retroStyle, onRetroStyleChange) =
        rememberPreference(
            RetroStyleKey,
            defaultValue = false,
        )
    val (retroDynamicLabel, onRetroDynamicLabelChange) =
        rememberPreference(
            RetroDynamicLabelKey,
            defaultValue = true,
        )
    val appStyle = if (retroStyle) 2 else if (liquidGlass) 1 else 0
    val onAppStyleSelected: (Int) -> Unit = { style ->
        onRetroStyleChange(style == 2)
        onLiquidGlassChange(style == 1)
    }
    val (miniPlayerDim, onMiniPlayerDimChange) =
        rememberPreference(com.metrolist.music.constants.MiniPlayerDimKey, defaultValue = 0f)
    val (playerGlassOpacity, onPlayerGlassOpacityChange) =
        rememberPreference(PlayerGlassOpacityKey, defaultValue = 0.55f)
    val (useNewMiniPlayerDesign, onUseNewMiniPlayerDesignChange) =
        rememberPreference(
            UseNewMiniPlayerDesignKey,
            defaultValue = true,
        )
    val (hidePlayerThumbnail, onHidePlayerThumbnailChange) =
        rememberPreference(
            HidePlayerThumbnailKey,
            defaultValue = false,
        )
    val (cropAlbumArt, onCropAlbumArtChange) =
        rememberPreference(
            CropAlbumArtKey,
            defaultValue = false,
        )
    val (playerBackground, onPlayerBackgroundChange) =
        rememberEnumPreference(
            PlayerBackgroundStyleKey,
            defaultValue = PlayerBackgroundStyle.DEFAULT,
        )

    val (defaultOpenTab, onDefaultOpenTabChange) =
        rememberEnumPreference(
            DefaultOpenTabKey,
            defaultValue = NavigationTab.HOME,
        )
    val (playerButtonsStyle, onPlayerButtonsStyleChange) =
        rememberEnumPreference(
            PlayerButtonsStyleKey,
            defaultValue = PlayerButtonsStyle.DEFAULT,
        )
    val (lyricsPosition, onLyricsPositionChange) =
        rememberEnumPreference(
            LyricsTextPositionKey,
            defaultValue = LyricsPosition.CENTER,
        )
    val (lyricsClick, onLyricsClickChange) = rememberPreference(LyricsClickKey, defaultValue = true)
    val (lyricsScroll, onLyricsScrollChange) =
        rememberPreference(
            LyricsScrollKey,
            defaultValue = true,
        )
    val (hideStatusBarOnFullscreen, onHideStatusBarOnFullscreenChange) =
        rememberPreference(
            HideStatusBarOnFullscreenKey,
            defaultValue = false,
        )
    val (respectAgentPositioning, onRespectAgentPositioningChange) = rememberPreference(RespectAgentPositioningKey, defaultValue = true)
    val (experimentalLyrics, onExperimentalLyricsChange) = rememberPreference(ExperimentalLyricsKey, defaultValue = true)

    val (lyricsGlowEffect, onLyricsGlowEffectChange) = rememberPreference(LyricsGlowEffectKey, defaultValue = false)
    val (lyricsAnimationStyle, onLyricsAnimationStyleChange) =
        rememberEnumPreference(
            LyricsAnimationStyleKey,
            defaultValue = LyricsAnimationStyle.FADE,
        )
    val (lyricsTextSize, onLyricsTextSizeChange) = rememberPreference(LyricsTextSizeKey, defaultValue = 24f)
    val (lyricsLineSpacing, onLyricsLineSpacingChange) = rememberPreference(LyricsLineSpacingKey, defaultValue = 1.2f)

    var showExperimentalLyricsBetaDialog by remember { mutableStateOf(false) }
    var showLyricsAnimationStyleDialog by remember { mutableStateOf(false) }
    var showLyricsTextSizeDialog by remember { mutableStateOf(false) }
    var showLyricsLineSpacingDialog by remember { mutableStateOf(false) }

    val (sliderStyle, onSliderStyleChange) =
        rememberEnumPreference(
            SliderStyleKey,
            defaultValue = SliderStyle.DEFAULT,
        )
    val (squigglySlider, onSquigglySliderChange) =
        rememberPreference(
            SquigglySliderKey,
            defaultValue = false,
        )
    val (swipeThumbnail, onSwipeThumbnailChange) =
        rememberPreference(
            SwipeThumbnailKey,
            defaultValue = true,
        )
    val (swipeSensitivity, onSwipeSensitivityChange) =
        rememberPreference(
            SwipeSensitivityKey,
            defaultValue = 0.73f,
        )
    val (gridItemSize, onGridItemSizeChange) =
        rememberEnumPreference(
            GridItemsSizeKey,
            defaultValue = GridItemSize.SMALL,
        )

    val (forceBottomNav, onForceBottomNavChange) =
        rememberPreference(
            ForceBottomNavBarKey,
            defaultValue = false,
        )
    val (slimNav, onSlimNavChange) =
        rememberPreference(
            SlimNavBarKey,
            defaultValue = false,
        )

    // Image corner radius pref (stored as Dp value as float)
    val (thumbnailCornerRadiusPref, onThumbnailCornerRadiusChange) = rememberPreference(ThumbnailCornerRadiusKey, defaultValue = 24f)

    val (densityScale, onDensityScaleChange) = rememberPreference(DensityScaleKey, defaultValue = 1f)
    var showDensityScaleDialog by rememberSaveable { mutableStateOf(false) }

    val (listenTogetherInTopBar, onListenTogetherInTopBarChange) =
        rememberPreference(
            ListenTogetherInTopBarKey,
            defaultValue = true,
        )

    val (swipeToSong, onSwipeToSongChange) =
        rememberPreference(
            SwipeToSongKey,
            defaultValue = false,
        )

    val (swipeToRemoveSong, onSwipeToRemoveSongChange) =
        rememberPreference(
            SwipeToRemoveSongKey,
            defaultValue = false,
        )

    val (showLikedPlaylist, onShowLikedPlaylistChange) =
        rememberPreference(
            ShowLikedPlaylistKey,
            defaultValue = true,
        )
    val (showDownloadedPlaylist, onShowDownloadedPlaylistChange) =
        rememberPreference(
            ShowDownloadedPlaylistKey,
            defaultValue = true,
        )
    val (showTopPlaylist, onShowTopPlaylistChange) =
        rememberPreference(
            ShowTopPlaylistKey,
            defaultValue = true,
        )
    val (showCachedPlaylist, onShowCachedPlaylistChange) =
        rememberPreference(
            ShowCachedPlaylistKey,
            defaultValue = true,
        )
    val (showUploadedPlaylist, onShowUploadedPlaylistChange) =
        rememberPreference(
            ShowUploadedPlaylistKey,
            defaultValue = true,
        )

    val availableBackgroundStyles =
        PlayerBackgroundStyle.entries.filter {
            it != PlayerBackgroundStyle.BLUR || Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
        }

    val (defaultChip, onDefaultChipChange) =
        rememberEnumPreference(
            key = ChipSortTypeKey,
            defaultValue = LibraryFilter.LIBRARY,
        )

    var showSliderOptionDialog by rememberSaveable {
        mutableStateOf(false)
    }

    var showPlayerBackgroundDialog by rememberSaveable {
        mutableStateOf(false)
    }

    var showPlayerButtonsStyleDialog by rememberSaveable {
        mutableStateOf(false)
    }

    var showLyricsPositionDialog by rememberSaveable {
        mutableStateOf(false)
    }

    if (showLyricsPositionDialog) {
        EnumDialog(
            onDismiss = { showLyricsPositionDialog = false },
            onSelect = {
                onLyricsPositionChange(it)
                showLyricsPositionDialog = false
            },
            title = stringResource(R.string.lyrics_text_position),
            current = lyricsPosition,
            values = LyricsPosition.values().toList(),
            valueText = {
                when (it) {
                    LyricsPosition.LEFT -> stringResource(R.string.left)
                    LyricsPosition.CENTER -> stringResource(R.string.center)
                    LyricsPosition.RIGHT -> stringResource(R.string.right)
                }
            },
        )
    }

    if (showLyricsAnimationStyleDialog) {
        EnumDialog(
            onDismiss = { showLyricsAnimationStyleDialog = false },
            onSelect = {
                onLyricsAnimationStyleChange(it)
                showLyricsAnimationStyleDialog = false
            },
            title = stringResource(R.string.lyrics_animation_style_title),
            current = lyricsAnimationStyle,
            values = LyricsAnimationStyle.values().toList(),
            valueText = {
                when (it) {
                    LyricsAnimationStyle.NONE -> stringResource(R.string.lyrics_animation_none)
                    LyricsAnimationStyle.FADE -> stringResource(R.string.lyrics_animation_fade)
                    LyricsAnimationStyle.GLOW -> stringResource(R.string.lyrics_animation_glow)
                    LyricsAnimationStyle.SLIDE -> stringResource(R.string.lyrics_animation_slide)
                    LyricsAnimationStyle.KARAOKE -> stringResource(R.string.lyrics_animation_karaoke)
                    LyricsAnimationStyle.APPLE -> stringResource(R.string.lyrics_animation_apple)
                }
            },
        )
    }

    if (showLyricsTextSizeDialog) {
        var tempTextSize by remember { mutableFloatStateOf(lyricsTextSize) }

        DefaultDialog(
            onDismiss = {
                tempTextSize = lyricsTextSize
                showLyricsTextSizeDialog = false
            },
            buttons = {
                TextButton(
                    onClick = {
                        tempTextSize = 24f
                    },
                ) {
                    Text(stringResource(R.string.reset))
                }

                Spacer(modifier = Modifier.weight(1f))

                TextButton(
                    onClick = {
                        tempTextSize = lyricsTextSize
                        showLyricsTextSizeDialog = false
                    },
                ) {
                    Text(stringResource(android.R.string.cancel))
                }
                TextButton(
                    onClick = {
                        onLyricsTextSizeChange(tempTextSize)
                        showLyricsTextSizeDialog = false
                    },
                ) {
                    Text(stringResource(android.R.string.ok))
                }
            },
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(16.dp),
            ) {
                Text(
                    text = stringResource(R.string.lyrics_text_size),
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(bottom = 16.dp),
                )

                Text(
                    text = "${tempTextSize.roundToInt()} sp",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(bottom = 16.dp),
                )

                Slider(
                    value = tempTextSize,
                    onValueChange = { tempTextSize = it },
                    valueRange = 12f..48f,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }

    if (showLyricsLineSpacingDialog) {
        var tempLineSpacing by remember { mutableFloatStateOf(lyricsLineSpacing) }

        DefaultDialog(
            onDismiss = {
                tempLineSpacing = lyricsLineSpacing
                showLyricsLineSpacingDialog = false
            },
            buttons = {
                TextButton(
                    onClick = {
                        tempLineSpacing = 1.3f
                    },
                ) {
                    Text(stringResource(R.string.reset))
                }

                Spacer(modifier = Modifier.weight(1f))

                TextButton(
                    onClick = {
                        tempLineSpacing = lyricsLineSpacing
                        showLyricsLineSpacingDialog = false
                    },
                ) {
                    Text(stringResource(android.R.string.cancel))
                }
                TextButton(
                    onClick = {
                        onLyricsLineSpacingChange(tempLineSpacing)
                        showLyricsLineSpacingDialog = false
                    },
                ) {
                    Text(stringResource(android.R.string.ok))
                }
            },
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(16.dp),
            ) {
                Text(
                    text = stringResource(R.string.lyrics_line_spacing),
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(bottom = 16.dp),
                )

                Text(
                    text = String.format(Locale.US, "%.1f", tempLineSpacing),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(bottom = 16.dp),
                )

                Slider(
                    value = tempLineSpacing,
                    onValueChange = { tempLineSpacing = it },
                    valueRange = 1.0f..3.0f,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }

    if (showPlayerButtonsStyleDialog) {
        EnumDialog(
            onDismiss = { showPlayerButtonsStyleDialog = false },
            onSelect = {
                onPlayerButtonsStyleChange(it)
                showPlayerButtonsStyleDialog = false
            },
            title = stringResource(R.string.player_buttons_style),
            current = playerButtonsStyle,
            values = PlayerButtonsStyle.values().toList(),
            valueText = {
                when (it) {
                    PlayerButtonsStyle.DEFAULT -> stringResource(R.string.default_style)
                    PlayerButtonsStyle.PRIMARY -> stringResource(R.string.primary_color_style)
                    PlayerButtonsStyle.TERTIARY -> stringResource(R.string.tertiary_color_style)
                }
            },
        )
    }

    if (showPlayerBackgroundDialog) {
        EnumDialog(
            onDismiss = { showPlayerBackgroundDialog = false },
            onSelect = {
                onPlayerBackgroundChange(it)
                showPlayerBackgroundDialog = false
            },
            title = stringResource(R.string.player_background_style),
            current = playerBackground,
            values = availableBackgroundStyles,
            valueText = {
                when (it) {
                    PlayerBackgroundStyle.DEFAULT -> stringResource(R.string.follow_theme)
                    PlayerBackgroundStyle.GRADIENT -> stringResource(R.string.gradient)
                    PlayerBackgroundStyle.BLUR -> stringResource(R.string.player_background_blur)
                }
            },
        )
    }

    if (showMiniPlayerBackgroundDialog) {
        EnumDialog(
            onDismiss = { showMiniPlayerBackgroundDialog = false },
            onSelect = {
                onMiniPlayerBackgroundChange(it)
                showMiniPlayerBackgroundDialog = false
            },
            title = stringResource(R.string.mini_player_background_style),
            current = miniPlayerBackground,
            values = availableMiniPlayerBackgroundStyles,
            valueText = {
                when (it) {
                    MiniPlayerBackgroundStyle.DEFAULT -> stringResource(R.string.follow_theme)
                    MiniPlayerBackgroundStyle.TRANSPARENT -> stringResource(R.string.transparent)
                    MiniPlayerBackgroundStyle.BLUR -> stringResource(R.string.player_background_blur)
                    MiniPlayerBackgroundStyle.GRADIENT -> stringResource(R.string.gradient)
                    MiniPlayerBackgroundStyle.PURE_BLACK -> stringResource(R.string.pure_black)
                }
            },
        )
    }

    var showDefaultOpenTabDialog by rememberSaveable {
        mutableStateOf(false)
    }

    if (showDefaultOpenTabDialog) {
        EnumDialog(
            onDismiss = { showDefaultOpenTabDialog = false },
            onSelect = {
                onDefaultOpenTabChange(it)
                showDefaultOpenTabDialog = false
            },
            title = stringResource(R.string.default_open_tab),
            current = defaultOpenTab,
            values = NavigationTab.values().toList(),
            valueText = {
                when (it) {
                    NavigationTab.HOME -> stringResource(R.string.home)
                    NavigationTab.SEARCH -> stringResource(R.string.search)
                    NavigationTab.LIBRARY -> stringResource(R.string.filter_library)
                }
            },
        )
    }

    if (showCustomAppTitleDialog) {
        var tempTitle by rememberSaveable { mutableStateOf(customAppTitle) }
        DefaultDialog(
            onDismiss = { showCustomAppTitleDialog = false },
            buttons = {
                TextButton(onClick = {
                    onCustomAppTitleChange("")
                    showCustomAppTitleDialog = false
                }) {
                    Text(stringResource(R.string.reset))
                }
                Spacer(modifier = Modifier.weight(1f))
                TextButton(onClick = { showCustomAppTitleDialog = false }) {
                    Text(stringResource(android.R.string.cancel))
                }
                TextButton(onClick = {
                    onCustomAppTitleChange(tempTitle.trim())
                    showCustomAppTitleDialog = false
                }) {
                    Text(stringResource(android.R.string.ok))
                }
            },
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = stringResource(R.string.app_custom_name),
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(bottom = 16.dp),
                )
                OutlinedTextField(
                    value = tempTitle,
                    onValueChange = { tempTitle = it },
                    label = { Text(stringResource(R.string.app_custom_name)) },
                    placeholder = { Text(stringResource(R.string.app_name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }

    if (showAppIconDialog) {
        DefaultDialog(
            onDismiss = { showAppIconDialog = false },
            buttons = {
                TextButton(onClick = { showAppIconDialog = false }) {
                    Text(stringResource(android.R.string.ok))
                }
            },
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = stringResource(R.string.app_icon),
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(bottom = 16.dp),
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            onEnableDynamicIconChange(true)
                            showAppIconDialog = false
                        }
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(
                        selected = customIconFile == null && enableDynamicIcon,
                        onClick = {
                            onEnableDynamicIconChange(true)
                            showAppIconDialog = false
                        },
                        modifier = Modifier.padding(end = 12.dp),
                    )
                    Column {
                        Text("QuintaLyst (Material You)", style = MaterialTheme.typography.bodyLarge)
                        Text("Icono adaptativo dinámico", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            onEnableDynamicIconChange(false)
                            showAppIconDialog = false
                        }
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(
                        selected = customIconFile == null && !enableDynamicIcon,
                        onClick = {
                            onEnableDynamicIconChange(false)
                            showAppIconDialog = false
                        },
                        modifier = Modifier.padding(end = 12.dp),
                    )
                    Column {
                        Text("QuintaLyst (Clásico)", style = MaterialTheme.typography.bodyLarge)
                        Text("Icono estático original", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = stringResource(R.string.custom_app_icon_gallery),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 4.dp),
                )
                Text(
                    text = stringResource(R.string.custom_icon_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 12.dp),
                )

                if (customIconFile != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        AsyncImage(
                            model = ImageRequest.Builder(iconContext)
                                .data(customIconFile)
                                .memoryCacheKey("custom_icon_${customAppIconPath}_${customAppIconTimestamp}")
                                .diskCachePolicy(CachePolicy.DISABLED)
                                .build(),
                            contentDescription = stringResource(R.string.custom_app_icon_gallery),
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(64.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .border(1.5.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(16.dp)),
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            GlassButton(
                                onClick = {
                                    val added = IconUtils.addCustomIconShortcut(iconContext, customAppTitle, customAppIconPath)
                                    if (added) {
                                        Toast.makeText(iconContext, iconContext.getString(R.string.shortcut_requested), Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text(stringResource(R.string.add_icon_to_home_screen))
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                GlassOutlinedButton(
                                    onClick = { pickImageLauncher.launch("image/*") },
                                    modifier = Modifier.weight(1f),
                                ) {
                                    Text("Cambiar")
                                }
                                TextButton(
                                    onClick = {
                                        IconUtils.deleteCustomIcon(iconContext)
                                        onCustomAppIconPathChange("")
                                        onCustomAppIconTimestampChange(System.currentTimeMillis())
                                    },
                                    modifier = Modifier.weight(1f),
                                ) {
                                    Text(stringResource(R.string.remove_custom_icon))
                                }
                            }
                        }
                    }
                } else {
                    GlassButton(
                        onClick = { pickImageLauncher.launch("image/*") },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(painter = painterResource(R.drawable.insert_photo), contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.choose_from_gallery))
                    }
                }
            }
        }
    }

    if (showMenuSectionsDialog) {
        DefaultDialog(
            onDismiss = { showMenuSectionsDialog = false },
            buttons = {
                TextButton(onClick = { showMenuSectionsDialog = false }) {
                    Text(stringResource(android.R.string.ok))
                }
            },
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                Text(
                    text = stringResource(R.string.home_sections),
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(bottom = 12.dp),
                )
                listOf(
                    Pair(stringResource(R.string.section_speed_dial), Pair(showSpeedDial, onShowSpeedDialChange)),
                    Pair(stringResource(R.string.section_quick_picks), Pair(showQuickPicks, onShowQuickPicksChange)),
                    Pair(stringResource(R.string.section_daily_discover), Pair(showDailyDiscover, onShowDailyDiscoverChange)),
                    Pair(stringResource(R.string.section_keep_listening), Pair(showKeepListening, onShowKeepListeningChange)),
                    Pair(stringResource(R.string.section_account_playlists), Pair(showAccountPlaylists, onShowAccountPlaylistsChange)),
                    Pair(stringResource(R.string.section_forgotten_favorites), Pair(showForgottenFavorites, onShowForgottenFavoritesChange)),
                    Pair(stringResource(R.string.section_from_the_community), Pair(showFromTheCommunity, onShowFromTheCommunityChange)),
                    Pair(stringResource(R.string.section_mood_and_genres), Pair(showMoodAndGenres, onShowMoodAndGenresChange)),
                ).forEach { (title, state) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(title, style = MaterialTheme.typography.bodyLarge)
                        Switch(
                            checked = state.first,
                            onCheckedChange = state.second,
                            thumbContent = {
                                Icon(
                                    painter = painterResource(if (state.first) R.drawable.check else R.drawable.close),
                                    contentDescription = null,
                                    modifier = Modifier.size(SwitchDefaults.IconSize),
                                )
                            },
                        )
                    }
                }
            }
        }
    }

    var showDefaultChipDialog by rememberSaveable {
        mutableStateOf(false)
    }

    if (showDefaultChipDialog) {
        EnumDialog(
            onDismiss = { showDefaultChipDialog = false },
            onSelect = {
                onDefaultChipChange(it)
                showDefaultChipDialog = false
            },
            title = stringResource(R.string.default_lib_chips),
            current = defaultChip,
            values = LibraryFilter.values().toList(),
            valueText = {
                when (it) {
                    LibraryFilter.SONGS -> stringResource(R.string.songs)
                    LibraryFilter.ARTISTS -> stringResource(R.string.artists)
                    LibraryFilter.ALBUMS -> stringResource(R.string.albums)
                    LibraryFilter.PLAYLISTS -> stringResource(R.string.playlists)
                    LibraryFilter.PODCASTS -> stringResource(R.string.filter_podcasts)
                    LibraryFilter.LIBRARY -> stringResource(R.string.filter_library)
                }
            },
        )
    }

    var showGridSizeDialog by rememberSaveable {
        mutableStateOf(false)
    }

    if (showGridSizeDialog) {
        EnumDialog(
            onDismiss = { showGridSizeDialog = false },
            onSelect = {
                onGridItemSizeChange(it)
                showGridSizeDialog = false
            },
            title = stringResource(R.string.grid_cell_size),
            current = gridItemSize,
            values = GridItemSize.values().toList(),
            valueText = {
                when (it) {
                    GridItemSize.BIG -> stringResource(R.string.big)
                    GridItemSize.SMALL -> stringResource(R.string.small)
                }
            },
        )
    }

    if (showDensityScaleDialog) {
        DefaultDialog(
            onDismiss = { showDensityScaleDialog = false },
            buttons = {
                TextButton(
                    onClick = { showDensityScaleDialog = false },
                ) {
                    Text(text = stringResource(android.R.string.cancel))
                }
            },
        ) {
            Column {
                DensityScale.entries.forEach { scale ->
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onDensityScaleChange(scale.value)
                                    showDensityScaleDialog = false
                                }.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = scale.label,
                            style = MaterialTheme.typography.bodyLarge,
                            color =
                                if (densityScale == scale.value) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                },
                        )
                    }
                }
            }
        }
    }

    if (showSliderOptionDialog) {
        DefaultDialog(
            buttons = {
                TextButton(
                    onClick = { showSliderOptionDialog = false },
                ) {
                    Text(text = stringResource(android.R.string.cancel))
                }
            },
            onDismiss = {
                showSliderOptionDialog = false
            },
        ) {
            val sliderPreviewColors =
                PlayerSliderColors.getSliderColors(
                    MaterialTheme.colorScheme.primary,
                    PlayerBackgroundStyle.DEFAULT,
                    isSystemInDarkTheme(),
                )

            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier =
                            Modifier
                                .aspectRatio(1f)
                                .weight(1f)
                                .clip(RoundedCornerShape(16.dp))
                                .border(
                                    1.dp,
                                    if (sliderStyle == SliderStyle.DEFAULT &&
                                        !squigglySlider
                                    ) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.outlineVariant
                                    },
                                    RoundedCornerShape(16.dp),
                                ).clickable {
                                    onSliderStyleChange(SliderStyle.DEFAULT)
                                    onSquigglySliderChange(false)
                                    showSliderOptionDialog = false
                                }.padding(12.dp),
                    ) {
                        val sliderValue = 0.35f
                        Slider(
                            value = sliderValue,
                            valueRange = 0f..1f,
                            onValueChange = { /* preview only */ },
                            colors = sliderPreviewColors,
                            enabled = false,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            text = stringResource(R.string.default_),
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier =
                            Modifier
                                .aspectRatio(1f)
                                .weight(1f)
                                .clip(RoundedCornerShape(16.dp))
                                .border(
                                    1.dp,
                                    if (sliderStyle == SliderStyle.WAVY &&
                                        !squigglySlider
                                    ) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.outlineVariant
                                    },
                                    RoundedCornerShape(16.dp),
                                ).clickable {
                                    onSliderStyleChange(SliderStyle.WAVY)
                                    onSquigglySliderChange(false)
                                    showSliderOptionDialog = false
                                }.padding(12.dp),
                    ) {
                        val sliderValue = 0.5f
                        WavySlider(
                            value = sliderValue,
                            valueRange = 0f..1f,
                            onValueChange = { /* preview only */ },
                            colors = sliderPreviewColors,
                            modifier = Modifier.weight(1f),
                            isPlaying = true,
                            enabled = false,
                        )
                        Text(
                            text = stringResource(R.string.wavy),
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier =
                            Modifier
                                .aspectRatio(1f)
                                .weight(1f)
                                .clip(RoundedCornerShape(16.dp))
                                .border(
                                    1.dp,
                                    if (sliderStyle ==
                                        SliderStyle.SLIM
                                    ) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.outlineVariant
                                    },
                                    RoundedCornerShape(16.dp),
                                ).clickable {
                                    onSliderStyleChange(SliderStyle.SLIM)
                                    onSquigglySliderChange(false)
                                    showSliderOptionDialog = false
                                }.padding(12.dp),
                    ) {
                        val sliderState = remember { SliderState(value = 0.65f) }
                        Slider(
                            state = sliderState,
                            thumb = { Spacer(modifier = Modifier.size(0.dp)) },
                            track = { sliderState ->
                                PlayerSliderTrack(
                                    sliderState = sliderState,
                                    colors = sliderPreviewColors,
                                )
                            },
                            colors = sliderPreviewColors,
                            enabled = false,
                            modifier = Modifier.weight(1f),
                        )

                        Text(
                            text = stringResource(R.string.slim),
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier =
                            Modifier
                                .aspectRatio(1f)
                                .weight(1f)
                                .clip(RoundedCornerShape(16.dp))
                                .border(
                                    1.dp,
                                    if (sliderStyle == SliderStyle.WAVY &&
                                        squigglySlider
                                    ) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.outlineVariant
                                    },
                                    RoundedCornerShape(16.dp),
                                ).clickable {
                                    onSliderStyleChange(SliderStyle.WAVY)
                                    onSquigglySliderChange(true)
                                    showSliderOptionDialog = false
                                }.padding(12.dp),
                    ) {
                        val sliderValue = 0.5f
                        SquigglySlider(
                            value = sliderValue,
                            valueRange = 0f..1f,
                            onValueChange = { /* preview only */ },
                            modifier = Modifier.weight(1f),
                            enabled = false,
                            colors = sliderPreviewColors,
                            isPlaying = true,
                        )
                        Text(
                            text = stringResource(R.string.squiggly),
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }

    Column(
        Modifier
            .windowInsetsPadding(LocalPlayerAwareWindowInsets.current)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
    ) {
        // ─── Fondo de la app (color sólido / gradiente / álbum / foto propia) ───
        AppBackgroundSettingsGroup()
        Spacer(modifier = Modifier.height(16.dp))

        // ─── Image appearance ──────────────────────────────────────────────────
        Material3SettingsGroup(
            title = "Imágenes",
            items = buildList {
                add(
                    Material3SettingsItem(
                        icon = painterResource(R.drawable.insert_photo),
                        title = { Text("Redondeo de imágenes: ${thumbnailCornerRadiusPref.toInt()}dp") },
                        description = {
                            Slider(
                                value = thumbnailCornerRadiusPref,
                                onValueChange = onThumbnailCornerRadiusChange,
                                valueRange = 0f..50f,
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                            )
                        },
                    ),
                )
            },
        )

        // ─── Accent Color ───────────────────────────────────────────────────────
        Material3SettingsGroup(
            title = stringResource(R.string.accent_color),
            items = buildList {
                add(
                    Material3SettingsItem(
                        icon = painterResource(R.drawable.palette),
                        title = { Text(stringResource(R.string.accent_color)) },
                        description = {
                            Column(modifier = Modifier.padding(top = 8.dp)) {
                                Text(
                                    text = if (isUsingCustomColor) "Color personalizado seleccionado" else "Color dinámico activo (sigue portada/sistema)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(bottom = 8.dp),
                                )
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    contentPadding = PaddingValues(vertical = 4.dp),
                                ) {
                                    items(PaletteColors) { palette ->
                                        val isDynamicPalette = palette.seedColor == Color.Transparent
                                        val isSelected = if (isDynamicPalette) {
                                            selectedThemeColorInt == DefaultThemeColor.toArgb()
                                        } else {
                                            selectedThemeColorInt == palette.seedColor.toArgb()
                                        }
                                        val color = if (isDynamicPalette) MaterialTheme.colorScheme.primary else palette.seedColor
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(CircleShape)
                                                .background(color)
                                                .border(
                                                    width = if (isSelected) 3.dp else 1.dp,
                                                    color = if (isSelected) MaterialTheme.colorScheme.onSurface else Color.White.copy(alpha = 0.35f),
                                                    shape = CircleShape,
                                                )
                                                .clickable {
                                                    val colorToSave = if (isDynamicPalette) DefaultThemeColor else palette.seedColor
                                                    onSelectedThemeColorChange(colorToSave.toArgb())
                                                    onDynamicThemeChange(isDynamicPalette)
                                                },
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            if (isSelected) {
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
                        },
                    ),
                )
            },
        )

        // ─── App Name & Icon ────────────────────────────────────────────────────
        Material3SettingsGroup(
            title = stringResource(R.string.app_custom_name),
            items = buildList {
                add(
                    Material3SettingsItem(
                        icon = painterResource(R.drawable.edit),
                        title = { Text(stringResource(R.string.app_custom_name)) },
                        description = {
                            Text(if (customAppTitle.isNotBlank()) customAppTitle else stringResource(R.string.app_name))
                        },
                        onClick = { showCustomAppTitleDialog = true },
                    ),
                )
                add(
                    Material3SettingsItem(
                        icon = painterResource(R.drawable.palette),
                        title = { Text(stringResource(R.string.app_icon)) },
                        description = {
                            Text(if (customIconFile != null) stringResource(R.string.custom_app_icon_gallery) else if (enableDynamicIcon) "Dinámico (Material You)" else "Clásico (Estático)")
                        },
                        onClick = { showAppIconDialog = true },
                    ),
                )
            },
        )

        // ─── Menu & Navigation Sections ─────────────────────────────────────────
        Material3SettingsGroup(
            title = stringResource(R.string.menu_sections_settings),
            items = buildList {
                add(
                    Material3SettingsItem(
                        icon = painterResource(R.drawable.tune),
                        title = { Text(stringResource(R.string.menu_sections_settings)) },
                        description = { Text(stringResource(R.string.menu_sections_settings_desc)) },
                        onClick = { showMenuSectionsDialog = true },
                    ),
                )
            },
        )

        Material3SettingsGroup(
            title = stringResource(R.string.theme),
            items =
                buildList {
                    add(
                        Material3SettingsItem(
                            icon = painterResource(R.drawable.speed),
                            title = { Text(stringResource(R.string.enable_high_refresh_rate)) },
                            description = { Text(stringResource(R.string.enable_high_refresh_rate_desc)) },
                            trailingContent = {
                                Switch(
                                    checked = enableHighRefreshRate,
                                    onCheckedChange = onEnableHighRefreshRateChange,
                                    thumbContent = {
                                        Icon(
                                            painter =
                                                painterResource(
                                                    id = if (enableHighRefreshRate) R.drawable.check else R.drawable.close,
                                                ),
                                            contentDescription = null,
                                            modifier = Modifier.size(SwitchDefaults.IconSize),
                                        )
                                    },
                                )
                            },
                            onClick = { onEnableHighRefreshRateChange(!enableHighRefreshRate) },
                        ),
                    )
                    add(
                        Material3SettingsItem(
                            icon = painterResource(R.drawable.fullscreen),
                            title = { Text(stringResource(R.string.enable_landscape_scaling)) },
                            description = { Text(stringResource(R.string.enable_landscape_scaling_desc)) },
                            trailingContent = {
                                Switch(
                                    checked = enableLandscapeScaling,
                                    onCheckedChange = onEnableLandscapeScalingChange,
                                    thumbContent = {
                                        Icon(
                                            painter =
                                                painterResource(
                                                    id = if (enableLandscapeScaling) R.drawable.check else R.drawable.close,
                                                ),
                                            contentDescription = null,
                                            modifier = Modifier.size(SwitchDefaults.IconSize),
                                        )
                                    },
                                )
                            },
                            onClick = { onEnableLandscapeScalingChange(!enableLandscapeScaling) },
                        ),
                    )
                    // Only show dynamic theme option when using the default/dynamic color
                    // When a custom color is selected, dynamic theme is automatically disabled
                    if (!isUsingCustomColor) {
                        add(
                            Material3SettingsItem(
                                icon = painterResource(R.drawable.palette),
                                title = { Text(stringResource(R.string.enable_dynamic_theme)) },
                                trailingContent = {
                                    Switch(
                                        checked = dynamicTheme,
                                        onCheckedChange = onDynamicThemeChange,
                                        thumbContent = {
                                            Icon(
                                                painter =
                                                    painterResource(
                                                        id = if (dynamicTheme) R.drawable.check else R.drawable.close,
                                                    ),
                                                contentDescription = null,
                                                modifier = Modifier.size(SwitchDefaults.IconSize),
                                            )
                                        },
                                    )
                                },
                                onClick = { onDynamicThemeChange(!dynamicTheme) },
                            ),
                        )
                    }
                    add(
                        Material3SettingsItem(
                            icon = painterResource(R.drawable.palette),
                            title = { Text(stringResource(R.string.enable_dynamic_icon)) },
                            description = { Text(stringResource(R.string.enable_dynamic_icon_desc)) },
                            trailingContent = {
                                Switch(
                                    checked = enableDynamicIcon,
                                    onCheckedChange = onEnableDynamicIconChange,
                                    thumbContent = {
                                        Icon(
                                            painter =
                                                painterResource(
                                                    id = if (enableDynamicIcon) R.drawable.check else R.drawable.close,
                                                ),
                                            contentDescription = null,
                                            modifier = Modifier.size(SwitchDefaults.IconSize),
                                        )
                                    },
                                )
                            },
                            onClick = { onEnableDynamicIconChange(!enableDynamicIcon) },
                        ),
                    )
                    add(
                        Material3SettingsItem(
                            icon = painterResource(R.drawable.palette),
                            title = { Text(stringResource(R.string.theme)) },
                            description = { Text(stringResource(R.string.theme_desc)) },
                            onClick = { navController.navigate("settings/appearance/theme") },
                        ),
                    )
                },
        )

        Spacer(modifier = Modifier.height(27.dp))

        val (pureBlackMiniPlayer, onPureBlackMiniPlayerChange) =
            rememberPreference(
                PureBlackMiniPlayerKey,
                defaultValue = false,
            )

        Material3SettingsGroup(
            title = stringResource(id = R.string.app_style),
            items =
                buildList {
                    add(
                        Material3SettingsItem(
                            icon = painterResource(R.drawable.nav_bar),
                            title = { Text(stringResource(R.string.app_style_original)) },
                            description = { Text(stringResource(R.string.app_style_original_desc)) },
                            trailingContent = { RadioButton(selected = appStyle == 0, onClick = null) },
                            onClick = { onAppStyleSelected(0) },
                        ),
                    )
                    add(
                        Material3SettingsItem(
                            icon = painterResource(R.drawable.nav_bar),
                            title = { Text(stringResource(R.string.liquid_glass)) },
                            description = { Text(stringResource(R.string.liquid_glass_desc)) },
                            trailingContent = { RadioButton(selected = appStyle == 1, onClick = null) },
                            onClick = { onAppStyleSelected(1) },
                        ),
                    )
                    if (appStyle == 1) {
                        add(
                            Material3SettingsItem(
                                icon = painterResource(R.drawable.contrast),
                                title = { Text("Opacidad del cristal de Reproduciendo: ${(playerGlassOpacity * 100).toInt()}%") },
                                description = {
                                    androidx.compose.material3.Slider(
                                        value = playerGlassOpacity.coerceIn(0.1f, 1f),
                                        onValueChange = onPlayerGlassOpacityChange,
                                        valueRange = 0.1f..1f,
                                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                                    )
                                },
                            ),
                        )
                    }
                    add(
                        Material3SettingsItem(
                            icon = painterResource(R.drawable.nav_bar),
                            title = { Text(stringResource(R.string.retro_style)) },
                            description = { Text(stringResource(R.string.retro_style_desc)) },
                            trailingContent = { RadioButton(selected = appStyle == 2, onClick = null) },
                            onClick = { onAppStyleSelected(2) },
                        ),
                    )
                    if (appStyle == 2) {
                        add(
                            Material3SettingsItem(
                                icon = painterResource(R.drawable.gradient),
                                title = { Text(stringResource(R.string.retro_label_dynamic)) },
                                description = { Text(stringResource(R.string.retro_label_dynamic_desc)) },
                                trailingContent = {
                                    Switch(
                                        checked = retroDynamicLabel,
                                        onCheckedChange = onRetroDynamicLabelChange,
                                        thumbContent = {
                                            Icon(
                                                painter =
                                                    painterResource(
                                                        id = if (retroDynamicLabel) R.drawable.check else R.drawable.close,
                                                    ),
                                                contentDescription = null,
                                                modifier = Modifier.size(SwitchDefaults.IconSize),
                                            )
                                        },
                                    )
                                },
                                onClick = { onRetroDynamicLabelChange(!retroDynamicLabel) },
                            ),
                        )
                    }
                },
        )

        Material3SettingsGroup(
            title = stringResource(id = R.string.mini_player),
            items =
                buildList {
                    add(
                        Material3SettingsItem(
                            icon = painterResource(R.drawable.nav_bar),
                            title = { Text(stringResource(R.string.new_mini_player_design)) },
                            trailingContent = {
                                Switch(
                                    checked = useNewMiniPlayerDesign,
                                    onCheckedChange = onUseNewMiniPlayerDesignChange,
                                    thumbContent = {
                                        Icon(
                                            painter =
                                                painterResource(
                                                    id = if (useNewMiniPlayerDesign) R.drawable.check else R.drawable.close,
                                                ),
                                            contentDescription = null,
                                            modifier = Modifier.size(SwitchDefaults.IconSize),
                                        )
                                    },
                                )
                            },
                            onClick = { onUseNewMiniPlayerDesignChange(!useNewMiniPlayerDesign) },
                        ),
                    )
                    add(
                        Material3SettingsItem(
                            icon = painterResource(R.drawable.gradient),
                            title = {
                                Text(
                                    text = stringResource(R.string.mini_player_background_style),
                                    color =
                                        if (!useNewMiniPlayerDesign) {
                                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                                        } else {
                                            MaterialTheme.colorScheme.onSurface
                                        },
                                )
                            },
                            description = {
                                Text(
                                    text =
                                        if (!useNewMiniPlayerDesign) {
                                            stringResource(R.string.mini_player_background_not_available)
                                        } else {
                                            when (miniPlayerBackground) {
                                                MiniPlayerBackgroundStyle.DEFAULT -> stringResource(R.string.follow_theme)
                                                MiniPlayerBackgroundStyle.TRANSPARENT -> stringResource(R.string.transparent)
                                                MiniPlayerBackgroundStyle.BLUR -> stringResource(R.string.player_background_blur)
                                                MiniPlayerBackgroundStyle.GRADIENT -> stringResource(R.string.gradient)
                                                MiniPlayerBackgroundStyle.PURE_BLACK -> stringResource(R.string.pure_black)
                                            }
                                        },
                                    color =
                                        if (!useNewMiniPlayerDesign) {
                                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        },
                                )
                            },
                            onClick = { if (useNewMiniPlayerDesign) showMiniPlayerBackgroundDialog = true },
                        ),
                    )
                    add(
                        Material3SettingsItem(
                            icon = painterResource(R.drawable.contrast),
                            title = { Text("Oscurecer barra de reproducción: ${(miniPlayerDim * 100).toInt()}%") },
                            description = {
                                androidx.compose.material3.Slider(
                                    value = miniPlayerDim.coerceIn(0f, 1f),
                                    onValueChange = onMiniPlayerDimChange,
                                    valueRange = 0f..1f,
                                    enabled = useNewMiniPlayerDesign,
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                                )
                            },
                        ),
                    )
                },
        )

        Spacer(modifier = Modifier.height(27.dp))

        var showSensitivityDialog by rememberSaveable { mutableStateOf(false) }

        Material3SettingsGroup(
            title = stringResource(R.string.player),
            items =
                listOf(
                    Material3SettingsItem(
                        icon = painterResource(R.drawable.palette),
                        title = { Text(stringResource(R.string.new_player_design)) },
                        trailingContent = {
                            Switch(
                                checked = useNewPlayerDesign,
                                onCheckedChange = onUseNewPlayerDesignChange,
                                thumbContent = {
                                    Icon(
                                        painter =
                                            painterResource(
                                                id = if (useNewPlayerDesign) R.drawable.check else R.drawable.close,
                                            ),
                                        contentDescription = null,
                                        modifier = Modifier.size(SwitchDefaults.IconSize),
                                    )
                                },
                            )
                        },
                        onClick = { onUseNewPlayerDesignChange(!useNewPlayerDesign) },
                    ),
                    Material3SettingsItem(
                        icon = painterResource(R.drawable.gradient),
                        title = { Text(stringResource(R.string.player_background_style)) },
                        description = {
                            Text(
                                when (playerBackground) {
                                    PlayerBackgroundStyle.DEFAULT -> stringResource(R.string.follow_theme)
                                    PlayerBackgroundStyle.GRADIENT -> stringResource(R.string.gradient)
                                    PlayerBackgroundStyle.BLUR -> stringResource(R.string.player_background_blur)
                                },
                            )
                        },
                        onClick = { showPlayerBackgroundDialog = true },
                    ),
                    Material3SettingsItem(
                        icon = painterResource(R.drawable.hide_image),
                        title = { Text(stringResource(R.string.hide_player_thumbnail)) },
                        description = { Text(stringResource(R.string.hide_player_thumbnail_desc)) },
                        trailingContent = {
                            Switch(
                                checked = hidePlayerThumbnail,
                                onCheckedChange = onHidePlayerThumbnailChange,
                                thumbContent = {
                                    Icon(
                                        painter =
                                            painterResource(
                                                id = if (hidePlayerThumbnail) R.drawable.check else R.drawable.close,
                                            ),
                                        contentDescription = null,
                                        modifier = Modifier.size(SwitchDefaults.IconSize),
                                    )
                                },
                            )
                        },
                        onClick = { onHidePlayerThumbnailChange(!hidePlayerThumbnail) },
                    ),
                    Material3SettingsItem(
                        icon = painterResource(R.drawable.crop),
                        title = { Text(stringResource(R.string.crop_album_art)) },
                        description = { Text(stringResource(R.string.crop_album_art_desc)) },
                        trailingContent = {
                            Switch(
                                checked = cropAlbumArt,
                                onCheckedChange = onCropAlbumArtChange,
                                thumbContent = {
                                    Icon(
                                        painter =
                                            painterResource(
                                                id = if (cropAlbumArt) R.drawable.check else R.drawable.close,
                                            ),
                                        contentDescription = null,
                                        modifier = Modifier.size(SwitchDefaults.IconSize),
                                    )
                                },
                            )
                        },
                        onClick = { onCropAlbumArtChange(!cropAlbumArt) },
                    ),
                    Material3SettingsItem(
                        icon = painterResource(R.drawable.palette),
                        title = { Text(stringResource(R.string.player_buttons_style)) },
                        description = {
                            Text(
                                when (playerButtonsStyle) {
                                    PlayerButtonsStyle.DEFAULT -> stringResource(R.string.default_style)
                                    PlayerButtonsStyle.PRIMARY -> stringResource(R.string.primary_color_style)
                                    PlayerButtonsStyle.TERTIARY -> stringResource(R.string.tertiary_color_style)
                                },
                            )
                        },
                        onClick = { showPlayerButtonsStyleDialog = true },
                    ),
                    Material3SettingsItem(
                        icon = painterResource(R.drawable.sliders),
                        title = { Text(stringResource(R.string.player_slider_style)) },
                        description = {
                            Text(
                                when (sliderStyle) {
                                    SliderStyle.DEFAULT -> {
                                        stringResource(R.string.default_)
                                    }

                                    SliderStyle.WAVY -> {
                                        if (squigglySlider) {
                                            stringResource(R.string.squiggly)
                                        } else {
                                            stringResource(
                                                R.string.wavy,
                                            )
                                        }
                                    }

                                    SliderStyle.SLIM -> {
                                        stringResource(R.string.slim)
                                    }
                                },
                            )
                        },
                        onClick = { showSliderOptionDialog = true },
                    ),
                    Material3SettingsItem(
                        icon = painterResource(R.drawable.swipe),
                        title = { Text(stringResource(R.string.enable_swipe_thumbnail)) },
                        trailingContent = {
                            Switch(
                                checked = swipeThumbnail,
                                onCheckedChange = onSwipeThumbnailChange,
                                thumbContent = {
                                    Icon(
                                        painter =
                                            painterResource(
                                                id = if (swipeThumbnail) R.drawable.check else R.drawable.close,
                                            ),
                                        contentDescription = null,
                                        modifier = Modifier.size(SwitchDefaults.IconSize),
                                    )
                                },
                            )
                        },
                        onClick = { onSwipeThumbnailChange(!swipeThumbnail) },
                    ),
                ) +
                    if (swipeThumbnail) {
                        listOf(
                            Material3SettingsItem(
                                icon = painterResource(R.drawable.tune),
                                title = { Text(stringResource(R.string.swipe_sensitivity)) },
                                description = {
                                    Text(
                                        stringResource(
                                            R.string.sensitivity_percentage,
                                            (swipeSensitivity * 100).roundToInt(),
                                        ),
                                    )
                                },
                                onClick = { showSensitivityDialog = true },
                            ),
                        )
                    } else {
                        emptyList()
                    },
        )

        if (showSensitivityDialog) {
            var tempSensitivity by remember { mutableFloatStateOf(swipeSensitivity) }

            DefaultDialog(
                onDismiss = {
                    tempSensitivity = swipeSensitivity
                    showSensitivityDialog = false
                },
                buttons = {
                    TextButton(
                        onClick = {
                            tempSensitivity = 0.73f
                        },
                    ) {
                        Text(stringResource(R.string.reset))
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    TextButton(
                        onClick = {
                            tempSensitivity = swipeSensitivity
                            showSensitivityDialog = false
                        },
                    ) {
                        Text(stringResource(android.R.string.cancel))
                    }
                    TextButton(
                        onClick = {
                            onSwipeSensitivityChange(tempSensitivity)
                            showSensitivityDialog = false
                        },
                    ) {
                        Text(stringResource(android.R.string.ok))
                    }
                },
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(16.dp),
                ) {
                    Text(
                        text = stringResource(R.string.swipe_sensitivity),
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.padding(bottom = 16.dp),
                    )

                    Text(
                        text =
                            stringResource(
                                R.string.sensitivity_percentage,
                                (tempSensitivity * 100).roundToInt(),
                            ),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(bottom = 16.dp),
                    )

                    Slider(
                        value = tempSensitivity,
                        onValueChange = { tempSensitivity = it },
                        valueRange = 0f..1f,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(27.dp))

        Material3SettingsGroup(
            title = stringResource(R.string.lyrics),
            items =
                buildList {
                    add(
                        Material3SettingsItem(
                            icon = painterResource(R.drawable.lyrics),
                            title = { Text(stringResource(R.string.experimental_lyrics)) },
                            description = { Text(stringResource(R.string.experimental_lyrics_desc)) },
                            showBadge = true,
                            trailingContent = {
                                Switch(
                                    checked = experimentalLyrics,
                                    onCheckedChange = {
                                        if (!experimentalLyrics) {
                                            showExperimentalLyricsBetaDialog = true
                                        } else {
                                            onExperimentalLyricsChange(false)
                                        }
                                    },
                                    thumbContent = {
                                        Icon(
                                            painter =
                                                painterResource(
                                                    id = if (experimentalLyrics) R.drawable.check else R.drawable.close,
                                                ),
                                            contentDescription = null,
                                            modifier = Modifier.size(SwitchDefaults.IconSize),
                                        )
                                    },
                                )
                            },
                            onClick = {
                                if (!experimentalLyrics) {
                                    showExperimentalLyricsBetaDialog = true
                                } else {
                                    onExperimentalLyricsChange(false)
                                }
                            },
                        ),
                    )

                    if (!experimentalLyrics) {
                        add(
                            Material3SettingsItem(
                                icon = painterResource(R.drawable.lyrics),
                                title = { Text(stringResource(R.string.lyrics_glow_effect)) },
                                description = { Text(stringResource(R.string.lyrics_glow_effect_desc)) },
                                trailingContent = {
                                    Switch(
                                        checked = lyricsGlowEffect,
                                        onCheckedChange = onLyricsGlowEffectChange,
                                        thumbContent = {
                                            Icon(
                                                painter =
                                                    painterResource(
                                                        id = if (lyricsGlowEffect) R.drawable.check else R.drawable.close,
                                                    ),
                                                contentDescription = null,
                                                modifier = Modifier.size(SwitchDefaults.IconSize),
                                            )
                                        },
                                    )
                                },
                                onClick = { onLyricsGlowEffectChange(!lyricsGlowEffect) },
                            ),
                        )
                        add(
                            Material3SettingsItem(
                                icon = painterResource(R.drawable.lyrics),
                                title = { Text(stringResource(R.string.lyrics_animation_style_title)) },
                                description = {
                                    Text(
                                        when (lyricsAnimationStyle) {
                                            LyricsAnimationStyle.NONE -> stringResource(R.string.lyrics_animation_none)
                                            LyricsAnimationStyle.FADE -> stringResource(R.string.lyrics_animation_fade)
                                            LyricsAnimationStyle.GLOW -> stringResource(R.string.lyrics_animation_glow)
                                            LyricsAnimationStyle.SLIDE -> stringResource(R.string.lyrics_animation_slide)
                                            LyricsAnimationStyle.KARAOKE -> stringResource(R.string.lyrics_animation_karaoke)
                                            LyricsAnimationStyle.APPLE -> stringResource(R.string.lyrics_animation_apple)
                                        },
                                    )
                                },
                                onClick = { showLyricsAnimationStyleDialog = true },
                            ),
                        )
                        add(
                            Material3SettingsItem(
                                icon = painterResource(R.drawable.lyrics),
                                title = { Text(stringResource(R.string.lyrics_text_size)) },
                                description = { Text("${lyricsTextSize.roundToInt()} sp") },
                                onClick = { showLyricsTextSizeDialog = true },
                            ),
                        )
                        add(
                            Material3SettingsItem(
                                icon = painterResource(R.drawable.lyrics),
                                title = { Text(stringResource(R.string.lyrics_line_spacing)) },
                                description = { Text(String.format(Locale.US, "%.1f", lyricsLineSpacing)) },
                                onClick = { showLyricsLineSpacingDialog = true },
                            ),
                        )
                    }

                    add(
                        Material3SettingsItem(
                            icon = painterResource(R.drawable.lyrics),
                            title = { Text(stringResource(R.string.lyrics_text_position)) },
                            description = {
                                Text(
                                    when (lyricsPosition) {
                                        LyricsPosition.LEFT -> stringResource(R.string.left)
                                        LyricsPosition.CENTER -> stringResource(R.string.center)
                                        LyricsPosition.RIGHT -> stringResource(R.string.right)
                                    },
                                )
                            },
                            onClick = { showLyricsPositionDialog = true },
                        ),
                    )
                    add(
                        Material3SettingsItem(
                            icon = painterResource(R.drawable.lyrics),
                            title = { Text(stringResource(R.string.respect_agent_positioning)) },
                            description = { Text(stringResource(R.string.respect_agent_positioning_desc)) },
                            trailingContent = {
                                Switch(
                                    checked = respectAgentPositioning,
                                    onCheckedChange = onRespectAgentPositioningChange,
                                    thumbContent = {
                                        Icon(
                                            painter =
                                                painterResource(
                                                    id = if (respectAgentPositioning) R.drawable.check else R.drawable.close,
                                                ),
                                            contentDescription = null,
                                            modifier = Modifier.size(SwitchDefaults.IconSize),
                                        )
                                    },
                                )
                            },
                            onClick = { onRespectAgentPositioningChange(!respectAgentPositioning) },
                        ),
                    )
                    add(
                        Material3SettingsItem(
                            icon = painterResource(R.drawable.lyrics),
                            title = { Text(stringResource(R.string.lyrics_click_change)) },
                            trailingContent = {
                                Switch(
                                    checked = lyricsClick,
                                    onCheckedChange = onLyricsClickChange,
                                    thumbContent = {
                                        Icon(
                                            painter =
                                                painterResource(
                                                    id = if (lyricsClick) R.drawable.check else R.drawable.close,
                                                ),
                                            contentDescription = null,
                                            modifier = Modifier.size(SwitchDefaults.IconSize),
                                        )
                                    },
                                )
                            },
                            onClick = { onLyricsClickChange(!lyricsClick) },
                        ),
                    )
                    add(
                        Material3SettingsItem(
                            icon = painterResource(R.drawable.lyrics),
                            title = { Text(stringResource(R.string.lyrics_auto_scroll)) },
                            trailingContent = {
                                Switch(
                                    checked = lyricsScroll,
                                    onCheckedChange = onLyricsScrollChange,
                                    thumbContent = {
                                        Icon(
                                            painter =
                                                painterResource(
                                                    id = if (lyricsScroll) R.drawable.check else R.drawable.close,
                                                ),
                                            contentDescription = null,
                                            modifier = Modifier.size(SwitchDefaults.IconSize),
                                        )
                                    },
                                )
                            },
                            onClick = { onLyricsScrollChange(!lyricsScroll) },
                        ),
                    )
                    add(
                        Material3SettingsItem(
                            icon = painterResource(R.drawable.lyrics),
                            title = { Text(stringResource(R.string.hide_status_bar_fullscreen)) },
                            description = { Text(stringResource(R.string.hide_status_bar_fullscreen_desc)) },
                            trailingContent = {
                                Switch(
                                    checked = hideStatusBarOnFullscreen,
                                    onCheckedChange = onHideStatusBarOnFullscreenChange,
                                    thumbContent = {
                                        Icon(
                                            painter =
                                                painterResource(
                                                    id = if (hideStatusBarOnFullscreen) R.drawable.check else R.drawable.close,
                                                ),
                                            contentDescription = null,
                                            modifier = Modifier.size(SwitchDefaults.IconSize),
                                        )
                                    },
                                )
                            },
                            onClick = { onHideStatusBarOnFullscreenChange(!hideStatusBarOnFullscreen) },
                        ),
                    )
                },
        )

        Spacer(modifier = Modifier.height(27.dp))

        Material3SettingsGroup(
            title = stringResource(R.string.misc),
            items =
                listOf(
                    Material3SettingsItem(
                        icon = painterResource(R.drawable.nav_bar),
                        title = { Text(stringResource(R.string.default_open_tab)) },
                        description = {
                            Text(
                                when (defaultOpenTab) {
                                    NavigationTab.HOME -> stringResource(R.string.home)
                                    NavigationTab.SEARCH -> stringResource(R.string.search)
                                    NavigationTab.LIBRARY -> stringResource(R.string.filter_library)
                                },
                            )
                        },
                        onClick = { showDefaultOpenTabDialog = true },
                    ),
                    Material3SettingsItem(
                        icon = painterResource(R.drawable.tab),
                        title = { Text(stringResource(R.string.default_lib_chips)) },
                        description = {
                            Text(
                                when (defaultChip) {
                                    LibraryFilter.SONGS -> stringResource(R.string.songs)
                                    LibraryFilter.ARTISTS -> stringResource(R.string.artists)
                                    LibraryFilter.ALBUMS -> stringResource(R.string.albums)
                                    LibraryFilter.PLAYLISTS -> stringResource(R.string.playlists)
                                    LibraryFilter.PODCASTS -> stringResource(R.string.filter_podcasts)
                                    LibraryFilter.LIBRARY -> stringResource(R.string.filter_library)
                                },
                            )
                        },
                        onClick = { showDefaultChipDialog = true },
                    ),
                    Material3SettingsItem(
                        icon = painterResource(R.drawable.swipe),
                        title = { Text(stringResource(R.string.swipe_song_to_add)) },
                        trailingContent = {
                            Switch(
                                checked = swipeToSong,
                                onCheckedChange = onSwipeToSongChange,
                                thumbContent = {
                                    Icon(
                                        painter =
                                            painterResource(
                                                id = if (swipeToSong) R.drawable.check else R.drawable.close,
                                            ),
                                        contentDescription = null,
                                        modifier = Modifier.size(SwitchDefaults.IconSize),
                                    )
                                },
                            )
                        },
                        onClick = { onSwipeToSongChange(!swipeToSong) },
                    ),
                    Material3SettingsItem(
                        icon = painterResource(R.drawable.swipe),
                        title = { Text(stringResource(R.string.swipe_song_to_remove)) },
                        trailingContent = {
                            Switch(
                                checked = swipeToRemoveSong,
                                onCheckedChange = onSwipeToRemoveSongChange,
                                thumbContent = {
                                    Icon(
                                        painter =
                                            painterResource(
                                                id = if (swipeToRemoveSong) R.drawable.check else R.drawable.close,
                                            ),
                                        contentDescription = null,
                                        modifier = Modifier.size(SwitchDefaults.IconSize),
                                    )
                                },
                            )
                        },
                        onClick = { onSwipeToRemoveSongChange(!swipeToRemoveSong) },
                    ),
                    Material3SettingsItem(
                        icon = painterResource(R.drawable.nav_bar),
                        title = { Text(stringResource(R.string.force_bottom_navbar)) },
                        trailingContent = {
                            Switch(
                                checked = forceBottomNav,
                                onCheckedChange = onForceBottomNavChange,
                                thumbContent = {
                                    Icon(
                                        painter =
                                            painterResource(
                                                id = if (forceBottomNav) R.drawable.check else R.drawable.close,
                                            ),
                                        contentDescription = null,
                                        modifier = Modifier.size(SwitchDefaults.IconSize),
                                    )
                                },
                            )
                        },
                        onClick = { onForceBottomNavChange(!forceBottomNav) },
                    ),
                    Material3SettingsItem(
                        icon = painterResource(R.drawable.nav_bar),
                        title = { Text(stringResource(R.string.slim_navbar)) },
                        trailingContent = {
                            Switch(
                                checked = slimNav,
                                onCheckedChange = onSlimNavChange,
                                thumbContent = {
                                    Icon(
                                        painter =
                                            painterResource(
                                                id = if (slimNav) R.drawable.check else R.drawable.close,
                                            ),
                                        contentDescription = null,
                                        modifier = Modifier.size(SwitchDefaults.IconSize),
                                    )
                                },
                            )
                        },
                        onClick = { onSlimNavChange(!slimNav) },
                    ),
                    Material3SettingsItem(
                        icon = painterResource(R.drawable.group_outlined),
                        title = { Text(stringResource(R.string.listen_together_in_top_bar)) },
                        description = { Text(stringResource(R.string.listen_together_in_top_bar_desc)) },
                        trailingContent = {
                            Switch(
                                checked = listenTogetherInTopBar,
                                onCheckedChange = onListenTogetherInTopBarChange,
                                thumbContent = {
                                    Icon(
                                        painter =
                                            painterResource(
                                                id = if (listenTogetherInTopBar) R.drawable.check else R.drawable.close,
                                            ),
                                        contentDescription = null,
                                        modifier = Modifier.size(SwitchDefaults.IconSize),
                                    )
                                },
                            )
                        },
                        onClick = { onListenTogetherInTopBarChange(!listenTogetherInTopBar) },
                    ),
                    Material3SettingsItem(
                        icon = painterResource(R.drawable.grid_view),
                        title = { Text(stringResource(R.string.grid_cell_size)) },
                        description = {
                            Text(
                                when (gridItemSize) {
                                    GridItemSize.BIG -> stringResource(R.string.big)
                                    GridItemSize.SMALL -> stringResource(R.string.small)
                                },
                            )
                        },
                        onClick = { showGridSizeDialog = true },
                    ),
                    Material3SettingsItem(
                        icon = painterResource(R.drawable.grid_view),
                        title = { Text(stringResource(R.string.display_density)) },
                        description = {
                            Text(DensityScale.fromValue(densityScale).label)
                        },
                        onClick = { showDensityScaleDialog = true },
                    ),
                ),
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (showExperimentalLyricsBetaDialog) {
            DefaultDialog(
                onDismiss = { showExperimentalLyricsBetaDialog = false },
                title = { Text(stringResource(R.string.experimental_lyrics_beta_title)) },
                buttons = {
                    TextButton(onClick = { showExperimentalLyricsBetaDialog = false }) {
                        Text(stringResource(R.string.cancel))
                    }
                    TextButton(onClick = {
                        showExperimentalLyricsBetaDialog = false
                        onExperimentalLyricsChange(true)
                    }) {
                        Text(stringResource(R.string.enable))
                    }
                },
            ) {
                Text(stringResource(R.string.experimental_lyrics_beta_message))
            }
        }
    }

    TopAppBar(
        title = { Text(stringResource(R.string.appearance)) },
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

enum class DarkMode {
    ON,
    OFF,
    AUTO,
}

enum class NavigationTab {
    HOME,
    SEARCH,
    LIBRARY,
}

enum class LyricsPosition {
    LEFT,
    CENTER,
    RIGHT,
}

