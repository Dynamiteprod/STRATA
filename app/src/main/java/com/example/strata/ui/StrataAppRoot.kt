package com.example.strata.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Album
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.SpeakerGroup
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.SpeakerGroup
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.strata.domain.NetworkContext
import com.example.strata.domain.SubscriptionPlan
import com.example.strata.ui.screens.AccountScreen
import com.example.strata.ui.screens.ArtistScreen
import com.example.strata.ui.screens.DiscoverScreen
import com.example.strata.ui.screens.ExpandedStudioPlayer
import com.example.strata.ui.screens.MiniPlayerBar
import com.example.strata.ui.screens.OrchestraScreen
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.StudioSurface
import com.example.ui.theme.TerracottaPrimary
import com.example.ui.theme.TextMuted

private const val ROUTE_DISCOVER = "discover"
private const val ROUTE_ORCHESTRA = "orchestra"
private const val ROUTE_ARTIST = "artist"
private const val ROUTE_ACCOUNT = "account"

private data class StrataNavItem(
    val route: String,
    val label: String,
    val activeIcon: ImageVector,
    val inactiveIcon: ImageVector
)

private val NAV_ITEMS = listOf(
    StrataNavItem(ROUTE_DISCOVER, "Découvrir", Icons.Rounded.GraphicEq, Icons.Outlined.GraphicEq),
    StrataNavItem(ROUTE_ORCHESTRA, "Orchestre", Icons.Rounded.SpeakerGroup, Icons.Outlined.SpeakerGroup),
    StrataNavItem(ROUTE_ARTIST, "Artiste", Icons.Rounded.Album, Icons.Outlined.Album),
    StrataNavItem(ROUTE_ACCOUNT, "Compte", Icons.Rounded.AccountCircle, Icons.Outlined.AccountCircle)
)

@Composable
fun StrataAppRoot(viewModel: StrataViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val playback by viewModel.playbackState.collectAsStateWithLifecycle()
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: ROUTE_DISCOVER
    val snackbarHostState = remember { SnackbarHostState() }
    val haptics = LocalHapticFeedback.current
    val lastBar = remember { mutableIntStateOf(-1) }

    LaunchedEffect(uiState.statusToast) {
        val msg = uiState.statusToast
        if (msg != null) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearToast()
        }
    }

    LaunchedEffect(playback.spectrum.currentBarNumber, uiState.hapticOnDownbeat, playback.isPlaying) {
        if (uiState.hapticOnDownbeat && playback.isPlaying) {
            val bar = playback.spectrum.currentBarNumber
            if (bar != lastBar.intValue) {
                lastBar.intValue = bar
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            }
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isWideScreen = maxWidth >= 840.dp

        Scaffold(
            containerColor = ObsidianBg,
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            bottomBar = {
                if (!isWideScreen) {
                    Column {
                        if (playback.currentTrack != null) {
                            MiniPlayerBar(
                                playback = playback,
                                currentDepth = uiState.currentDepth,
                                onExpand = { viewModel.setPlayerExpanded(true) },
                                onTogglePlay = { viewModel.togglePlayPause() },
                                onNext = { viewModel.playNextTrack() }
                            )
                        }
                        NavigationBar(
                            containerColor = StudioSurface,
                            windowInsets = WindowInsets.navigationBars
                        ) {
                            NAV_ITEMS.forEach { item ->
                                val selected = currentRoute == item.route
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = {
                                        navController.navigate(item.route) {
                                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    },
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) item.activeIcon else item.inactiveIcon,
                                            contentDescription = item.label
                                        )
                                    },
                                    label = { Text(item.label) },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = TerracottaPrimary,
                                        selectedTextColor = TerracottaPrimary,
                                        indicatorColor = TerracottaPrimary.copy(alpha = 0.16f),
                                        unselectedIconColor = TextMuted,
                                        unselectedTextColor = TextMuted
                                    ),
                                    modifier = Modifier.testTag("nav_${item.route}")
                                )
                            }
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (isWideScreen) {
                    NavigationRail(
                        containerColor = StudioSurface,
                        modifier = Modifier.statusBarsPadding()
                    ) {
                        NAV_ITEMS.forEach { item ->
                            val selected = currentRoute == item.route
                            NavigationRailItem(
                                selected = selected,
                                onClick = {
                                    navController.navigate(item.route) {
                                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) item.activeIcon else item.inactiveIcon,
                                        contentDescription = item.label
                                    )
                                },
                                label = { Text(item.label) },
                                modifier = Modifier.testTag("nav_rail_${item.route}")
                            )
                        }
                    }
                }

                Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                    NavHost(
                        navController = navController,
                        startDestination = ROUTE_DISCOVER,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        composable(ROUTE_DISCOVER) {
                            DiscoverScreen(
                                uiState = uiState,
                                filteredTracks = viewModel.filteredTracks(),
                                currentPlayingTrackId = playback.currentTrack?.id,
                                onSearchChange = { viewModel.updateSearchQuery(it) },
                                onGenreSelect = { viewModel.selectGenre(it) },
                                onToggleOfflineFilter = { viewModel.toggleOfflineFilter() },
                                onToggleNetworkContext = {
                                    val next = if (uiState.networkContext == NetworkContext.CELLULAR) {
                                        NetworkContext.WIFI
                                    } else {
                                        NetworkContext.CELLULAR
                                    }
                                    viewModel.setNetworkContext(next)
                                },
                                onPlayTrack = { viewModel.playTrack(it, expandPlayer = true) },
                                onToggleOfflineTrack = { viewModel.toggleTrackOffline(it) },
                                onPlayFork = { fork, baseTrack ->
                                    viewModel.playTrack(baseTrack, expandPlayer = true, fork = fork)
                                }
                            )
                        }
                        composable(ROUTE_ORCHESTRA) {
                            OrchestraScreen(
                                uiState = uiState,
                                activeTrack = playback.currentTrack,
                                viewModel = viewModel
                            )
                        }
                        composable(ROUTE_ARTIST) {
                            ArtistScreen(uiState = uiState, viewModel = viewModel)
                        }
                        composable(ROUTE_ACCOUNT) {
                            AccountScreen(uiState = uiState, viewModel = viewModel)
                        }
                    }

                    if (isWideScreen && playback.currentTrack != null) {
                        MiniPlayerBar(
                            playback = playback,
                            currentDepth = uiState.currentDepth,
                            onExpand = { viewModel.setPlayerExpanded(true) },
                            onTogglePlay = { viewModel.togglePlayPause() },
                            onNext = { viewModel.playNextTrack() },
                            modifier = Modifier.align(Alignment.BottomCenter)
                        )
                    }
                }
            }
        }

        // Full-Screen Single-Surface STRATA Player Overlay
        AnimatedVisibility(
            visible = uiState.isPlayerExpanded,
            enter = slideInVertically { it },
            exit = slideOutVertically { it }
        ) {
            ExpandedStudioPlayer(
                uiState = uiState,
                playback = playback,
                viewModel = viewModel,
                onCollapse = { viewModel.setPlayerExpanded(false) }
            )
        }

        // Plan upgrade prompt when restricted cran is tapped
        if (uiState.showPlanUpgradeSheet) {
            AlertDialog(
                onDismissRequest = { viewModel.setShowPlanUpgradeSheet(false) },
                containerColor = StudioSurface,
                title = { Text("Débloquer les couches studio") },
                text = {
                    Text(
                        "Les crans 3–4 (stems, EQ 3 bandes, export, forks) sont inclus dans le Pass Jour (100 FCFA) ou l'offre Pro (5 000 FCFA/mois) via Mobile Money."
                    )
                },
                confirmButton = {
                    Button(
                        onClick = { viewModel.setPlan(SubscriptionPlan.PASS_DAY) },
                        modifier = Modifier.testTag("upgrade_pass_day_btn")
                    ) {
                        Text("Activer Pass Jour (100 FCFA)")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.setShowPlanUpgradeSheet(false) }) {
                        Text("Plus tard")
                    }
                }
            )
        }
    }
}
