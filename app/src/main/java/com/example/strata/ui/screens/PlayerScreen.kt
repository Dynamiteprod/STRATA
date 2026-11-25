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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountTree
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Loop
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
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
        shape = RoundedCornerShape(18.dp),
        tonalElevation = 6.dp,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .border(1.dp, StudioBorder, RoundedCornerShape(18.dp))
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onExpand)
            .testTag("mini_player_bar")
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 8.dp),
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
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Cran ${currentDepth.level} • ${currentDepth.labelFr}",
                            style = MaterialTheme.typography.labelSmall,
                            color = TerracottaPrimary
                        )
                        Text(
                            text = "${(track.analysis.tempo.bpm * playback.tempoMultiplier).toInt()} BPM",
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
            .navigationBarsPadding()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top Studio Header: Collapse + Lineage Pill + Share
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
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
                        modifier = Modifier.size(30.dp)
                    )
                }

                // Playable Genealogy pill (also triggerable by vertical swipe on cover!)
                Surface(
                    onClick = { viewModel.setShowGenealogySheet(true) },
                    color = if (links.isNotEmpty()) TerracottaPrimary.copy(alpha = 0.2f) else StudioSurface,
                    shape = RoundedCornerShape(50),
                    modifier = Modifier
                        .border(
                            1.dp,
                            if (links.isNotEmpty()) TerracottaPrimary else StudioBorder,
                            RoundedCornerShape(50)
                        )
                        .testTag("lineage_pill_btn")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            Icons.Rounded.AccountTree,
                            contentDescription = "Généalogie jouable",
                            tint = if (links.isNotEmpty()) TerracottaPrimary else TextMuted,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = if (links.isNotEmpty()) "Lignée (${links.size}) • Glisser ↕" else "Lignée",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                IconButton(
                    onClick = { viewModel.shareTimestampLink() },
                    modifier = Modifier.testTag("share_timestamp_btn")
                ) {
                    Icon(Icons.Rounded.Share, contentDescription = "Partager ce moment")
                }
            }

            // Pending genealogy crossfade indicator
            AnimatedVisibility(visible = playback.pendingTransition != null) {
                playback.pendingTransition?.let { pending ->
                    Surface(
                        color = KoraGold.copy(alpha = 0.2f),
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

            // Main Content + Right-Edge One-Handed Depth Slider
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    contentPadding = PaddingValues(start = 16.dp, end = 8.dp, top = 4.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // CRAN 0: ÉCOUTE (Always visible; cover shrinks smoothly as depth unfolds!)
                    item {
                        val isCompactHeader = depth.level >= 2
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
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(210.dp)
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
                                        // Beat pulse badge
                                        Surface(
                                            color = ObsidianBg.copy(alpha = 0.78f),
                                            shape = RoundedCornerShape(50),
                                            modifier = Modifier
                                                .align(Alignment.BottomStart)
                                                .padding(10.dp)
                                        ) {
                                            Text(
                                                text = "Mesure ${playback.spectrum.currentBarNumber} • Temps ${playback.spectrum.activeBeatInBar}/4",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = KoraGold,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                            )
                                        }
                                    }
                                    TrackHeaderInfoRow(track, playback, uiState.streamingQuality, viewModel)
                                }
                            } else {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Image(
                                        painter = painterResource(id = track.coverResId),
                                        contentDescription = "Pochette ${track.title}",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .size(56.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = playback.activeFork?.title ?: track.title,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${track.artistName} • Mes. ${playback.spectrum.currentBarNumber}.${playback.spectrum.activeBeatInBar}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = TextSecondary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    IconButton(onClick = { viewModel.toggleTrackOffline(track.id) }) {
                                        Icon(
                                            imageVector = if (track.isDownloadedOffline) Icons.Rounded.CheckCircle else Icons.Rounded.CloudDownload,
                                            contentDescription = "Hors-ligne",
                                            tint = if (track.isDownloadedOffline) LaguneTeal else TextMuted
                                        )
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
                                    text = "Pistes séparées (${track.analysis.stems.size})",
                                    style = MaterialTheme.typography.titleMedium
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
                                    modifier = Modifier.testTag("karaoke_mode_btn")
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

                // Right-edge One-handed Depth Slider
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .padding(end = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    VerticalDepthRail(
                        currentDepth = depth,
                        maxAllowedDepth = uiState.currentPlan.maxDepthAllowed,
                        pendingDownbeat = playback.pendingMixToStemsOnDownbeat,
                        onSelectDepth = { viewModel.selectDepthLevel(it) }
                    )
                }
            }

            // Bottom Transport Controls (Always present across all 5 crans)
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
                style = MaterialTheme.typography.headlineSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${track.artistName} • ${track.genre.labelFr}",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                MetaTagPill("${quality.label} ${quality.kbps} kb/s", LaguneTeal)
                MetaTagPill(if (track.isDownloadedOffline) "Hors-ligne ✓" else "Streaming", KoraGold)
            }
        }
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
                    color = KoraGold
                )
                if (showChords && !track.analysis.hasVerifiedChords) {
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
                        .padding(horizontal = 8.dp, vertical = 6.dp)
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
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Outils Musicien", style = MaterialTheme.typography.titleMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    MetaTagPill("$effectiveBpm BPM", KoraGold)
                    MetaTagPill(transposeChordLabel(track.analysis.key.display(), playback.transposeSemitones), LaguneTeal)
                }
            }

            // Structure segments (Intro / Couplet / Refrain / Pont)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
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
            Column {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Vitesse (hauteur préservée)", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                    Text("${(playback.tempoMultiplier * 100).toInt()}%", style = MaterialTheme.typography.labelMedium, color = KoraGold)
                }
                Slider(
                    value = playback.tempoMultiplier,
                    onValueChange = onTempoChange,
                    valueRange = 0.50f..1.50f,
                    colors = SliderDefaults.colors(thumbColor = KoraGold, activeTrackColor = KoraGold),
                    modifier = Modifier.testTag("tempo_slider")
                )
            }

            // Transposition + Bar-locked A-B loop
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Ton", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                    IconButton(onClick = { onTranspose(playback.transposeSemitones - 1) }) {
                        Icon(Icons.Rounded.Remove, contentDescription = "Baisser d'un demi-ton")
                    }
                    Text(
                        text = "${if (playback.transposeSemitones > 0) "+" else ""}${playback.transposeSemitones}",
                        style = MaterialTheme.typography.labelLarge,
                        color = LaguneTeal
                    )
                    IconButton(onClick = { onTranspose(playback.transposeSemitones + 1) }) {
                        Icon(Icons.Rounded.Add, contentDescription = "Monter d'un demi-ton")
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(onClick = onLoopA, contentPadding = PaddingValues(horizontal = 10.dp), modifier = Modifier.testTag("loop_a_btn")) {
                        Text(if (playback.loopStartMs != null) "A ✓" else "A")
                    }
                    OutlinedButton(onClick = onLoopB, contentPadding = PaddingValues(horizontal = 10.dp), modifier = Modifier.testTag("loop_b_btn")) {
                        Text(if (playback.loopEndMs != null) "B ✓" else "B")
                    }
                    if (playback.loopStartMs != null || playback.loopEndMs != null) {
                        IconButton(onClick = onClearLoop) {
                            Icon(Icons.Rounded.Loop, contentDescription = "Couper la boucle", tint = TerracottaPrimary)
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
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Slider(
            value = playback.positionMs.toFloat().coerceIn(0f, dur.toFloat()),
            onValueChange = { onSeek(it.toLong()) },
            valueRange = 0f..dur.toFloat(),
            colors = SliderDefaults.colors(thumbColor = TerracottaPrimary, activeTrackColor = TerracottaPrimary)
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(formatTimeMs(playback.positionMs), style = MaterialTheme.typography.labelSmall, color = TextMuted)
            Text(formatTimeMs(dur), style = MaterialTheme.typography.labelSmall, color = TextMuted)
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onToggleShuffle) {
                Icon(
                    Icons.Rounded.Shuffle,
                    contentDescription = "Aléatoire",
                    tint = if (shuffleEnabled) TerracottaPrimary else TextMuted
                )
            }
            IconButton(onClick = onPrev) {
                Icon(Icons.Rounded.SkipPrevious, contentDescription = "Précédent", modifier = Modifier.size(32.dp))
            }
            Surface(
                onClick = onTogglePlay,
                color = TerracottaPrimary,
                shape = CircleShape,
                modifier = Modifier
                    .size(58.dp)
                    .testTag("player_play_pause_btn")
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (playback.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = if (playback.isPlaying) "Pause" else "Lecture",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
            IconButton(onClick = onNext) {
                Icon(Icons.Rounded.SkipNext, contentDescription = "Suivant", modifier = Modifier.size(32.dp))
            }
            IconButton(onClick = onToggleRepeat) {
                Icon(
                    Icons.Rounded.Repeat,
                    contentDescription = "Répéter",
                    tint = if (repeatEnabled) TerracottaPrimary else TextMuted
                )
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
