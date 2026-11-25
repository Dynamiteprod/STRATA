package com.example.strata.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.CallSplit
import androidx.compose.material.icons.rounded.AccountTree
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.DataSaverOn
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.strata.data.StrataCatalog
import com.example.strata.domain.ForkSpec
import com.example.strata.domain.GenreStyle
import com.example.strata.domain.NetworkContext
import com.example.strata.domain.Track
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
fun DiscoverScreen(
    uiState: StrataViewModel.UiState,
    filteredTracks: List<Track>,
    currentPlayingTrackId: String?,
    onSearchChange: (String) -> Unit,
    onGenreSelect: (GenreStyle?) -> Unit,
    onToggleOfflineFilter: () -> Unit,
    onToggleNetworkContext: () -> Unit,
    onPlayTrack: (Track) -> Unit,
    onToggleOfflineTrack: (String) -> Unit,
    onPlayFork: (ForkSpec, Track) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 112.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. Hero Studio Banner + Network Data-Saver Pill
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .height(188.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .border(1.dp, StudioBorder, RoundedCornerShape(22.dp))
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_hero_strata),
                    contentDescription = "Session studio STRATA à Abidjan",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    ObsidianBg.copy(alpha = 0.15f),
                                    ObsidianBg.copy(alpha = 0.88f)
                                )
                            )
                        )
                )
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = TerracottaPrimary.copy(alpha = 0.92f),
                            shape = RoundedCornerShape(50)
                        ) {
                            Text(
                                text = "STRATA • ABIDJAN",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }

                        // Network context toggle chip (Mobile Éco vs Wi-Fi)
                        val isCellular = uiState.networkContext == NetworkContext.CELLULAR
                        Surface(
                            onClick = onToggleNetworkContext,
                            color = StudioSurface.copy(alpha = 0.88f),
                            shape = RoundedCornerShape(50),
                            modifier = Modifier.testTag("network_context_chip")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Icon(
                                    imageVector = if (isCellular) Icons.Rounded.DataSaverOn else Icons.Rounded.Wifi,
                                    contentDescription = "Contexte réseau",
                                    tint = if (isCellular) KoraGold else LaguneTeal,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = if (isCellular) {
                                        "Mobile • ${uiState.streamingQuality.label} ${uiState.streamingQuality.kbps} kb/s"
                                    } else {
                                        "Wi-Fi • ${uiState.streamingQuality.label} ${uiState.streamingQuality.kbps} kb/s"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    Column {
                        Text(
                            text = "Chaque chanson s'ouvre comme une session de studio",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Curseur 5 crans • Généalogie synchro • Orchestre • Forks XOF",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                }
            }
        }

        // 2. Search by Title, Artist, Lyrics, BPM, Key
        item {
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = onSearchChange,
                placeholder = {
                    Text(
                        "Chercher titre, artiste, paroles, BPM (124) ou tonalité (Am)…",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                leadingIcon = {
                    Icon(Icons.Rounded.Search, contentDescription = "Rechercher")
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = StudioSurface,
                    unfocusedContainerColor = StudioSurface,
                    focusedBorderColor = TerracottaPrimary,
                    unfocusedBorderColor = StudioBorder
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .testTag("search_input")
            )
        }

        // 3. Genre & Offline Filter Chips
        item {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        selected = uiState.onlyOfflineFilter,
                        onClick = onToggleOfflineFilter,
                        label = { Text("Hors-ligne") },
                        leadingIcon = {
                            Icon(
                                Icons.Rounded.CloudDownload,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = LaguneTeal.copy(alpha = 0.22f),
                            selectedLabelColor = LaguneTeal
                        ),
                        modifier = Modifier.testTag("filter_offline")
                    )
                }
                items(GenreStyle.entries) { genre ->
                    FilterChip(
                        selected = uiState.selectedGenre == genre,
                        onClick = { onGenreSelect(genre) },
                        label = { Text(genre.labelFr) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = TerracottaPrimary.copy(alpha = 0.24f),
                            selectedLabelColor = TerracottaPrimary
                        ),
                        modifier = Modifier.testTag("filter_genre_${genre.name}")
                    )
                }
            }
        }

        // 4. Forks Rémunérés Shelf
        if (uiState.forks.isNotEmpty() && uiState.searchQuery.isBlank()) {
            item {
                Column(modifier = Modifier.padding(top = 4.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Forks ouverts (${uiState.forks.size})",
                            style = MaterialTheme.typography.titleMedium,
                            color = KoraGold
                        )
                        Text(
                            text = "70% ayants droit • 30% créateur",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(uiState.forks, key = { it.forkId }) { fork ->
                            val baseTrack = uiState.catalog.firstOrNull { it.id == fork.baseTrackId }
                            if (baseTrack != null) {
                                Surface(
                                    onClick = { onPlayFork(fork, baseTrack) },
                                    color = StudioSurfaceHigh,
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier
                                        .width(248.dp)
                                        .border(1.dp, KoraGold.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
                                        .testTag("fork_card_${fork.forkId}")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(44.dp)
                                                .clip(CircleShape)
                                                .background(KoraGold.copy(alpha = 0.16f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                Icons.AutoMirrored.Rounded.CallSplit,
                                                contentDescription = "Jouer le fork",
                                                tint = KoraGold
                                            )
                                        }
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = fork.title,
                                                style = MaterialTheme.typography.labelLarge,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = "Par ${fork.authorName} • ${fork.tempoBpm.toInt()} BPM",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = TextSecondary,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 5. Catalog Section Title
        item {
            Text(
                text = "Catalogue Ouest-Africain Ouvert (${filteredTracks.size})",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
        }

        if (filteredTracks.isEmpty()) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Rounded.GraphicEq,
                        contentDescription = null,
                        tint = TextMuted,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Aucun morceau ne correspond à ta recherche.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
            }
        } else {
            items(filteredTracks, key = { it.id }) { track ->
                TrackCatalogCard(
                    track = track,
                    isPlayingThis = track.id == currentPlayingTrackId,
                    lineageCount = StrataCatalog.linksForTrack(track.id).size,
                    onPlay = { onPlayTrack(track) },
                    onToggleOffline = { onToggleOfflineTrack(track.id) }
                )
            }
        }
    }
}

@Composable
private fun TrackCatalogCard(
    track: Track,
    isPlayingThis: Boolean,
    lineageCount: Int,
    onPlay: () -> Unit,
    onToggleOffline: () -> Unit
) {
    val borderColor = if (isPlayingThis) TerracottaPrimary else StudioBorder
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isPlayingThis) StudioSurfaceHigh else StudioSurface
        ),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .border(1.dp, borderColor, RoundedCornerShape(18.dp))
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onPlay)
            .testTag("track_card_${track.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(12.dp))
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
                        .background(Color.Black.copy(alpha = if (isPlayingThis) 0.45f else 0.18f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isPlayingThis) Icons.Rounded.GraphicEq else Icons.Rounded.PlayArrow,
                        contentDescription = "Lire ${track.title}",
                        tint = if (isPlayingThis) TerracottaPrimary else Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = track.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${track.artistName} • ${track.genre.labelFr}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(6.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    MetaTagPill("${track.analysis.tempo.bpm.toInt()} BPM", KoraGold)
                    MetaTagPill(track.analysis.key.display(), LaguneTeal)
                    if (lineageCount > 0) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Icon(
                                Icons.Rounded.AccountTree,
                                contentDescription = "Lignée musicale",
                                tint = TerracottaPrimary,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = "$lineageCount",
                                style = MaterialTheme.typography.labelSmall,
                                color = TerracottaPrimary
                            )
                        }
                    }
                }
            }

            IconButton(
                onClick = onToggleOffline,
                modifier = Modifier.testTag("offline_btn_${track.id}")
            ) {
                Icon(
                    imageVector = if (track.isDownloadedOffline) Icons.Rounded.CheckCircle else Icons.Rounded.CloudDownload,
                    contentDescription = if (track.isDownloadedOffline) "Disponible hors-ligne" else "Télécharger hors-ligne",
                    tint = if (track.isDownloadedOffline) LaguneTeal else TextMuted
                )
            }
        }
    }
}

@Composable
fun MetaTagPill(text: String, accentColor: Color) {
    Surface(
        color = accentColor.copy(alpha = 0.14f),
        shape = RoundedCornerShape(6.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = accentColor,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}
