package com.example.strata.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SpeakerGroup
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material.icons.rounded.WifiTethering
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.strata.domain.OrchestraMode
import com.example.strata.domain.OrchestraParticipant
import com.example.strata.domain.StemType
import com.example.strata.domain.Track
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
fun OrchestraScreen(
    uiState: StrataViewModel.UiState,
    activeTrack: Track?,
    viewModel: StrataViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var joinCodeInput by remember { mutableStateOf("") }
    var guestNameInput by remember { mutableStateOf("") }

    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        viewModel.runAcousticCalibration(hasMicPermission = granted)
    }

    val session = uiState.orchestraSession

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 112.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("L'Orchestre de poche", style = MaterialTheme.typography.headlineMedium)
                Text(
                    "2 à 8 téléphones dans la même pièce jouent chacun une piste synchronisée (< 10 ms).",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }
        }

        if (session == null) {
            item {
                val trackToHost = activeTrack ?: uiState.catalog.first()
                Card(
                    colors = CardDefaults.cardColors(containerColor = StudioSurface),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, StudioBorder, RoundedCornerShape(20.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .background(TerracottaPrimary.copy(alpha = 0.18f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Rounded.SpeakerGroup, contentDescription = null, tint = TerracottaPrimary)
                            }
                            Column {
                                Text("Héberger une session", style = MaterialTheme.typography.titleLarge)
                                Text(
                                    "Morceau : ${trackToHost.title}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = KoraGold
                                )
                            }
                        }
                        Text(
                            "Les invités rejoignent même sans abonnement via Hotspot ou Wi-Fi local. Les stems sont téléchargés avant lecture : seuls les ordres et horodatages circulent.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                        Button(
                            onClick = { viewModel.createOrchestraSession(trackToHost) },
                            colors = ButtonDefaults.buttonColors(containerColor = TerracottaPrimary),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("orchestra_create_btn")
                        ) {
                            Icon(Icons.Rounded.WifiTethering, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Créer une session d'Orchestre")
                        }
                    }
                }
            }

            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = StudioSurface),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, StudioBorder, RoundedCornerShape(20.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("Rejoindre par code à 4 chiffres", style = MaterialTheme.typography.titleMedium)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedTextField(
                                value = joinCodeInput,
                                onValueChange = { if (it.length <= 4) joinCodeInput = it.filter { c -> c.isDigit() } },
                                label = { Text("Code (ex. 4821)") },
                                singleLine = true,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("orchestra_code_input")
                            )
                            Button(
                                onClick = { viewModel.joinOrchestraSessionByCode(joinCodeInput.ifBlank { "4821" }) },
                                modifier = Modifier
                                    .height(52.dp)
                                    .testTag("orchestra_join_btn")
                            ) {
                                Text("Rejoindre")
                            }
                        }
                    }
                }
            }
        } else {
            // Active Session Card: QR Code + 4-digit Code + Calibration Status
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = StudioSurface),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, TerracottaPrimary.copy(alpha = 0.55f), RoundedCornerShape(20.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                DeterministicQrMatrix(code = session.sessionCode, modifier = Modifier.size(64.dp))
                                Column {
                                    Text(
                                        text = "Session #${session.sessionCode}",
                                        style = MaterialTheme.typography.headlineSmall,
                                        color = KoraGold
                                    )
                                    Text(
                                        text = "${session.participants.size}/8 téléphones connectés",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = TextSecondary
                                    )
                                }
                            }
                            if (session.isCalibrated) {
                                Surface(
                                    color = SuccessGreen.copy(alpha = 0.16f),
                                    shape = RoundedCornerShape(50)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(Icons.Rounded.Verified, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(14.dp))
                                        Text(
                                            text = "${String.format("%.1f", session.maxInterDeviceErrorMs)} ms",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = SuccessGreen
                                        )
                                    }
                                }
                            }
                        }

                        // Calibration progress or button
                        if (uiState.isCalibrating) {
                            LinearProgressIndicator(
                                color = LaguneTeal,
                                trackColor = StudioBorder,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Text(
                                text = uiState.calibrationProgressMessage ?: "Calibration en cours…",
                                style = MaterialTheme.typography.labelMedium,
                                color = LaguneTeal
                            )
                        } else {
                            OutlinedButton(
                                onClick = {
                                    val granted = ContextCompat.checkSelfPermission(
                                        context,
                                        Manifest.permission.RECORD_AUDIO
                                    ) == PackageManager.PERMISSION_GRANTED
                                    if (granted) {
                                        viewModel.runAcousticCalibration(hasMicPermission = true)
                                    } else {
                                        micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("orchestra_calibrate_btn")
                            ) {
                                Icon(Icons.Rounded.Mic, contentDescription = null, tint = LaguneTeal)
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    if (session.isCalibrated) {
                                        "Recalibrer acoustiquement (horloge + chirp + dérive)"
                                    } else {
                                        "Calibrer au micro (NTP + Chirp < 10 ms)"
                                    }
                                )
                            }
                            Text(
                                text = "Confidentialité : le micro n'est utilisé que pendant le chirp de calibration ; aucun enregistrement conservé.",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextMuted
                            )
                        }

                        // Mode selector: Ensemble / Jeu / Répétition
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            OrchestraMode.entries.forEach { mode ->
                                FilterChip(
                                    selected = session.mode == mode,
                                    onClick = { viewModel.setOrchestraMode(mode) },
                                    label = {
                                        Text(
                                            when (mode) {
                                                OrchestraMode.ENSEMBLE -> "Ensemble"
                                                OrchestraMode.GAME_GUESS -> "Mode Jeu"
                                                OrchestraMode.REHEARSAL -> "Répétition"
                                            }
                                        )
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = TerracottaPrimary.copy(alpha = 0.24f),
                                        selectedLabelColor = TerracottaPrimary
                                    ),
                                    modifier = Modifier.testTag("orchestra_mode_${mode.name}")
                                )
                            }
                        }

                        Button(
                            onClick = { viewModel.startOrchestraSynchronizedPlayback() },
                            colors = ButtonDefaults.buttonColors(containerColor = TerracottaPrimary),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("orchestra_play_btn")
                        ) {
                            Icon(Icons.Rounded.PlayArrow, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Lancer la lecture synchronisée")
                        }
                    }
                }
            }

            // Participants & Stem Attribution
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Répartition spatiale des pistes", style = MaterialTheme.typography.titleMedium)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedTextField(
                            value = guestNameInput,
                            onValueChange = { guestNameInput = it },
                            placeholder = { Text("Nom invité") },
                            singleLine = true,
                            modifier = Modifier.width(120.dp)
                        )
                        Surface(
                            onClick = {
                                viewModel.addParticipantToOrchestra(guestNameInput)
                                guestNameInput = ""
                            },
                            color = StudioSurfaceHigh,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("add_participant_btn")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Rounded.Add, contentDescription = "Ajouter un téléphone")
                            }
                        }
                    }
                }
            }

            items(session.participants, key = { it.id }) { participant ->
                ParticipantCard(
                    participant = participant,
                    isCalibrated = session.isCalibrated,
                    onToggleStem = { stem -> viewModel.toggleParticipantStem(participant.id, stem) }
                )
            }
        }
    }
}

@Composable
private fun ParticipantCard(
    participant: OrchestraParticipant,
    isCalibrated: Boolean,
    onToggleStem: (StemType) -> Unit
) {
    Surface(
        color = StudioSurface,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, StudioBorder, RoundedCornerShape(16.dp))
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(
                        imageVector = if (participant.isHost) Icons.Rounded.GraphicEq else Icons.Rounded.Groups,
                        contentDescription = null,
                        tint = if (participant.isHost) TerracottaPrimary else KoraGold
                    )
                    Column {
                        Text(participant.name, style = MaterialTheme.typography.titleMedium)
                        Text(
                            "${participant.deviceModel} • Latence brute ${participant.reportedLatencyMs} ms",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted
                        )
                    }
                }
                if (isCalibrated) {
                    MetaTagPill("Δ ${String.format("%.1f", participant.calibratedOffsetMs)} ms", SuccessGreen)
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                StemType.entries.forEach { stem ->
                    val assigned = participant.assignedStems.contains(stem)
                    val color = stemColor(stem)
                    FilterChip(
                        selected = assigned,
                        onClick = { onToggleStem(stem) },
                        label = { Text(stem.labelFr, maxLines = 1) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = color.copy(alpha = 0.25f),
                            selectedLabelColor = color
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun DeterministicQrMatrix(code: String, modifier: Modifier = Modifier) {
    val seed = code.toIntOrNull() ?: 4821
    Canvas(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Color.White)
            .padding(5.dp)
    ) {
        val grid = 7
        val cell = size.width / grid
        for (r in 0 until grid) {
            for (c in 0 until grid) {
                val isCorner = (r < 2 && c < 2) || (r < 2 && c >= grid - 2) || (r >= grid - 2 && c < 2)
                val bit = ((seed * (r + 3) + c * 17) % 5) < 2
                if (isCorner || bit) {
                    drawRect(
                        color = ObsidianBg,
                        topLeft = Offset(c * cell, r * cell),
                        size = Size(cell * 0.9f, cell * 0.9f)
                    )
                }
            }
        }
    }
}
