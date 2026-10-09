/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.component

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.metrolist.music.ui.screens.Screens
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest

@Stable
private fun isRouteSelected(currentRoute: String?, screenRoute: String, navigationItems: List<Screens>): Boolean {
    if (currentRoute == null) return false
    if (currentRoute == screenRoute) return true
    if (navigationItems.any { it.route == screenRoute } &&
        currentRoute.startsWith("$screenRoute/")) return true

    // Fix: match the route template, not the resolved route
    if (screenRoute == "search_input" &&
        (currentRoute.startsWith("search/") || currentRoute == "search/{query}")) return true

    return false
}

@Composable
fun AppNavigationRail(
    navigationItems: List<Screens>,
    currentRoute: String?,
    onItemClick: (Screens, Boolean) -> Unit,
    modifier: Modifier = Modifier,
    pureBlack: Boolean = false,
    onSearchLongClick: (() -> Unit)? = null,
    onHomeLongHold: (() -> Unit)? = null,
) {
    if (LocalLiquidBackdrop.current != null) {
        LiquidGlassNavigationRail(
            navigationItems = navigationItems,
            currentRoute = currentRoute,
            onItemClick = onItemClick,
            modifier = modifier,
            pureBlack = pureBlack,
            onSearchLongClick = onSearchLongClick,
            onHomeLongHold = onHomeLongHold,
        )
        return
    }

    val containerColor =
        if (LocalRetroStyle.current) {
            RetroPalette.Plastic
        } else if (pureBlack) {
            Color.Black
        } else {
            MaterialTheme.colorScheme.surfaceContainer
        }
    val haptics = LocalHapticFeedback.current
    val viewConfiguration = LocalViewConfiguration.current

    NavigationRail(
        modifier = modifier,
        containerColor = containerColor
    ) {
        Spacer(modifier = Modifier.weight(1f))

        navigationItems.forEach { screen ->
            val isSelected = remember(currentRoute, screen.route) {
                isRouteSelected(currentRoute, screen.route, navigationItems)
            }
            val currentIsSelected by rememberUpdatedState(isSelected)
            val iconRes = remember(isSelected, screen) {
                if (isSelected) screen.iconIdActive else screen.iconIdInactive
            }

            val isSearchItem = screen == Screens.Search && onSearchLongClick != null
            val isHomeHoldItem = screen == Screens.Home && onHomeLongHold != null
            val interactionSource = remember { MutableInteractionSource() }

            // Long press detection using InteractionSource
            if (isSearchItem || isHomeHoldItem) {
                LaunchedEffect(interactionSource) {
                    var isLongClick = false
                    interactionSource.interactions.collectLatest { interaction ->
                        when (interaction) {
                            is PressInteraction.Press -> {
                                isLongClick = false
                                delay(if (isHomeHoldItem) 15_000L else viewConfiguration.longPressTimeoutMillis)
                                isLongClick = true
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                if (isHomeHoldItem) onHomeLongHold.invoke() else onSearchLongClick?.invoke()
                            }
                            is PressInteraction.Release -> {
                                if (!isLongClick) {
                                    onItemClick(screen, currentIsSelected)
                                }
                            }
                            is PressInteraction.Cancel -> {
                                isLongClick = false
                            }
                        }
                    }
                }
            }

            NavigationRailItem(
                selected = isSelected,
                onClick = {
                    if (!isSearchItem && !isHomeHoldItem) {
                        onItemClick(screen, currentIsSelected)
                    }
                    // Long presses are handled via InteractionSource
                },
                interactionSource = interactionSource,
                icon = {
                    Icon(
                        painter = painterResource(id = iconRes),
                        contentDescription = stringResource(screen.titleId)
                    )
                }
            )
        }

        Spacer(modifier = Modifier.weight(1f))
    }
}

@Composable
fun AppNavigationBar(
    navigationItems: List<Screens>,
    currentRoute: String?,
    onItemClick: (Screens, Boolean) -> Unit,
    modifier: Modifier = Modifier,
    pureBlack: Boolean = false,
    slimNav: Boolean = false,
    onSearchLongClick: (() -> Unit)? = null,
    onHomeLongHold: (() -> Unit)? = null,
) {
    if (LocalRetroStyle.current) {
        RetroNavigationBar(
            navigationItems = navigationItems,
            currentRoute = currentRoute,
            onItemClick = onItemClick,
            modifier = modifier,
            slimNav = slimNav,
            onSearchLongClick = onSearchLongClick,
            onHomeLongHold = onHomeLongHold,
        )
        return
    }

    if (LocalLiquidBackdrop.current != null) {
        LiquidGlassNavigationBar(
            navigationItems = navigationItems,
            currentRoute = currentRoute,
            onItemClick = onItemClick,
            modifier = modifier,
            pureBlack = pureBlack,
            slimNav = slimNav,
            onSearchLongClick = onSearchLongClick,
            onHomeLongHold = onHomeLongHold,
        )
        return
    }

    val containerColor = if (pureBlack) Color.Black else MaterialTheme.colorScheme.surfaceContainer
    val contentColor = if (pureBlack) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
    val haptics = LocalHapticFeedback.current
    val viewConfiguration = LocalViewConfiguration.current

    NavigationBar(
        modifier = modifier,
        containerColor = containerColor,
        contentColor = contentColor
    ) {
        navigationItems.forEach { screen ->
            val isSelected = remember(currentRoute, screen.route) {
                isRouteSelected(currentRoute, screen.route, navigationItems)
            }
            val currentIsSelected by rememberUpdatedState(isSelected)
            val iconRes = remember(isSelected, screen) {
                if (isSelected) screen.iconIdActive else screen.iconIdInactive
            }

            val isSearchItem = screen == Screens.Search && onSearchLongClick != null
            val isHomeHoldItem = screen == Screens.Home && onHomeLongHold != null
            val interactionSource = remember { MutableInteractionSource() }

            // Long press detection using InteractionSource
            if (isSearchItem || isHomeHoldItem) {
                LaunchedEffect(interactionSource) {
                    var isLongClick = false
                    interactionSource.interactions.collectLatest { interaction ->
                        when (interaction) {
                            is PressInteraction.Press -> {
                                isLongClick = false
                                delay(if (isHomeHoldItem) 15_000L else viewConfiguration.longPressTimeoutMillis)
                                isLongClick = true
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                if (isHomeHoldItem) onHomeLongHold.invoke() else onSearchLongClick?.invoke()
                            }
                            is PressInteraction.Release -> {
                                if (!isLongClick) {
                                    onItemClick(screen, currentIsSelected)
                                }
                            }
                            is PressInteraction.Cancel -> {
                                isLongClick = false
                            }
                        }
                    }
                }
            }

            NavigationBarItem(
                selected = isSelected,
                onClick = {
                    if (!isSearchItem && !isHomeHoldItem) {
                        onItemClick(screen, currentIsSelected)
                    }
                    // Long presses are handled via InteractionSource
                },
                interactionSource = interactionSource,
                icon = {
                    Icon(
                        painter = painterResource(id = iconRes),
                        contentDescription = stringResource(screen.titleId)
                    )
                },
                label = if (!slimNav) {
                    {
                        Text(
                            text = stringResource(screen.titleId),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                } else null
            )
        }
    }
}

@Composable
private fun LiquidGlassNavigationBar(
    navigationItems: List<Screens>,
    currentRoute: String?,
    onItemClick: (Screens, Boolean) -> Unit,
    modifier: Modifier,
    pureBlack: Boolean,
    slimNav: Boolean,
    onSearchLongClick: (() -> Unit)?,
    onHomeLongHold: (() -> Unit)?,
) {
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val selectedIndex =
        remember(currentRoute, navigationItems) {
            navigationItems.indexOfFirst { isRouteSelected(currentRoute, it.route, navigationItems) }
        }
    val barFallback =
        if (pureBlack) Color.Black.copy(alpha = 0.92f) else MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.94f)
    val unselectedColor = if (pureBlack) Color.White.copy(alpha = 0.75f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)

    Box(
        modifier = modifier.padding(horizontal = 20.dp),
        contentAlignment = Alignment.TopCenter,
    ) {
        BoxWithConstraints(
            modifier =
                Modifier
                    .padding(top = 6.dp)
                    .widthIn(max = 520.dp)
                    .fillMaxWidth()
                    .height(if (slimNav) 56.dp else 68.dp)
                    .liquidGlass(
                        cornerRadius = 40.dp,
                        fallbackColor = barFallback,
                    ).padding(5.dp),
        ) {
            val itemWidth = maxWidth / navigationItems.size
            val pillOffset by animateDpAsState(
                targetValue = itemWidth * selectedIndex.coerceAtLeast(0),
                animationSpec = spring(dampingRatio = 0.62f, stiffness = Spring.StiffnessMediumLow),
                label = "liquidGlassPill",
            )

            if (selectedIndex >= 0) {
                Box(
                    modifier =
                        Modifier
                            .graphicsLayer { translationX = pillOffset.toPx() }
                            .width(itemWidth)
                            .fillMaxHeight()
                            .liquidGlass(
                                cornerRadius = 40.dp,
                                fallbackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                                refraction = 10.dp,
                                bezel = 16.dp,
                                blur = 0.5.dp,
                                chromaticAberration = 0.25f,
                                tint = Color.White.copy(alpha = if (isDark) 0.12f else 0.38f),
                            ),
                )
            }

            Row(modifier = Modifier.fillMaxSize()) {
                navigationItems.forEach { screen ->
                    val isSelected = selectedIndex >= 0 && navigationItems[selectedIndex] == screen
                    val isSearchItem = screen == Screens.Search && onSearchLongClick != null
                    val isHomeHoldItem = screen == Screens.Home && onHomeLongHold != null

                    LiquidGlassNavItem(
                        screen = screen,
                        isSelected = isSelected,
                        showLabel = !slimNav,
                        selectedColor = MaterialTheme.colorScheme.primary,
                        unselectedColor = unselectedColor,
                        longPressDelayMs = if (isHomeHoldItem) 15_000L else null,
                        onLongPress =
                            when {
                                isHomeHoldItem -> onHomeLongHold
                                isSearchItem -> onSearchLongClick
                                else -> null
                            },
                        onClick = { onItemClick(screen, isSelected) },
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                    )
                }
            }
        }
    }
}

@Composable
private fun RetroNavigationBar(
    navigationItems: List<Screens>,
    currentRoute: String?,
    onItemClick: (Screens, Boolean) -> Unit,
    modifier: Modifier,
    slimNav: Boolean,
    onSearchLongClick: (() -> Unit)?,
    onHomeLongHold: (() -> Unit)?,
) {
    val selectedIndex =
        remember(currentRoute, navigationItems) {
            navigationItems.indexOfFirst { isRouteSelected(currentRoute, it.route, navigationItems) }
        }
    val keyShape = RoundedCornerShape(10.dp)

    Box(
        modifier = modifier.fillMaxWidth().retroDeck(),
        contentAlignment = Alignment.TopCenter,
    ) {
        Row(
            modifier =
                Modifier
                    .padding(top = 8.dp)
                    .widthIn(max = 520.dp)
                    .fillMaxWidth()
                    .height(if (slimNav) 52.dp else 62.dp)
                    .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            navigationItems.forEach { screen ->
                val isSelected = selectedIndex >= 0 && navigationItems[selectedIndex] == screen
                val isSearchItem = screen == Screens.Search && onSearchLongClick != null
                val isHomeHoldItem = screen == Screens.Home && onHomeLongHold != null

                LiquidGlassNavItem(
                    screen = screen,
                    isSelected = isSelected,
                    showLabel = !slimNav,
                    selectedColor = RetroPalette.Amber,
                    unselectedColor = RetroPalette.Cream.copy(alpha = 0.85f),
                    longPressDelayMs = if (isHomeHoldItem) 15_000L else null,
                    onLongPress =
                        when {
                            isHomeHoldItem -> onHomeLongHold
                            isSearchItem -> onSearchLongClick
                            else -> null
                        },
                    onClick = { onItemClick(screen, isSelected) },
                    modifier =
                        Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .retroKey(keyShape, pressed = isSelected, led = isSelected),
                )
            }
        }
    }
}

@Composable
private fun LiquidGlassNavigationRail(
    navigationItems: List<Screens>,
    currentRoute: String?,
    onItemClick: (Screens, Boolean) -> Unit,
    modifier: Modifier,
    pureBlack: Boolean,
    onSearchLongClick: (() -> Unit)?,
    onHomeLongHold: (() -> Unit)?,
) {
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val selectedIndex =
        remember(currentRoute, navigationItems) {
            navigationItems.indexOfFirst { isRouteSelected(currentRoute, it.route, navigationItems) }
        }
    val railFallback =
        if (pureBlack) Color.Black.copy(alpha = 0.92f) else MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.94f)
    val unselectedColor = if (pureBlack) Color.White.copy(alpha = 0.75f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
    val itemHeight = 56.dp

    Box(
        modifier =
            modifier
                .fillMaxHeight()
                .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Start + WindowInsetsSides.Vertical))
                .padding(horizontal = 8.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier =
                Modifier
                    .width(64.dp)
                    .liquidGlass(
                        cornerRadius = 32.dp,
                        fallbackColor = railFallback,
                    ).padding(5.dp),
        ) {
            val pillOffset by animateDpAsState(
                targetValue = itemHeight * selectedIndex.coerceAtLeast(0),
                animationSpec = spring(dampingRatio = 0.62f, stiffness = Spring.StiffnessMediumLow),
                label = "liquidGlassRailPill",
            )

            if (selectedIndex >= 0) {
                Box(
                    modifier =
                        Modifier
                            .graphicsLayer { translationY = pillOffset.toPx() }
                            .fillMaxWidth()
                            .height(itemHeight)
                            .liquidGlass(
                                cornerRadius = 32.dp,
                                fallbackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                                refraction = 10.dp,
                                bezel = 16.dp,
                                blur = 0.5.dp,
                                chromaticAberration = 0.25f,
                                tint = Color.White.copy(alpha = if (isDark) 0.12f else 0.38f),
                            ),
                )
            }

            Column {
                navigationItems.forEach { screen ->
                    val isSelected = selectedIndex >= 0 && navigationItems[selectedIndex] == screen
                    val isSearchItem = screen == Screens.Search && onSearchLongClick != null
                    val isHomeHoldItem = screen == Screens.Home && onHomeLongHold != null

                    LiquidGlassNavItem(
                        screen = screen,
                        isSelected = isSelected,
                        showLabel = false,
                        selectedColor = MaterialTheme.colorScheme.primary,
                        unselectedColor = unselectedColor,
                        longPressDelayMs = if (isHomeHoldItem) 15_000L else null,
                        onLongPress =
                            when {
                                isHomeHoldItem -> onHomeLongHold
                                isSearchItem -> onSearchLongClick
                                else -> null
                            },
                        onClick = { onItemClick(screen, isSelected) },
                        modifier = Modifier.fillMaxWidth().height(itemHeight),
                    )
                }
            }
        }
    }
}

@Composable
private fun LiquidGlassNavItem(
    screen: Screens,
    isSelected: Boolean,
    showLabel: Boolean,
    selectedColor: Color,
    unselectedColor: Color,
    longPressDelayMs: Long?,
    onLongPress: (() -> Unit)?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = LocalHapticFeedback.current
    val viewConfiguration = LocalViewConfiguration.current
    val interactionSource = remember { MutableInteractionSource() }
    val currentOnClick by rememberUpdatedState(onClick)
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 1.14f else 1f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMedium),
        label = "liquidGlassItemScale",
    )

    if (onLongPress != null) {
        LaunchedEffect(interactionSource) {
            var isLongClick = false
            interactionSource.interactions.collectLatest { interaction ->
                when (interaction) {
                    is PressInteraction.Press -> {
                        isLongClick = false
                        delay(longPressDelayMs ?: viewConfiguration.longPressTimeoutMillis)
                        isLongClick = true
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        onLongPress.invoke()
                    }
                    is PressInteraction.Release -> {
                        if (!isLongClick) currentOnClick()
                    }
                    is PressInteraction.Cancel -> {
                        isLongClick = false
                    }
                }
            }
        }
    }

    val color = if (isSelected) selectedColor else unselectedColor
    val iconRes = if (isSelected) screen.iconIdActive else screen.iconIdInactive

    Column(
        modifier =
            modifier
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    role = Role.Tab,
                    onClick = { if (onLongPress == null) currentOnClick() },
                ).graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            painter = painterResource(id = iconRes),
            contentDescription = stringResource(screen.titleId),
            tint = color,
        )
        if (showLabel) {
            Text(
                text = stringResource(screen.titleId),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                color = color,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
