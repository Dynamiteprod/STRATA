package com.example.strata.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.CallSplit
import androidx.compose.material.icons.rounded.AccountTree
import androidx.compose.material.icons.rounded.AutoGraph
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Lyrics
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.strata.domain.DepthLevel
import com.example.strata.domain.StemMixState
import com.example.strata.domain.StemType
import com.example.strata.domain.Track
import com.example.strata.domain.VersionLink
import com.example.strata.engine.MultiStemAudioEngine
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.KoraGold
import com.example.ui.theme.LaguneTeal
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.StemBassColor
import com.example.ui.theme.StemDrumsColor
import com.example.ui.theme.StemOtherColor
import com.example.ui.theme.StemVocalsColor
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioSurface
import com.example.ui.theme.StudioSurfaceBright
import com.example.ui.theme.StudioSurfaceHigh
import com.example.ui.theme.TerracottaPrimary
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSecondary

fun stemColor(type: StemType): Color = when (type) {
    StemType.VOCALS -> StemVocalsColor
    StemType.DRUMS -> StemDrumsColor
    StemType.BASS -> StemBassColor
    StemType.OTHER -> StemOtherColor
}

/**
 * One-handed Vertical Depth Slider pinned on the right thumb-zone (Section 3.2 & 18.2).
 */
@Composable
fun VerticalDepthRail(
    currentDepth: DepthLevel,
    maxAllowedDepth: Int,
    pendingDownbeat: Boolean,
    onSelectDepth: (DepthLevel) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = StudioSurface.copy(alpha = 0.94f),
        shape = RoundedCornerShape(24.dp),
        modifier = modifier
            .border(1.dp, StudioBorder, RoundedCornerShape(24.dp))
            .padding(vertical = 8.dp, horizontal = 4.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Display top-down 4 -> 0 so Studio is up and Écoute is down
            DepthLevel.entries.sortedByDescending { it.level }.forEach { depth ->
                val isSelected = depth == currentDepth
                val isLocked = depth.level > maxAllowedDepth
                val icon = when (depth) {
                    DepthLevel.ECOUTE -> Icons.Rounded.Headphones
                    DepthLevel.PAROLES -> Icons.Rounded.Lyrics
                    DepthLevel.MUSICIEN -> Icons.Rounded.MusicNote
                    DepthLevel.PISTES -> Icons.Rounded.Tune
                    DepthLevel.STUDIO -> Icons.Rounded.AutoGraph
                }
                val bg = when {
                    isSelected -> TerracottaPrimary
                    isLocked -> StudioSurfaceHigh.copy(alpha = 0.5f)
                    else -> StudioSurfaceHigh
                }
                Box(
                    modifier = Modifier
                        .size(width = 52.dp, height = 52.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(bg)
                        .clickable { onSelectDepth(depth) }
                        .testTag("depth_slider_cran_${depth.level}"),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = if (isLocked) Icons.Rounded.Lock else icon,
                            contentDescription = "Cran ${depth.level} ${depth.labelFr}",
                            tint = when {
                                isSelected -> Color.White
                                isLocked -> TextMuted
                                else -> TextSecondary
                            },
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "${depth.level} ${depth.labelFr.take(5)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isSelected) Color.White else TextMuted,
                            maxLines = 1
                        )
                    }
                }
            }
            if (pendingDownbeat) {
                Text(
                    text = "1er tps…",
                    style = MaterialTheme.typography.labelSmall,
                    color = KoraGold
                )
            }
        }
    }
}

@Composable
fun StemChannelStrip(
    mixState: StemMixState,
    stemSourceLabel: String,
    isSelectedForStudio: Boolean,
    onSelectForStudio: () -> Unit,
    onVolumeChange: (Float) -> Unit,
    onToggleMute: () -> Unit,
    onToggleSolo: () -> Unit
) {
    val accent = stemColor(mixState.type)
    Surface(
        color = if (isSelectedForStudio) StudioSurfaceBright else StudioSurfaceHigh,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = if (isSelectedForStudio) accent else StudioBorder,
                shape = RoundedCornerShape(14.dp)
            )
            .clickable(onClick = onSelectForStudio)
            .testTag("stem_strip_${mixState.type.id}")
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(accent)
                    )
                    Text(
                        text = mixState.type.labelFr,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = stemSourceLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    // 48dp minimum touch targets for M & S
                    Surface(
                        onClick = onToggleMute,
                        color = if (mixState.muted) ErrorRed else StudioSurface,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("stem_mute_${mixState.type.id}")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                "M",
                                style = MaterialTheme.typography.labelLarge,
                                color = if (mixState.muted) ObsidianBg else TextSecondary
                            )
                        }
                    }
                    Surface(
                        onClick = onToggleSolo,
                        color = if (mixState.solo) KoraGold else StudioSurface,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("stem_solo_${mixState.type.id}")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                "S",
                                style = MaterialTheme.typography.labelLarge,
                                color = if (mixState.solo) ObsidianBg else TextSecondary
                            )
                        }
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Slider(
                    value = mixState.volume,
                    onValueChange = onVolumeChange,
                    valueRange = 0f..1.4f,
                    colors = SliderDefaults.colors(
                        thumbColor = accent,
                        activeTrackColor = accent,
                        inactiveTrackColor = StudioBorder
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("stem_slider_${mixState.type.id}")
                )
                Text(
                    text = "${(mixState.volume * 100).toInt()}%",
                    style = MaterialTheme.typography.labelMedium,
                    color = accent,
                    modifier = Modifier.width(44.dp)
                )
            }
        }
    }
}

@Composable
fun StudioAnalysisPanel(
    track: Track,
    selectedStem: StemMixState,
    playback: MultiStemAudioEngine.PlaybackState,
    onPanChange: (Float) -> Unit,
    onEqChange: (low: Float, mid: Float, high: Float) -> Unit,
    onExportLoop: () -> Unit,
    onOpenForkDialog: () -> Unit
) {
    val accent = stemColor(selectedStem.type)
    val stemPeaks = track.analysis.stems
        .firstOrNull { it.type == selectedStem.type }?.waveformPeaks ?: emptyList()
    val progress = (playback.positionMs.toFloat() / playback.durationMs.coerceAtLeast(1L).toFloat()).coerceIn(0f, 1f)

    Surface(
        color = StudioSurface,
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, accent.copy(alpha = 0.55f), RoundedCornerShape(18.dp))
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
                Text(
                    text = "Studio • ${selectedStem.type.labelFr}",
                    style = MaterialTheme.typography.titleMedium,
                    color = accent
                )
                Text(
                    text = "Peak ${playback.spectrum.peakDbfs.toInt()} dBFS • LUFS ${track.analysis.loudnessLufs}",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
            }

            // Multi-peak Waveform Canvas + Live Playhead
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(ObsidianBg)
                    .padding(horizontal = 4.dp, vertical = 6.dp)
            ) {
                val count = stemPeaks.size.coerceAtLeast(1)
                val barW = (size.width / count) * 0.68f
                val gap = (size.width / count)
                val midY = size.height / 2f
                stemPeaks.forEachIndexed { idx, amp ->
                    val x = idx * gap
                    val h = (amp * size.height * 0.88f).coerceAtLeast(4f)
                    val passed = (idx.toFloat() / count) <= progress
                    drawRoundRect(
                        color = if (passed) accent else StudioBorder,
                        topLeft = Offset(x, midY - h / 2f),
                        size = Size(barW, h),
                        cornerRadius = CornerRadius(3f, 3f)
                    )
                }
                val playX = progress * size.width
                drawLine(
                    color = Color.White,
                    start = Offset(playX, 0f),
                    end = Offset(playX, size.height),
                    strokeWidth = 3f
                )
            }

            // 12-band Live Spectrum Analyzer
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(34.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(ObsidianBg)
                    .padding(4.dp)
            ) {
                val bands = playback.spectrum.bands
                val bw = (size.width / bands.size.coerceAtLeast(1)) * 0.72f
                val step = size.width / bands.size.coerceAtLeast(1)
                bands.forEachIndexed { idx, energy ->
                    val bh = (energy * size.height).coerceAtLeast(3f)
                    drawRoundRect(
                        color = if (idx < 4) LaguneTeal else if (idx < 8) KoraGold else TerracottaPrimary,
                        topLeft = Offset(idx * step, size.height - bh),
                        size = Size(bw, bh),
                        cornerRadius = CornerRadius(4f, 4f)
                    )
                }
            }

            // Pan + 3-Band EQ Sliders
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Pan", style = MaterialTheme.typography.labelSmall, modifier = Modifier.width(54.dp))
                Slider(
                    value = selectedStem.pan,
                    onValueChange = onPanChange,
                    valueRange = -1f..1f,
                    colors = SliderDefaults.colors(thumbColor = accent, activeTrackColor = accent),
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = when {
                        selectedStem.pan < -0.1f -> "L ${(-selectedStem.pan * 100).toInt()}"
                        selectedStem.pan > 0.1f -> "R ${(selectedStem.pan * 100).toInt()}"
                        else -> "Centre"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.width(52.dp)
                )
            }

            EqBandRow("Graves", selectedStem.eqLowDb, accent) {
                onEqChange(it, selectedStem.eqMidDb, selectedStem.eqHighDb)
            }
            EqBandRow("Médiums", selectedStem.eqMidDb, accent) {
                onEqChange(selectedStem.eqLowDb, it, selectedStem.eqHighDb)
            }
            EqBandRow("Aigus", selectedStem.eqHighDb, accent) {
                onEqChange(selectedStem.eqLowDb, selectedStem.eqMidDb, it)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onExportLoop,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("export_loop_btn")
                ) {
                    Text("Exporter boucle", maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Button(
                    onClick = onOpenForkDialog,
                    colors = ButtonDefaults.buttonColors(containerColor = KoraGold, contentColor = ObsidianBg),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("create_fork_btn")
                ) {
                    Icon(Icons.AutoMirrored.Rounded.CallSplit, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Créer un Fork", maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

@Composable
private fun EqBandRow(label: String, db: Float, accent: Color, onChange: (Float) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = TextSecondary, modifier = Modifier.width(54.dp))
        Slider(
            value = db,
            onValueChange = onChange,
            valueRange = -12f..12f,
            colors = SliderDefaults.colors(thumbColor = accent, activeTrackColor = accent),
            modifier = Modifier.weight(1f)
        )
        Text(
            text = "${if (db >= 0) "+" else ""}${db.toInt()} dB",
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.width(52.dp)
        )
    }
}

@Composable
fun GenealogyGraphDialog(
    currentTrack: Track,
    links: List<VersionLink>,
    catalog: List<Track>,
    onTransitionTo: (Track, VersionLink) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = StudioSurface,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Rounded.AccountTree, contentDescription = null, tint = TerracottaPrimary)
                Text("Généalogie jouable", style = MaterialTheme.typography.titleLarge)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Basculer vers une version liée au point musicalement équivalent, calé sur le 1er temps suivant :",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
                if (links.isEmpty()) {
                    Text(
                        "Aucune version liée enregistrée pour « ${currentTrack.title} ».",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextMuted
                    )
                } else {
                    links.forEach { link ->
                        val targetId = if (link.sourceTrackId == currentTrack.id) link.targetTrackId else link.sourceTrackId
                        val target = catalog.firstOrNull { it.id == targetId }
                        if (target != null) {
                            Surface(
                                onClick = { onTransitionTo(target, link) },
                                color = StudioSurfaceHigh,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, TerracottaPrimary.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                                    .testTag("lineage_target_${target.id}")
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        MetaTagPill(link.linkType.labelFr, TerracottaPrimary)
                                        Text(
                                            text = "Confiance ${(link.confidence * 100).toInt()}%",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = LaguneTeal
                                        )
                                    }
                                    Spacer(Modifier.height(6.dp))
                                    Text(target.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    Text(
                                        "${target.artistName} • ${target.analysis.tempo.bpm.toInt()} BPM • ${target.analysis.key.display()}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = TextSecondary
                                    )
                                    Text(
                                        "Provenance : ${link.provenance.labelFr}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextMuted
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Fermer")
            }
        }
    )
}

@Composable
fun ForkCreatorDialog(
    baseTrack: Track,
    onPublish: (title: String, reverseSections: Boolean, hasOriginalAddedTrack: Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    var title by remember { mutableStateOf("${baseTrack.title} — Fork Studio") }
    var reverseSections by remember { mutableStateOf(true) }
    var hasOriginalTrack by remember { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = StudioSurface,
        title = { Text("Créer un Fork rémunéré") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Un fork est un projet léger rejoué à partir des stems originaux (aucune copie audio dupliquée).",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Titre du fork") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { reverseSections = !reverseSections }
                ) {
                    Checkbox(checked = reverseSections, onCheckedChange = { reverseSections = it })
                    Text("Réarranger la structure (Refrain en ouverture)", style = MaterialTheme.typography.bodyMedium)
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { hasOriginalTrack = !hasOriginalTrack }
                ) {
                    Checkbox(checked = hasOriginalTrack, onCheckedChange = { hasOriginalTrack = it })
                    Text("J'ajoute ma propre piste enregistrée (part 30% au lieu de 15%)", style = MaterialTheme.typography.bodyMedium)
                }
                Surface(
                    color = KoraGold.copy(alpha = 0.14f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (hasOriginalTrack) {
                            "Règle active : 70% ayants droit d'origine (BURIDA) • 30% auteur du fork"
                        } else {
                            "Règle active : 85% ayants droit d'origine (BURIDA) • 15% auteur du fork"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = KoraGold,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onPublish(title, reverseSections, hasOriginalTrack) },
                colors = ButtonDefaults.buttonColors(containerColor = KoraGold, contentColor = ObsidianBg),
                modifier = Modifier.testTag("confirm_publish_fork_btn")
            ) {
                Text("Publier le Fork")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annuler") }
        }
    )
}
