package com.example.strata.ui.screens

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.strata.domain.NetworkContext
import com.example.strata.domain.StreamingQuality
import com.example.strata.domain.SubscriptionPlan
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
fun AccountScreen(
    uiState: StrataViewModel.UiState,
    viewModel: StrataViewModel,
    modifier: Modifier = Modifier
) {
    var pendingMobileMoneyPlan by remember { mutableStateOf<SubscriptionPlan?>(null) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 112.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column {
                Text("Compte, Offres & Data", style = MaterialTheme.typography.headlineMedium)
                Text(
                    "Forfaits adaptés Côte d'Ivoire (Mobile Money) • Mode Éco par défaut",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }
        }

        // Subscription / Persona Switcher
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = StudioSurface),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, StudioBorder, RoundedCornerShape(18.dp))
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Offre active : ${uiState.currentPlan.titleFr}", style = MaterialTheme.typography.titleMedium, color = KoraGold)
                    Text(
                        "Accès jusqu'au cran ${uiState.currentPlan.maxDepthAllowed} • Hors-ligne : ${if (uiState.currentPlan.offlineEnabled) "Oui" else "Non"}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                    SubscriptionPlan.entries.forEach { plan ->
                        val selected = uiState.currentPlan == plan
                        Surface(
                            onClick = {
                                if (plan == SubscriptionPlan.FREE) {
                                    viewModel.setPlan(plan)
                                } else {
                                    pendingMobileMoneyPlan = plan
                                }
                            },
                            color = if (selected) TerracottaPrimary.copy(alpha = 0.2f) else StudioSurfaceHigh,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(
                                    1.dp,
                                    if (selected) TerracottaPrimary else StudioBorder,
                                    RoundedCornerShape(12.dp)
                                )
                                .testTag("plan_option_${plan.id}")
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(plan.titleFr, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                    Text(
                                        "Crans 0–${plan.maxDepthAllowed} • ${if (plan.canHostOrchestra) "Hôte Orchestre" else "Invité Orchestre"}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextSecondary
                                    )
                                }
                                MetaTagPill(plan.priceFcfaLabel, if (selected) TerracottaPrimary else KoraGold)
                            }
                        }
                    }
                }
            }
        }

        // Data Saver & Streaming Quality
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = StudioSurface),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, StudioBorder, RoundedCornerShape(18.dp))
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Qualité & Économie de Data", style = MaterialTheme.typography.titleMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = uiState.networkContext == NetworkContext.CELLULAR,
                            onClick = { viewModel.setNetworkContext(NetworkContext.CELLULAR) },
                            label = { Text("Réseau Mobile (Éco)") },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = KoraGold.copy(alpha = 0.22f))
                        )
                        FilterChip(
                            selected = uiState.networkContext == NetworkContext.WIFI,
                            onClick = { viewModel.setNetworkContext(NetworkContext.WIFI) },
                            label = { Text("Wi-Fi") },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = LaguneTeal.copy(alpha = 0.22f))
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                        StreamingQuality.entries.forEach { q ->
                            FilterChip(
                                selected = uiState.streamingQuality == q,
                                onClick = { viewModel.setStreamingQuality(q) },
                                label = { Text("${q.label} ${q.kbps}k") },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    Text(
                        "Le cran choisi est mémorisé séparément pour le réseau mobile et le Wi-Fi. Les stems ne se téléchargent jamais par défaut.",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted
                    )
                }
            }
        }

        // Accessibility & Language
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = StudioSurface),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, StudioBorder, RoundedCornerShape(18.dp))
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Accessibilité & Langues", style = MaterialTheme.typography.titleMedium)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Retour haptique sur les temps forts", style = MaterialTheme.typography.bodyMedium)
                        Switch(
                            checked = uiState.hapticOnDownbeat,
                            onCheckedChange = { viewModel.toggleHaptics() },
                            colors = SwitchDefaults.colors(checkedTrackColor = TerracottaPrimary)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Langue (${uiState.language.label})", style = MaterialTheme.typography.bodyMedium)
                        TextButton(onClick = { viewModel.toggleLanguage() }) {
                            Text("Basculer FR / EN")
                        }
                    }
                }
            }
        }
    }

    pendingMobileMoneyPlan?.let { plan ->
        AlertDialog(
            onDismissRequest = { pendingMobileMoneyPlan = null },
            containerColor = StudioSurface,
            title = { Text("Paiement Mobile Money") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Souscrire à l'offre ${plan.titleFr} (${plan.priceFcfaLabel}) via l'agrégateur local :")
                    Text("• Orange Money / MTN MoMo / Moov Money / Wave", style = MaterialTheme.typography.bodyMedium, color = KoraGold)
                    Text("• Aucune reconduction cachée pour le Pass Jour / Semaine.", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.setPlan(plan)
                        pendingMobileMoneyPlan = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = KoraGold, contentColor = ObsidianBg),
                    modifier = Modifier.testTag("confirm_mobile_money_plan_btn")
                ) {
                    Text("Confirmer (${plan.priceFcfaLabel})")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingMobileMoneyPlan = null }) { Text("Annuler") }
            }
        )
    }
}
