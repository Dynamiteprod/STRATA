package com.example.strata.domain

import androidx.annotation.DrawableRes

/**
 * Depth levels for the single-screen STRATA depth slider (Crans 0 to 4).
 * Source of truth: Cahier des charges Section 3.2.
 */
enum class DepthLevel(val level: Int, val labelFr: String, val labelEn: String, val subtitleFr: String) {
    ECOUTE(0, "Écoute", "Listen", "Mix stéréo"),
    PAROLES(1, "Paroles", "Lyrics", "Karaoké synchro"),
    MUSICIEN(2, "Musicien", "Musician", "Accords, tempo, boucle A-B"),
    PISTES(3, "Pistes", "Stems", "4 pistes séparées"),
    STUDIO(4, "Studio", "Studio", "Onde, EQ 3 bandes, spectre, fork")
}

enum class StreamingQuality(val label: String, val kbps: Int, val dataPerMinKb: Int) {
    ECO("Éco", 48, 360),
    NORMAL("Normal", 128, 960),
    HAUTE("Haute", 256, 1920)
}

enum class NetworkContext {
    CELLULAR,
    WIFI
}

enum class SubscriptionPlan(
    val id: String,
    val titleFr: String,
    val priceFcfaLabel: String,
    val maxDepthAllowed: Int,
    val offlineEnabled: Boolean,
    val canHostOrchestra: Boolean,
    val canExportLoop: Boolean,
    val canForkUnlimited: Boolean
) {
    FREE("free", "Gratuit", "0 FCFA", 2, false, false, false, false),
    PASS_DAY("pass_day", "Pass Jour", "100 FCFA", 4, true, true, true, true),
    PREMIUM("premium", "Premium", "2 000 FCFA / mois", 3, true, true, false, false),
    PRO("pro", "Pro", "5 000 FCFA / mois", 4, true, true, true, true),
    EDUCATION("education", "Éducation", "Tarif groupe", 4, true, true, true, true)
}

enum class StemType(val id: String, val labelFr: String, val labelEn: String) {
    VOCALS("vocals", "Voix", "Vocals"),
    DRUMS("drums", "Batterie", "Drums"),
    BASS("bass", "Basse", "Bass"),
    OTHER("other", "Autres", "Other")
}

enum class StemSource(val wire: String) {
    OFFICIAL("official"),
    AI("ai")
}

enum class GenreStyle(val labelFr: String) {
    COUPE_DECALE("Coupé-Décalé"),
    ZOUGLOU("Zouglou"),
    RAP_IVOIRE("Rap Ivoire"),
    AFROBEATS("Afrobeats"),
    GOSPEL("Gospel")
}

enum class VersionLinkType(val wire: String, val labelFr: String) {
    SAMPLE_OF("sample_of", "Sample d'origine"),
    COVER_OF("cover_of", "Reprise"),
    LIVE_OF("live_of", "Version Live"),
    DEMO_OF("demo_of", "Démo / Remaster"),
    FORK_OF("fork_of", "Fork STRATA"),
    INTERPOLATION_OF("interpolation_of", "Interpolation")
}

enum class LinkProvenance(val labelFr: String) {
    ARTIST("Déclaré par l'artiste"),
    COMMUNITY("Validé par la communauté"),
    STAFF("Vérifié STRATA"),
    AUTO("Détecté automatiquement")
}

data class TempoInfo(
    val bpm: Double,
    val confidence: Double,
    val isVariable: Boolean = false
)

data class BeatPoint(
    val tSec: Double,
    val beatInBar: Int
)

data class KeyInfo(
    val tonic: String,
    val mode: String, // "major" or "minor"
    val confidence: Double
) {
    fun display(): String = if (mode == "minor") "${tonic}m" else tonic
}

data class ChordEvent(
    val startSec: Double,
    val endSec: Double,
    val label: String,
    val confidence: Double
)

data class SectionSegment(
    val startSec: Double,
    val endSec: Double,
    val label: String
)

data class LyricWord(
    val startSec: Double,
    val endSec: Double,
    val text: String
)

data class LyricLine(
    val startSec: Double,
    val endSec: Double,
    val text: String,
    val words: List<LyricWord> = emptyList()
)

data class StemInfo(
    val type: StemType,
    val source: StemSource,
    val model: String,
    val waveformPeaks: List<Float>
)

data class HumanReviewed(
    val chords: Boolean,
    val sections: Boolean,
    val lyrics: Boolean
)

/**
 * Matches analysis.json schema in Section 5.2.
 */
data class TrackAnalysis(
    val trackId: String,
    val analysisVersion: String = "1.3.0",
    val durationMs: Long,
    val sampleRate: Int = 44100,
    val loudnessLufs: Double = -14.0,
    val tempo: TempoInfo,
    val beats: List<BeatPoint>,
    val timeSignature: String = "4/4",
    val key: KeyInfo,
    val chords: List<ChordEvent>,
    val sections: List<SectionSegment>,
    val lyrics: List<LyricLine>,
    val stems: List<StemInfo>,
    val humanReviewed: HumanReviewed
) {
    val meanChordConfidence: Double
        get() = if (chords.isEmpty()) 0.0 else chords.map { it.confidence }.average()

    val hasVerifiedChords: Boolean
        get() = humanReviewed.chords || meanChordConfidence >= 0.68
}

data class LicenseConfig(
    val allowStream: Boolean = true,
    val allowStems: Boolean = true,
    val allowLoopExport: Boolean = true,
    val allowFork: Boolean = true,
    val allowOrchestra: Boolean = true,
    val forkSplitBp: Int = 7000, // 70% to original rightsholders
    val territories: List<String> = listOf("CI", "SN", "CM", "BF", "BJ", "TG", "ML", "GH", "NG")
)

data class RightsSplit(
    val partyId: String,
    val partyName: String,
    val role: String, // author, composer, publisher, producer, performer
    val shareBp: Int, // basis points, total = 10000
    val cmoAffiliation: String? = "BURIDA"
)

data class Track(
    val id: String,
    val workId: String,
    val title: String,
    val artistId: String,
    val artistName: String,
    val releaseTitle: String,
    val genre: GenreStyle,
    val isrc: String,
    val iswc: String,
    val durationMs: Long,
    @DrawableRes val coverResId: Int,
    val analysis: TrackAnalysis,
    val license: LicenseConfig,
    val splits: List<RightsSplit>,
    val rootPitchMidi: Int,
    val isDownloadedOffline: Boolean = false,
    val isFork: Boolean = false,
    val forkAuthorUserId: String? = null,
    val localUriString: String? = null
)

data class AlignmentPoint(
    val sourceSec: Double,
    val targetSec: Double
)

data class VersionLink(
    val id: String,
    val sourceTrackId: String,
    val targetTrackId: String,
    val linkType: VersionLinkType,
    val provenance: LinkProvenance,
    val confidence: Double,
    val coverage: Double,
    val mapping: List<AlignmentPoint>
)

data class StemMixState(
    val type: StemType,
    val volume: Float = 1.0f, // 0f..1.5f
    val muted: Boolean = false,
    val solo: Boolean = false,
    val pan: Float = 0f, // -1f (L) .. +1f (R)
    val eqLowDb: Float = 0f, // -12..+12
    val eqMidDb: Float = 0f,
    val eqHighDb: Float = 0f
)

data class TimelineSlice(
    val fromBeat: Int,
    val toBeat: Int,
    val sourceBeat: Int
)

data class ForkSpec(
    val forkId: String,
    val baseTrackId: String,
    val parentForkId: String? = null,
    val baseAnalysisVersion: String = "1.3.0",
    val authorUserId: String,
    val authorName: String,
    val title: String,
    val timeline: List<TimelineSlice>,
    val stemGainsDb: Map<StemType, Float>,
    val hasOriginalAddedTrack: Boolean,
    val tempoBpm: Double,
    val depthFromOriginal: Int = 1
)

enum class AccountingDirection { DEBIT, CREDIT }

enum class PayoutMethod(val id: String, val label: String) {
    ORANGE_MONEY("orange_money", "Orange Money"),
    MTN_MOMO("mtn_momo", "MTN MoMo"),
    MOOV_MONEY("moov", "Moov Money"),
    WAVE("wave", "Wave"),
    BANK("bank", "Virement bancaire")
}

data class LedgerEntry(
    val id: String,
    val eventId: String,
    val accountId: String,
    val beneficiaryName: String,
    val roleLabel: String,
    val amountMinorXof: Long, // centimes de FCFA (integer only, never floating point)
    val direction: AccountingDirection,
    val ruleVersion: String,
    val description: String,
    val timestampMs: Long
)

enum class OrchestraMode(val labelFr: String) {
    ENSEMBLE("Ensemble"),
    GAME_GUESS("Jeu (Devine l'instrument)"),
    REHEARSAL("Répétition (Joue en direct)")
}

data class OrchestraParticipant(
    val id: String,
    val name: String,
    val deviceModel: String,
    val isHost: Boolean,
    val assignedStems: Set<StemType>,
    val reportedLatencyMs: Int,
    val calibratedOffsetMs: Double,
    val clockDriftPpm: Double,
    val speakerGainDb: Float,
    val isConnected: Boolean = true
)

data class OrchestraSession(
    val sessionId: String,
    val sessionCode: String,
    val trackId: String,
    val hostName: String,
    val mode: OrchestraMode = OrchestraMode.ENSEMBLE,
    val isCalibrated: Boolean = false,
    val participants: List<OrchestraParticipant> = emptyList(),
    val maxInterDeviceErrorMs: Double = 0.0
)

data class ArtistAnalytics(
    val totalPlays: Int,
    val stemIsolationCounts: Map<StemType, Int>,
    val topLoopedSection: String,
    val activeForksCount: Int,
    val orchestraSessionsCount: Int
)

enum class AppLanguage(val code: String, val label: String) {
    FR("fr", "Français"),
    EN("en", "English")
}
