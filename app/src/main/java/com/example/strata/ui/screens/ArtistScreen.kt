package com.example.strata.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.CloudUpload
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.strata.domain.AccountingDirection
import com.example.strata.domain.PayoutMethod
import com.example.strata.domain.StemType
import com.example.strata.engine.LedgerEngine
import com.example.strata.ui.StrataViewModel
import com.example.ui.theme.KoraGold
import com.example.ui.theme.LaguneTeal
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioSurface
import com.example.ui.theme.StudioSurfaceHigh
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.TerracottaPrimary
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSecondary

@Composable
fun ArtistScreen(
    uiState: StrataViewModel.UiState,
    viewModel: StrataViewModel,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var showUploadDialog by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Espace Artiste & Droits", style = MaterialTheme.typography.headlineMedium)
                    Text("BURIDA / SACEM • Grand livre XOF • Licences", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                }
                Button(
                    onClick = { showUploadDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = TerracottaPrimary),
                    modifier = Modifier.testTag("artist_upload_btn")
                ) {
                    Icon(Icons.Rounded.CloudUpload, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Déposer")
                }
            }
        }

        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = StudioSurface,
            contentColor = TerracottaPrimary
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Licences") },
                modifier = Modifier.testTag("artist_tab_licenses")
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Stats Stems") },
                modifier = Modifier.testTag("artist_tab_stats")
            )
            Tab(
                selected = selectedTab == 2,
                onClick = { selectedTab = 2 },
                text = { Text("Grand Livre") },
                modifier = Modifier.testTag("artist_tab_ledger")
            )
        }

        when (selectedTab) {
            0 -> ArtistLicensesList(uiState, viewModel)
            1 -> ArtistAnalyticsView(uiState)
            2 -> ArtistLedgerAndPayoutView(uiState, viewModel)
        }
    }

    if (showUploadDialog) {
        ArtistUploadTrackDialog(
            onSubmit = { title, artist, bpm, key, isrc, hasStems ->
                viewModel.ingestNewArtistTrack(title, artist, bpm, key, isrc, hasStems)
                showUploadDialog = false
            },
            onDismiss = { showUploadDialog = false }
        )
    }
}

@Composable
private fun ArtistLicensesList(uiState: StrataViewModel.UiState, viewModel: StrataViewModel) {
    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 112.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(uiState.catalog.take(4), key = { it.id }) { track ->
            Card(
                colors = CardDefaults.cardColors(containerColor = StudioSurface),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, StudioBorder, RoundedCornerShape(18.dp))
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(track.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        "ISRC : ${track.isrc} • ISWC : ${track.iswc}",
                        style = MaterialTheme.typography.labelSmall,
                        color = KoraGold
                    )
                    LicenseToggleRow("Stems visibles (crans 3–4)", track.license.allowStems) { v ->
                        viewModel.updateTrackLicense(track.id) { it.copy(allowStems = v) }
                    }
                    LicenseToggleRow("Autoriser les Forks (70% / 30%)", track.license.allowFork) { v ->
                        viewModel.updateTrackLicense(track.id) { it.copy(allowFork = v) }
                    }
                    LicenseToggleRow("Export de boucles WAV (Pro)", track.license.allowLoopExport) { v ->
                        viewModel.updateTrackLicense(track.id) { it.copy(allowLoopExport = v) }
                    }
                    LicenseToggleRow("Sessions Orchestre de poche", track.license.allowOrchestra) { v ->
                        viewModel.updateTrackLicense(track.id) { it.copy(allowOrchestra = v) }
                    }
                }
            }
        }
    }
}

@Composable
private fun LicenseToggleRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedThumbColor = ObsidianBg, checkedTrackColor = LaguneTeal)
        )
    }
}

@Composable
private fun ArtistAnalyticsView(uiState: StrataViewModel.UiState) {
    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 112.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        uiState.artistAnalytics.forEach { (trackId, stats) ->
            val track = uiState.catalog.firstOrNull { it.id == trackId } ?: return@forEach
            item(key = trackId) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = StudioSurface),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, StudioBorder, RoundedCornerShape(18.dp))
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(track.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            MetaTagPill("${stats.totalPlays} écoutes", KoraGold)
                            MetaTagPill("${stats.activeForksCount} forks", TerracottaPrimary)
                            MetaTagPill("${stats.orchestraSessionsCount} orchestres", LaguneTeal)
                        }
                        Text(
                            "Passage le plus bouclé : ${stats.topLoopedSection}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                        Text("Pistes les plus isolées :", style = MaterialTheme.typography.labelMedium, color = TextMuted)
                        val maxCount = (stats.stemIsolationCounts.values.maxOrNull() ?: 1).coerceAtLeast(1)
                        StemType.entries.forEach { st ->
                            val count = stats.stemIsolationCounts[st] ?: 0
                            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(st.labelFr, style = MaterialTheme.typography.labelSmall)
                                    Text("$count isolations", style = MaterialTheme.typography.labelSmall, color = stemColor(st))
                                }
                                LinearProgressIndicator(
                                    progress = { count.toFloat() / maxCount.toFloat() },
                                    color = stemColor(st),
                                    trackColor = StudioBorder,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ArtistLedgerAndPayoutView(uiState: StrataViewModel.UiState, viewModel: StrataViewModel) {
    val balanced = remember(uiState.ledgerEntries) { LedgerEngine.isBalanced(uiState.ledgerEntries) }
    val totalCreditedMinor = remember(uiState.ledgerEntries) {
        uiState.ledgerEntries.filter { it.direction == AccountingDirection.CREDIT }.sumOf { it.amountMinorXof }
    }

    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 112.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = StudioSurfaceHigh),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, KoraGold.copy(alpha = 0.5f), RoundedCornerShape(18.dp))
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Solde cumulé (double entrée)", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                            Text(
                                text = LedgerEngine.formatXofFromMinor(totalCreditedMinor),
                                style = MaterialTheme.typography.headlineMedium,
                                color = KoraGold
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Rounded.VerifiedUser, contentDescription = null, tint = if (balanced) SuccessGreen else TerracottaPrimary)
                            Text(
                                text = if (balanced) "Équilibré ✓" else "Écart",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (balanced) SuccessGreen else TerracottaPrimary
                            )
                        }
                    }

                    Text("Paiement Mobile Money (${uiState.payoutPhoneMasked}) :", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                        listOf(
                            PayoutMethod.ORANGE_MONEY,
                            PayoutMethod.MTN_MOMO,
                            PayoutMethod.MOOV_MONEY,
                            PayoutMethod.WAVE
                        ).forEach { method ->
                            FilterChip(
                                selected = uiState.selectedPayoutMethod == method,
                                onClick = { viewModel.selectPayoutMethod(method) },
                                label = { Text(method.label.substringBefore(" "), maxLines = 1) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = KoraGold.copy(alpha = 0.24f),
                                    selectedLabelColor = KoraGold
                                ),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    Button(
                        onClick = { viewModel.triggerArtistMobileMoneyPayout() },
                        colors = ButtonDefaults.buttonColors(containerColor = KoraGold, contentColor = ObsidianBg),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("payout_mobile_money_btn")
                    ) {
                        Icon(Icons.Rounded.AccountBalanceWallet, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Verser via ${uiState.selectedPayoutMethod.label}")
                    }
                }
            }
        }

        item {
            Text(
                "Écritures immuables (${uiState.ledgerEntries.size})",
                style = MaterialTheme.typography.titleMedium
            )
        }

        items(uiState.ledgerEntries, key = { it.id }) { entry ->
            val isCredit = entry.direction == AccountingDirection.CREDIT
            Surface(
                color = StudioSurface,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, StudioBorder, RoundedCornerShape(12.dp))
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(entry.beneficiaryName, style = MaterialTheme.typography.labelLarge)
                        Text(
                            "${entry.roleLabel} • ${entry.description}",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                        Text(
                            "Règle ${entry.ruleVersion} • ${entry.eventId}",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted
                        )
                    }
                    Text(
                        text = "${if (isCredit) "+" else "-"}${LedgerEngine.formatXofFromMinor(entry.amountMinorXof)}",
                        style = MaterialTheme.typography.labelLarge,
                        color = if (isCredit) SuccessGreen else TerracottaPrimary
                    )
                }
            }
        }
    }
}

@Composable
private fun ArtistUploadTrackDialog(
    onSubmit: (title: String, artist: String, bpm: Double, key: String, isrc: String, hasStems: Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    var title by remember { mutableStateOf("") }
    var artist by remember { mutableStateOf("Konaté Studio") }
    var bpmText by remember { mutableStateOf("118") }
    var keyText by remember { mutableStateOf("Am") }
    var isrcText by remember { mutableStateOf("CIABJ2600991") }
    var hasOfficialStems by remember { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = StudioSurface,
        title = { Text("Déposer un morceau (Ingestion)") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Pipeline : Normalisation EBU R128 (-14 LUFS), empreinte, grille de temps, tonalité, accords, ISRC.",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Titre du morceau") }, singleLine = true)
                OutlinedTextField(value = artist, onValueChange = { artist = it }, label = { Text("Artiste") }, singleLine = true)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = bpmText,
                        onValueChange = { bpmText = it.filter { c -> c.isDigit() } },
                        label = { Text("BPM") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = keyText,
                        onValueChange = { keyText = it },
                        label = { Text("Tonalité") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                OutlinedTextField(value = isrcText, onValueChange = { isrcText = it }, label = { Text("Code ISRC (BURIDA)") }, singleLine = true)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = hasOfficialStems, onCheckedChange = { hasOfficialStems = it })
                    Text("Stems officiels 24 bits fournis (sinon séparation IA)", style = MaterialTheme.typography.bodyMedium)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSubmit(title, artist, bpmText.toDoubleOrNull() ?: 118.0, keyText, isrcText, hasOfficialStems)
                },
                modifier = Modifier.testTag("confirm_upload_track_btn")
            ) {
                Text("Lancer l'ingestion")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuler") } }
    )
}
