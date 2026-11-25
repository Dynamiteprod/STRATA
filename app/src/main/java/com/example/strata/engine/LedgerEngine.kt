package com.example.strata.engine

import com.example.strata.domain.AccountingDirection
import com.example.strata.domain.ForkSpec
import com.example.strata.domain.LedgerEntry
import com.example.strata.domain.RightsSplit
import com.example.strata.domain.Track
import kotlin.math.abs

/**
 * Double-entry revenue distribution engine for STRATA plays & forks.
 * Implements Section 8.2 & 8.3:
 * - Strictly integer amounts in minor units (centimes de FCFA / XOF), NEVER floating point.
 * - Immutable debit/credit entries balanced per event.
 * - Default Fork split: 70% original rightsholders (split by basis points totaling 10000 bp)
 *   and 30% fork author (or 15% if no original track added; remaining 15% stays with original).
 * - Cascading forks-of-forks: each level forwards 70% upward, capped at 3 levels.
 */
object LedgerEngine {

    const val RULE_VERSION_2026_V1 = "2026.11.v1"
    const val PLATFORM_POOL_ACCOUNT = "acc_platform_royalty_pool"

    data class PlayRevenueRequest(
        val eventId: String,
        val netValueMinorXof: Long, // e.g. 500 = 5.00 FCFA
        val baseTrack: Track,
        val forkChain: List<ForkSpec> = emptyList(), // ordered from immediate played fork -> parent fork (max 3)
        val timestampMs: Long = System.currentTimeMillis()
    )

    data class FraudCheckResult(
        val isFlagged: Boolean,
        val reason: String? = null
    )

    /**
     * Generates balanced double-entry ledger lines for a play or fork play event.
     * Guarantees sum(debits) == sum(credits) == netValueMinorXof with zero remainder loss.
     */
    fun recordPlayRevenue(request: PlayRevenueRequest): List<LedgerEntry> {
        val total = request.netValueMinorXof
        if (total <= 0L) return emptyList()

        val entries = mutableListOf<LedgerEntry>()
        var seq = 1

        // 1. Debit platform royalty pool
        entries += LedgerEntry(
            id = "ldg_${request.eventId}_00",
            eventId = request.eventId,
            accountId = PLATFORM_POOL_ACCOUNT,
            beneficiaryName = "Pool Redevances STRATA",
            roleLabel = "plateforme_pool",
            amountMinorXof = total,
            direction = AccountingDirection.DEBIT,
            ruleVersion = RULE_VERSION_2026_V1,
            description = if (request.forkChain.isEmpty()) {
                "Écoute : ${request.baseTrack.title}"
            } else {
                "Écoute Fork : ${request.forkChain.first().title}"
            },
            timestampMs = request.timestampMs
        )

        val cappedChain = request.forkChain.take(3)
        if (cappedChain.isEmpty()) {
            // Standard track play: 100% of net royalty distributed by track splits
            entries += distributeBySplits(
                eventId = request.eventId,
                amountMinor = total,
                splits = request.baseTrack.splits,
                labelPrefix = "Droits originaux",
                timestampMs = request.timestampMs,
                startSeq = seq
            )
            return entries
        }

        // 2. Cascading fork split (bottom-up from played fork toward original work)
        var upstreamPool = total
        val forkCredits = mutableListOf<Pair<ForkSpec, Long>>()

        for (fork in cappedChain) {
            val authorShareBp = when {
                fork.hasOriginalAddedTrack -> (10_000 - request.baseTrack.license.forkSplitBp).coerceIn(1000, 5000)
                else -> 1500 // 15% if no original stem/audio added (Section 8.2)
            }
            val authorAmount = (upstreamPool * authorShareBp) / 10_000L
            val forwardedUpward = upstreamPool - authorAmount
            forkCredits += fork to authorAmount
            upstreamPool = forwardedUpward
        }

        // Distribute remaining upstreamPool to original rightsholders
        val originalEntries = distributeBySplits(
            eventId = request.eventId,
            amountMinor = upstreamPool,
            splits = request.baseTrack.splits,
            labelPrefix = "Ayants droit d'origine",
            timestampMs = request.timestampMs,
            startSeq = seq
        )
        entries += originalEntries
        seq += originalEntries.size

        // Credit each fork author in chain
        for ((index, pair) in forkCredits.withIndex()) {
            val (fork, amount) = pair
            if (amount > 0L) {
                entries += LedgerEntry(
                    id = "ldg_${request.eventId}_${seq.toString().padStart(2, '0')}",
                    eventId = request.eventId,
                    accountId = "acc_${fork.authorUserId}",
                    beneficiaryName = fork.authorName,
                    roleLabel = if (index == 0) "Auteur Fork" else "Auteur Fork parent (niv. ${index + 1})",
                    amountMinorXof = amount,
                    direction = AccountingDirection.CREDIT,
                    ruleVersion = RULE_VERSION_2026_V1,
                    description = "Fork '${fork.title}'",
                    timestampMs = request.timestampMs
                )
                seq++
            }
        }

        return entries
    }

    private fun distributeBySplits(
        eventId: String,
        amountMinor: Long,
        splits: List<RightsSplit>,
        labelPrefix: String,
        timestampMs: Long,
        startSeq: Int
    ): List<LedgerEntry> {
        if (splits.isEmpty() || amountMinor <= 0L) return emptyList()
        val totalBp = splits.sumOf { it.shareBp }.coerceAtLeast(1)
        val result = mutableListOf<LedgerEntry>()
        var allocated = 0L

        splits.forEachIndexed { idx, split ->
            val share = if (idx == splits.lastIndex) {
                // Assign integer remainder to final beneficiary so double-entry balances to the exact centime
                amountMinor - allocated
            } else {
                (amountMinor * split.shareBp.toLong()) / totalBp.toLong()
            }
            allocated += share
            if (share > 0L) {
                result += LedgerEntry(
                    id = "ldg_${eventId}_${(startSeq + idx).toString().padStart(2, '0')}",
                    eventId = eventId,
                    accountId = "acc_${split.partyId}",
                    beneficiaryName = split.partyName,
                    roleLabel = "${split.role} (${split.cmoAffiliation ?: "Indép."})",
                    amountMinorXof = share,
                    direction = AccountingDirection.CREDIT,
                    ruleVersion = RULE_VERSION_2026_V1,
                    description = "$labelPrefix — ${split.shareBp / 100.0}%",
                    timestampMs = timestampMs
                )
            }
        }
        return result
    }

    /**
     * Create immutable reversing entries for corrections (Section 8.3).
     */
    fun reverseEntries(originalEntries: List<LedgerEntry>, reversalEventId: String, reason: String): List<LedgerEntry> {
        val now = System.currentTimeMillis()
        return originalEntries.mapIndexed { idx, entry ->
            entry.copy(
                id = "ldg_rev_${reversalEventId}_${idx.toString().padStart(2, '0')}",
                eventId = reversalEventId,
                direction = if (entry.direction == AccountingDirection.DEBIT) {
                    AccountingDirection.CREDIT
                } else {
                    AccountingDirection.DEBIT
                },
                description = "Contre-passation ($reason) : ${entry.description}",
                timestampMs = now
            )
        }
    }

    /**
     * Verify double-entry invariant: total debits == total credits.
     */
    fun isBalanced(entries: List<LedgerEntry>): Boolean {
        val debits = entries.filter { it.direction == AccountingDirection.DEBIT }.sumOf { it.amountMinorXof }
        val credits = entries.filter { it.direction == AccountingDirection.CREDIT }.sumOf { it.amountMinorXof }
        return debits == credits
    }

    /**
     * Fraud detection (Section 8.3): empty forks (no rearrangement, no gain change, no added track)
     * or excessive loop plays in short window.
     */
    fun detectForkFraud(fork: ForkSpec, recentPlayCountSameUser10Min: Int): FraudCheckResult {
        val hasMixChange = fork.stemGainsDb.values.any { abs(it) > 0.5f }
        val hasTimelineEdit = fork.timeline.size > 1
        if (!fork.hasOriginalAddedTrack && !hasMixChange && !hasTimelineEdit) {
            return FraudCheckResult(true, "Fork vide détecté (empreinte identique à l'original).")
        }
        if (recentPlayCountSameUser10Min > 25) {
            return FraudCheckResult(true, "Écoutes en boucle suspectes (> 25 en 10 min).")
        }
        return FraudCheckResult(false)
    }

    fun formatXofFromMinor(amountMinorXof: Long): String {
        val whole = amountMinorXof / 100
        val cents = abs(amountMinorXof % 100)
        return if (cents == 0L) "$whole FCFA" else "$whole,${cents.toString().padStart(2, '0')} FCFA"
    }
}
