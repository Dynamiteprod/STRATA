package com.example.strata

import com.example.strata.data.StrataCatalog
import com.example.strata.domain.AccountingDirection
import com.example.strata.domain.ForkSpec
import com.example.strata.domain.StemType
import com.example.strata.domain.TimelineSlice
import com.example.strata.engine.ClockSyncProtocol
import com.example.strata.engine.LedgerEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LedgerEngineTest {

    @Test
    fun `standard play revenue balances debits and credits exactly in minor units`() {
        val track = StrataCatalog.tracks.first()
        val entries = LedgerEngine.recordPlayRevenue(
            LedgerEngine.PlayRevenueRequest(
                eventId = "evt_t1",
                netValueMinorXof = 777L, // prime number to verify remainder handling
                baseTrack = track
            )
        )

        assertTrue("Double-entry ledger must balance to the exact centime", LedgerEngine.isBalanced(entries))
        val totalCredits = entries.filter { it.direction == AccountingDirection.CREDIT }.sumOf { it.amountMinorXof }
        assertEquals(777L, totalCredits)
    }

    @Test
    fun `fork with added track pays 70 percent to original and 30 percent to fork author`() {
        val track = StrataCatalog.tracks.first()
        val fork = StrataCatalog.sampleForks.first() // hasOriginalAddedTrack = true
        val entries = LedgerEngine.recordPlayRevenue(
            LedgerEngine.PlayRevenueRequest(
                eventId = "evt_fork_1",
                netValueMinorXof = 1000L,
                baseTrack = track,
                forkChain = listOf(fork)
            )
        )

        assertTrue(LedgerEngine.isBalanced(entries))
        val authorCredit = entries.first { it.accountId == "acc_${fork.authorUserId}" }.amountMinorXof
        assertEquals(300L, authorCredit)
        val originalCredits = entries
            .filter { it.direction == AccountingDirection.CREDIT && it.accountId != "acc_${fork.authorUserId}" }
            .sumOf { it.amountMinorXof }
        assertEquals(700L, originalCredits)
    }

    @Test
    fun `fork without added track pays 15 percent to fork author and cascades max 3 levels`() {
        val track = StrataCatalog.tracks.first()
        val f1 = ForkSpec("frk_a", track.id, authorUserId = "usr_a", authorName = "A", title = "F1", timeline = listOf(TimelineSlice(0, 16, 0), TimelineSlice(16, 32, 0)), stemGainsDb = emptyMap(), hasOriginalAddedTrack = false, tempoBpm = 124.0)
        val f2 = f1.copy(forkId = "frk_b", authorUserId = "usr_b", hasOriginalAddedTrack = true)
        val f3 = f1.copy(forkId = "frk_c", authorUserId = "usr_c", hasOriginalAddedTrack = true)
        val f4 = f1.copy(forkId = "frk_d", authorUserId = "usr_d", hasOriginalAddedTrack = true)

        val entries = LedgerEngine.recordPlayRevenue(
            LedgerEngine.PlayRevenueRequest(
                eventId = "evt_cascade",
                netValueMinorXof = 10000L,
                baseTrack = track,
                forkChain = listOf(f1, f2, f3, f4) // 4th level must be capped!
            )
        )

        assertTrue(LedgerEngine.isBalanced(entries))
        assertFalse("4th cascade level should be excluded by 3-level cap", entries.any { it.accountId == "acc_usr_d" })
        assertEquals(1500L, entries.first { it.accountId == "acc_usr_a" }.amountMinorXof)
    }

    @Test
    fun `empty fork triggers fraud flag and reversing entries cancel cleanly`() {
        val track = StrataCatalog.tracks.first()
        val emptyFork = ForkSpec("frk_empty", track.id, authorUserId = "usr_x", authorName = "X", title = "Empty", timeline = listOf(TimelineSlice(0, 64, 0)), stemGainsDb = mapOf(StemType.BASS to 0f), hasOriginalAddedTrack = false, tempoBpm = 124.0)
        val fraud = LedgerEngine.detectForkFraud(emptyFork, 1)
        assertTrue(fraud.isFlagged)

        val orig = LedgerEngine.recordPlayRevenue(LedgerEngine.PlayRevenueRequest("evt_ok", 500L, track))
        val rev = LedgerEngine.reverseEntries(orig, "evt_rev", "annulation")
        assertTrue(LedgerEngine.isBalanced(orig + rev))
        val netAccountSum = (orig + rev).sumOf {
            if (it.direction == AccountingDirection.CREDIT) it.amountMinorXof else -it.amountMinorXof
        }
        assertEquals(0L, netAccountSum)
    }

    @Test
    fun `clock sync protocol rejects high RTT outliers and detects chirp arrival within 1 ms`() {
        val samples = (0 until 20).map { i ->
            val rtt = if (i % 5 == 0) 45.0 else 4.0 // 4ms clean samples, 45ms spikes
            ClockSyncProtocol.ClockPingSample(1000.0, 1010.0, 1010.0, 1000.0 + rtt)
        }
        val stats = ClockSyncProtocol.computeClockOffset(samples)
        assertTrue("RTT outlier rejection should yield best RTT <= 4.1 ms", stats.bestRttMs <= 4.1)

        val chirp = ClockSyncProtocol.generateCalibrationChirp()
        val rec = ShortArray(4410)
        val expectedStart = 900
        val injectedDelaySamples = 88 // ~2.0 ms at 44.1 kHz
        chirp.copyInto(rec, destinationOffset = expectedStart + injectedDelaySamples)
        val measuredMs = ClockSyncProtocol.detectChirpArrivalMs(rec, chirp, expectedStart)
        val expectedMs = (injectedDelaySamples * 1000.0) / 44100.0
        assertEquals(expectedMs, measuredMs, 0.2)
    }
}
