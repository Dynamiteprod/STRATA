package com.example.strata.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountTree
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AudioFile
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.MicOff
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.strata.data.StrataCatalog
import com.example.strata.domain.DepthLevel
import com.example.strata.domain.StemType
import com.example.strata.domain.StreamingQuality
import com.example.strata.domain.Track
import com.example.strata.engine.MultiStemAudioEngine
import com.example.strata.ui.StrataViewModel
import com.example.ui.theme.KoraGold
import com.example.ui.theme.LaguneTeal
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioSurface
import com.example.ui.theme.StudioSurfaceHigh
import com.example.ui.theme.TerracottaPrimary
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSecondary

@Composable
fun MiniPlayerBar(
    playback: MultiStemAudioEngine.PlaybackState,
    currentDepth: DepthLevel,
    onExpand: () -> Unit,
    onTogglePlay: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    val track = playback.currentTrack ?: return
    val progress = (playback.positionMs.toFloat() / playback.durationMs.coerceAtLeast(1L).toFloat()).coerceIn(0f, 1f)

    Surface(
        color = StudioSurfaceHigh,
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 6.dp,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .border(1.dp, StudioBorder, RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onExpand)
            .testTag("mini_player_bar")
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Image(
                    painter = painterResource(id = track.coverResId),
                    contentDescription = "Pochette ${track.title}",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(10.dp))
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = playback.activeFork?.title ?: track.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Cran ${currentDepth.level} • ${currentDepth.labelFr}",
                            style = MaterialTheme.typography.labelSmall,
                            color = TerracottaPrimary
                        )
                        Text(
                            text = if (track.localUriString != null) "Téléphone" else "${(track.analysis.tempo.bpm * playback.tempoMultiplier).toInt()} BPM",
                            style = MaterialTheme.typography.labelSmall,
                            color = KoraGold
                        )
                    }
                }
                IconButton(
                    onClick = onTogglePlay,
                    modifier = Modifier.testTag("mini_play_pause_btn")
                ) {
                    Icon(
                        imageVector = if (playback.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = if (playback.isPlaying) "Pause" else "Lecture",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
                IconButton(
                    onClick = onNext,
                    modifier = Modifier.testTag("mini_next_btn")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.SkipNext,
                        contentDescription = "Morceau suivant",
                        tint = TextSecondary
                    )
                }
            }
            LinearProgressIndicator(
                progress = { progress },
                color = TerracottaPrimary,
                trackColor = StudioBorder,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.5.dp)
            )
        }
    }
}

@Composable
fun ExpandedStudioPlayer(
    uiState: StrataViewModel.UiState,
    playback: MultiStemAudioEngine.PlaybackState,
    viewModel: StrataViewModel,
    onCollapse: () -> Unit
) {
    BackHandler { onCollapse() }

    val track = playback.currentTrack ?: return
    val depth = uiState.currentDepth
    val links = remember(track.id) { StrataCatalog.linksForTrack(track.id) }
    var selectedStudioStem by remember { mutableStateOf(StemType.BASS) }
    var dragAccum by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(StudioSurfaceHigh, ObsidianBg, ObsidianBg)
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .widthIn(max = 560.dp)
                .fillMaxWidth()
        ) {
            // 1. Sleek Top Header: Collapse + Lineage Pill + Share
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = onCollapse,
                    modifier = Modifier.testTag("collapse_player_btn")
                ) {
                    Icon(
                        Icons.Rounded.KeyboardArrowDown,
                        contentDescription = "Réduire le lecteur",
                        modifier = Modifier.size(28.dp)
                    )
                }

                Surface(
                    onClick = { viewModel.setShowGenealogySheet(true) },
                    color = if (links.isNotEmpty()) TerracottaPrimary.copy(alpha = 0.18f) else StudioSurface,
                    shape = RoundedCornerShape(50),
                    modifier = Modifier
                        .border(
                            1.dp,
                            if (links.isNotEmpty()) TerracottaPrimary.copy(alpha = 0.65f) else StudioBorder,
                            RoundedCornerShape(50)
                        )
                        .testTag("lineage_pill_btn")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = if (track.localUriString != null) Icons.Rounded.AudioFile else Icons.Rounded.AccountTree,
                            contentDescription = "Généalogie jouable",
                            tint = when {
                                links.isNotEmpty() -> TerracottaPrimary
                                track.localUriString != null -> LaguneTeal
                                else -> TextMuted
                            },
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = when {
                                links.isNotEmpty() -> "Lignée (${links.size}) • ↕"
                                track.localUriString != null -> "Fichier du téléphone"
                                else -> "Lignée"
                            },
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1
                        )
                    }
                }

                IconButton(
                    onClick = { viewModel.shareTimestampLink() },
                    modifier = Modifier.testTag("share_timestamp_btn")
                ) {
                    Icon(
                        Icons.Rounded.Share,
                        contentDescription = "Partager ce moment",
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            // 2. Full-Width 5-Cran Studio Depth Selector (never squeezes phone width!)
            HorizontalDepthBar(
                currentDepth = depth,
                maxAllowedDepth = uiState.currentPlan.maxDepthAllowed,
                pendingDownbeat = playback.pendingMixToStemsOnDownbeat,
                onSelectDepth = { viewModel.selectDepthLevel(it) },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )

            // Pending genealogy crossfade indicator
            AnimatedVisibility(visible = playback.pendingTransition != null) {
                playback.pendingTransition?.let { pending ->
                    Surface(
                        color = KoraGold.copy(alpha = 0.18f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "Transition (${pending.linkLabel}) vers « ${pending.targetTrack.title} » calée sur le 1er temps…",
                            style = MaterialTheme.typography.labelMedium,
                            color = KoraGold,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }
            }

            // 3. Full-Width Scrollable Studio Layers
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // CRAN 0: Cover & Track Info (compact bar when depth >= 1)
                item {
                    val isCompactHeader = depth.level >= 1
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .pointerInput(track.id) {
                                detectVerticalDragGestures(
                                    onDragEnd = {
                                        if (kotlin.math.abs(dragAccum) > 70f) {
                                            viewModel.swipeNextLinkedVersion()
                                        }
                                        dragAccum = 0f
                                    },
                                    onVerticalDrag = { _, dragAmount -> dragAccum += dragAmount }
                                )
                            }
                    ) {
                        if (!isCompactHeader) {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(216.dp)
                                        .clip(RoundedCornerShape(22.dp))
                                        .border(
                                            width = if (playback.spectrum.activeBeatInBar == 1 && playback.isPlaying) 2.dp else 1.dp,
                                            color = if (playback.spectrum.activeBeatInBar == 1 && playback.isPlaying) TerracottaPrimary else StudioBorder,
                                            shape = RoundedCornerShape(22.dp)
                                        )
                                ) {
                                    Image(
                                        painter = painterResource(id = track.coverResId),
                                        contentDescription = "Pochette ${track.title}",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(
                                                Brush.verticalGradient(
                                                    listOf(Color.Transparent, ObsidianBg.copy(alpha = 0.55f))
                                                )
                                            )
                                    )
                                    Row(
                                        modifier = Modifier
                                            .align(Alignment.BottomStart)
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Surface(
                                            color = ObsidianBg.copy(alpha = 0.80f),
                                            shape = RoundedCornerShape(50)
                                        ) {
                                            Text(
                                                text = "Mesure ${playback.spectrum.currentBarNumber} • Temps ${playback.spectrum.activeBeatInBar}/4",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = KoraGold,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                            )
                                        }
                                        Surface(
                                            color = ObsidianBg.copy(alpha = 0.80f),
                                            shape = RoundedCornerShape(50)
                                        ) {
                                            Text(
                                                text = if (track.localUriString != null) {
                                                    "Audio Local • 4 Stems"
                                                } else {
                                                    "${uiState.streamingQuality.label} ${uiState.streamingQuality.kbps} kb/s"
                                                },
                                                style = MaterialTheme.typography.labelSmall,
                                                color = LaguneTeal,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                            )
                                        }
                                    }
                                }
                                TrackHeaderInfoRow(track, playback, uiState.streamingQuality, viewModel)
                            }
                        } else {
                            Surface(
                                color = StudioSurface,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, StudioBorder, RoundedCornerShape(14.dp))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Image(
                                        painter = painterResource(id = track.coverResId),
                                        contentDescription = "Pochette ${track.title}",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = playback.activeFork?.title ?: track.title,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${track.artistName} • Mes. ${playback.spectrum.currentBarNumber}.${playback.spectrum.activeBeatInBar}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = TextSecondary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    if (track.localUriString == null) {
                                        IconButton(onClick = { viewModel.toggleTrackOffline(track.id) }) {
                                            Icon(
                                                imageVector = if (track.isDownloadedOffline) Icons.Rounded.CheckCircle else Icons.Rounded.CloudDownload,
                                                contentDescription = "Hors-ligne",
                                                tint = if (track.isDownloadedOffline) LaguneTeal else TextMuted
                                            )
                                        }
                                    } else {
                                        MetaTagPill("Local", LaguneTeal)
                                    }
                                }
                            }
                        }
                    }
                }

                // CRAN 1+: PAROLES SYNCHRONISÉES (& ACCORDS SI CRAN >= 2)
                if (depth.level >= 1) {
                    item {
                        LyricsAndChordsLayer(
                            track = track,
                            positionSec = playback.positionMs / 1000.0,
                            showChords = depth.level >= 2,
                            transposeSemitones = playback.transposeSemitones,
                            onJumpToSec = { sec -> viewModel.seekToMs((sec * 1000.0).toLong()) }
                        )
                    }
                }

                // CRAN 2+: MUSICIEN (Structure, Tempo 50-150%, Transposition, Boucle A-B)
                if (depth.level >= 2) {
                    item {
                        MusicianToolsLayer(
                            track = track,
                            playback = playback,
                            onSelectSection = { sec -> viewModel.seekToMs((sec * 1000.0).toLong()) },
                            onTempoChange = { viewModel.setTempoMultiplier(it) },
                            onTranspose = { viewModel.setTranspose(it) },
                            onLoopA = { viewModel.setLoopA() },
                            onLoopB = { viewModel.setLoopB() },
                            onClearLoop = { viewModel.clearLoop() }
                        )
                    }
                }

                // CRAN 3+: PISTES SÉPARÉES (Stems + Mode Karaoké)
                if (depth.level >= 3) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Mixage 4 pistes",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            FilterChip(
                                selected = playback.karaokeMode,
                                onClick = { viewModel.toggleKaraoke() },
                                label = { Text("Mode Karaoké") },
                                leadingIcon = {
                                    Icon(Icons.Rounded.MicOff, contentDescription = null, modifier = Modifier.size(16.dp))
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = TerracottaPrimary,
                                    selectedLabelColor = Color.White
                                ),
                                modifier = Modifier
                                    .heightIn(min = 48.dp)
                                    .testTag("karaoke_mode_btn")
                            )
                        }
                    }

                    items(StemType.entries, key = { it.id }) { stemType ->
                        val mixState = playback.stemMix[stemType] ?: return@items
                        val stemMeta = track.analysis.stems.firstOrNull { it.type == stemType }
                        val srcLabel = if (stemMeta?.source?.wire == "official") "Studio" else "IA"
                        StemChannelStrip(
                            mixState = mixState,
                            stemSourceLabel = srcLabel,
                            isSelectedForStudio = depth.level >= 4 && selectedStudioStem == stemType,
                            onSelectForStudio = { selectedStudioStem = stemType },
                            onVolumeChange = { vol -> viewModel.updateStem(stemType) { it.copy(volume = vol) } },
                            onToggleMute = { viewModel.updateStem(stemType) { it.copy(muted = !it.muted) } },
                            onToggleSolo = { viewModel.updateStem(stemType) { it.copy(solo = !it.solo) } }
                        )
                    }
                }

                // CRAN 4: STUDIO (Waveform, Pan, 3-Band EQ, Spectrum, Export, Fork)
                if (depth.level >= 4) {
                    item {
                        val activeStemState = playback.stemMix[selectedStudioStem] ?: return@item
                        StudioAnalysisPanel(
                            track = track,
                            selectedStem = activeStemState,
                            playback = playback,
                            onPanChange = { p -> viewModel.updateStem(selectedStudioStem) { it.copy(pan = p) } },
                            onEqChange = { l, m, h ->
                                viewModel.updateStem(selectedStudioStem) {
                                    it.copy(eqLowDb = l, eqMidDb = m, eqHighDb = h)
                                }
                            },
                            onExportLoop = { viewModel.exportCurrentLoop() },
                            onOpenForkDialog = { viewModel.setShowForkDialog(true) }
                        )
                    }
                }
            }

            // 4. Sleek, Proportional Bottom Transport Controls
            TransportControlsBar(
                playback = playback,
                shuffleEnabled = uiState.shuffleEnabled,
                repeatEnabled = uiState.repeatEnabled,
                onSeek = { viewModel.seekToMs(it) },
                onTogglePlay = { viewModel.togglePlayPause() },
                onPrev = { viewModel.playPreviousTrack() },
                onNext = { viewModel.playNextTrack() },
                onToggleShuffle = { viewModel.toggleShuffle() },
                onToggleRepeat = { viewModel.toggleRepeat() }
            )
        }
    }

    if (uiState.showGenealogySheet) {
        GenealogyGraphDialog(
            currentTrack = track,
            links = links,
            catalog = uiState.catalog,
            onTransitionTo = { target, link -> viewModel.transitionToLinkedVersion(target, link) },
            onDismiss = { viewModel.setShowGenealogySheet(false) }
        )
    }

    if (uiState.showForkDialog) {
        ForkCreatorDialog(
            baseTrack = track,
            onPublish = { title, rev, added -> viewModel.publishFork(title, rev, added) },
            onDismiss = { viewModel.setShowForkDialog(false) }
        )
    }

    if (uiState.showDataWarningDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissDataWarning() },
            containerColor = StudioSurface,
            title = { Text("Consommation data (réseau mobile)") },
            text = {
                Text(
                    "Les 4 stems (64 kb/s chacun) consomment environ 2× plus de data qu'un mix Normal (~6,7 Mo pour 3 min 30). Télécharger et basculer au prochain temps fort ?"
                )
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.confirmCellularStemDownload() },
                    modifier = Modifier.testTag("confirm_cellular_stems_btn")
                ) {
                    Text("Télécharger les stems")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissDataWarning() }) { Text("Rester en mix") }
            }
        )
    }
}

@Composable
private fun TrackHeaderInfoRow(
    track: Track,
    playback: MultiStemAudioEngine.PlaybackState,
    quality: StreamingQuality,
    viewModel: StrataViewModel
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = playback.activeFork?.title ?: track.title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = if (track.localUriString != null) {
                    "${track.artistName} • ${track.releaseTitle}"
                } else {
                    "${track.artistName} • ${track.genre.labelFr}"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (track.localUriString != null) {
                    MetaTagPill("Téléphone", LaguneTeal)
                    MetaTagPill("DSP 4 Pistes", KoraGold)
                } else {
                    MetaTagPill("${track.analysis.tempo.bpm.toInt()} BPM", KoraGold)
                    MetaTagPill(track.analysis.key.display(), LaguneTeal)
                    MetaTagPill(if (track.isDownloadedOffline) "Hors-ligne ✓" else "Streaming ${quality.kbps}k", TextSecondary)
                }
            }
        }
        if (track.localUriString == null) {
            IconButton(
                onClick = { viewModel.toggleTrackOffline(track.id) },
                modifier = Modifier.testTag("player_offline_btn")
            ) {
                Icon(
                    imageVector = if (track.isDownloadedOffline) Icons.Rounded.CheckCircle else Icons.Rounded.CloudDownload,
                    contentDescription = "Téléchargement hors-ligne",
                    tint = if (track.isDownloadedOffline) LaguneTeal else TextMuted
                )
            }
        }
    }
}

@Composable
private fun LyricsAndChordsLayer(
    track: Track,
    positionSec: Double,
    showChords: Boolean,
    transposeSemitones: Int,
    onJumpToSec: (Double) -> Unit
) {
    Surface(
        color = StudioSurface,
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, StudioBorder, RoundedCornerShape(18.dp))
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (showChords) "Paroles & Accords" else "Paroles synchronisées",
                    style = MaterialTheme.typography.titleMedium,
                    color = KoraGold,
                    fontWeight = FontWeight.Bold
                )
                if (showChords && !track.analysis.hasVerifiedChords && track.localUriString == null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Rounded.Warning, contentDescription = null, tint = KoraGold, modifier = Modifier.size(14.dp))
                        Text("Accords non vérifiés", style = MaterialTheme.typography.labelSmall, color = KoraGold)
                    }
                }
            }

            track.analysis.lyrics.forEach { line ->
                val isActive = positionSec >= line.startSec && positionSec < line.endSec
                val lineChord = track.analysis.chords.firstOrNull { line.startSec >= it.startSec && line.startSec < it.endSec }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isActive) TerracottaPrimary.copy(alpha = 0.16f) else Color.Transparent)
                        .clickable { onJumpToSec(line.startSec) }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    if (showChords && lineChord != null) {
                        Text(
                            text = transposeChordLabel(lineChord.label, transposeSemitones),
                            style = MaterialTheme.typography.labelMedium,
                            color = KoraGold,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = line.text,
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (isActive) Color.White else TextSecondary,
                        fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }
    }
}

@Composable
private fun MusicianToolsLayer(
    track: Track,
    playback: MultiStemAudioEngine.PlaybackState,
    onSelectSection: (Double) -> Unit,
    onTempoChange: (Float) -> Unit,
    onTranspose: (Int) -> Unit,
    onLoopA: () -> Unit,
    onLoopB: () -> Unit,
    onClearLoop: () -> Unit
) {
    val effectiveBpm = (track.analysis.tempo.bpm * playback.tempoMultiplier).toInt()
    Surface(
        color = StudioSurface,
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, KoraGold.copy(alpha = 0.4f), RoundedCornerShape(18.dp))
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Outils Musicien", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    MetaTagPill("$effectiveBpm BPM", KoraGold)
                    MetaTagPill(transposeChordLabel(track.analysis.key.display(), playback.transposeSemitones), LaguneTeal)
                }
            }

            // Structure segments (Intro / Couplet / Refrain / Pont)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(track.analysis.sections) { sec ->
                    val currentSec = playback.positionMs / 1000.0
                    val active = currentSec >= sec.startSec && currentSec < sec.endSec
                    FilterChip(
                        selected = active,
                        onClick = { onSelectSection(sec.startSec) },
                        label = { Text(sec.label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = KoraGold.copy(alpha = 0.25f),
                            selectedLabelColor = KoraGold
                        )
                    )
                }
            }

            // Time-stretch slider 50% - 150% without pitch change
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Vitesse (tempo)", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "${(playback.tempoMultiplier * 100).toInt()}%",
                            style = MaterialTheme.typography.labelLarge,
                            color = KoraGold,
                            fontWeight = FontWeight.Bold
                        )
                        if (kotlin.math.abs(playback.tempoMultiplier - 1.0f) > 0.02f) {
                            Surface(
                                onClick = { onTempoChange(1.0f) },
                                color = StudioSurfaceHigh,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "1×",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }
                }
                Slider(
                    value = playback.tempoMultiplier,
                    onValueChange = onTempoChange,
                    valueRange = 0.50f..1.50f,
                    colors = SliderDefaults.colors(thumbColor = KoraGold, activeTrackColor = KoraGold),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(32.dp)
                        .testTag("tempo_slider")
                )
            }

            HorizontalDivider(color = StudioBorder)

            // Row 1: Transposition
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Transposition", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        onClick = { onTranspose(playback.transposeSemitones - 1) },
                        color = StudioSurfaceHigh,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .size(width = 44.dp, height = 38.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Rounded.Remove, contentDescription = "Baisser d'un demi-ton", modifier = Modifier.size(18.dp))
                        }
                    }
                    Text(
                        text = "${if (playback.transposeSemitones > 0) "+" else ""}${playback.transposeSemitones} st",
                        style = MaterialTheme.typography.labelLarge,
                        color = LaguneTeal,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp)
                    )
                    Surface(
                        onClick = { onTranspose(playback.transposeSemitones + 1) },
                        color = StudioSurfaceHigh,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .size(width = 44.dp, height = 38.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Rounded.Add, contentDescription = "Monter d'un demi-ton", modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }

            // Row 2: Bar-locked A-B Loop
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Boucle A–B", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onLoopA,
                        contentPadding = PaddingValues(horizontal = 14.dp),
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("loop_a_btn")
                    ) {
                        Text(if (playback.loopStartMs != null) "Point A ✓" else "Point A")
                    }
                    OutlinedButton(
                        onClick = onLoopB,
                        contentPadding = PaddingValues(horizontal = 14.dp),
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("loop_b_btn")
                    ) {
                        Text(if (playback.loopEndMs != null) "Point B ✓" else "Point B")
                    }
                    if (playback.loopStartMs != null || playback.loopEndMs != null) {
                        IconButton(onClick = onClearLoop) {
                            Icon(Icons.Rounded.Close, contentDescription = "Effacer la boucle", tint = TerracottaPrimary)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TransportControlsBar(
    playback: MultiStemAudioEngine.PlaybackState,
    shuffleEnabled: Boolean,
    repeatEnabled: Boolean,
    onSeek: (Long) -> Unit,
    onTogglePlay: () -> Unit,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onToggleShuffle: () -> Unit,
    onToggleRepeat: () -> Unit
) {
    val dur = playback.durationMs.coerceAtLeast(1L)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(StudioSurface)
    ) {
        HorizontalDivider(color = StudioBorder)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp)
        ) {
            Slider(
                value = playback.positionMs.toFloat().coerceIn(0f, dur.toFloat()),
                onValueChange = { onSeek(it.toLong()) },
                valueRange = 0f..dur.toFloat(),
                colors = SliderDefaults.colors(
                    thumbColor = TerracottaPrimary,
                    activeTrackColor = TerracottaPrimary,
                    inactiveTrackColor = StudioBorder
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(28.dp)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(formatTimeMs(playback.positionMs), style = MaterialTheme.typography.labelSmall, color = TextMuted)
                Text(formatTimeMs(dur), style = MaterialTheme.typography.labelSmall, color = TextMuted)
            }
            Spacer(Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onToggleShuffle) {
                    Icon(
                        Icons.Rounded.Shuffle,
                        contentDescription = "Aléatoire",
                        tint = if (shuffleEnabled) TerracottaPrimary else TextMuted,
                        modifier = Modifier.size(22.dp)
                    )
                }
                IconButton(onClick = onPrev) {
                    Icon(Icons.Rounded.SkipPrevious, contentDescription = "Précédent", modifier = Modifier.size(30.dp))
                }
                Surface(
                    onClick = onTogglePlay,
                    color = TerracottaPrimary,
                    shape = CircleShape,
                    modifier = Modifier
                        .size(54.dp)
                        .testTag("player_play_pause_btn")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (playback.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            contentDescription = if (playback.isPlaying) "Pause" else "Lecture",
                            tint = Color.White,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                }
                IconButton(onClick = onNext) {
                    Icon(Icons.Rounded.SkipNext, contentDescription = "Suivant", modifier = Modifier.size(30.dp))
                }
                IconButton(onClick = onToggleRepeat) {
                    Icon(
                        Icons.Rounded.Repeat,
                        contentDescription = "Répéter",
                        tint = if (repeatEnabled) TerracottaPrimary else TextMuted,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}

private fun formatTimeMs(ms: Long): String {
    val totalSec = (ms / 1000L).coerceAtLeast(0L)
    val m = totalSec / 60
    val s = totalSec % 60
    return "$m:${s.toString().padStart(2, '0')}"
}

private val CHROMATIC_NOTES = listOf("C", "C#", "D", "Eb", "E", "F", "F#", "G", "Ab", "A", "Bb", "B")

private fun transposeChordLabel(label: String, semitones: Int): String {
    if (semitones == 0) return label
    val root = when {
        label.startsWith("C#") || label.startsWith("Db") -> 1
        label.startsWith("D#") || label.startsWith("Eb") -> 3
        label.startsWith("F#") || label.startsWith("Gb") -> 6
        label.startsWith("G#") || label.startsWith("Ab") -> 8
        label.startsWith("A#") || label.startsWith("Bb") -> 10
        label.startsWith("C") -> 0
        label.startsWith("D") -> 2
        label.startsWith("E") -> 4
        label.startsWith("F") -> 5
        label.startsWith("G") -> 7
        label.startsWith("A") -> 9
        label.startsWith("B") -> 11
        else -> return label
    }
    val suffix = if (label.length >= 2 && (label[1] == '#' || label[1] == 'b')) label.substring(2) else label.substring(1)
    val newIdx = ((root + semitones) % 12 + 12) % 12
    return CHROMATIC_NOTES[newIdx] + suffix
}
