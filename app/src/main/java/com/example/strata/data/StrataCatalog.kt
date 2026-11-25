package com.example.strata.data

import com.example.R
import com.example.strata.domain.AlignmentPoint
import com.example.strata.domain.ArtistAnalytics
import com.example.strata.domain.BeatPoint
import com.example.strata.domain.ChordEvent
import com.example.strata.domain.ForkSpec
import com.example.strata.domain.GenreStyle
import com.example.strata.domain.HumanReviewed
import com.example.strata.domain.KeyInfo
import com.example.strata.domain.LicenseConfig
import com.example.strata.domain.LinkProvenance
import com.example.strata.domain.LyricLine
import com.example.strata.domain.LyricWord
import com.example.strata.domain.RightsSplit
import com.example.strata.domain.SectionSegment
import com.example.strata.domain.StemInfo
import com.example.strata.domain.StemSource
import com.example.strata.domain.StemType
import com.example.strata.domain.TempoInfo
import com.example.strata.domain.TimelineSlice
import com.example.strata.domain.Track
import com.example.strata.domain.TrackAnalysis
import com.example.strata.domain.VersionLink
import com.example.strata.domain.VersionLinkType
import kotlin.math.abs
import kotlin.math.sin

/**
 * Curated offline-first West African / Ivorian catalog (Coupé-Décalé, Zouglou, Rap Ivoire,
 * Afrobeats, Gospel) with complete beat grids, chord progressions, lyrics, stems,
 * rights splits (BURIDA), and playable genealogy links.
 */
object StrataCatalog {

    val tracks: List<Track> = listOf(
        // 1. Abidjan Minuit — Coupé-Décalé (Root of Lineage Family 1)
        buildTrack(
            id = "trk_01_abidjan_minuit",
            workId = "wrk_01_abidjan_minuit",
            title = "Abidjan Minuit",
            artistId = "art_koffi_dj",
            artistName = "DJ Arafel & Les Garçons",
            releaseTitle = "Nuit de Marcory EP",
            genre = GenreStyle.COUPE_DECALE,
            isrc = "CIABJ2600101",
            iswc = "T-901.224.810-1",
            bpm = 124.0,
            keyTonic = "A",
            keyMode = "minor",
            chordLoop = listOf("Am", "F", "C", "G"),
            durationSec = 76,
            coverResId = R.drawable.img_cover_coupe,
            rootMidi = 57,
            stemSource = StemSource.OFFICIAL,
            chordsVerified = true,
            chordConfidence = 0.92,
            lyricsLines = listOf(
                4.0 to "Marcory s'allume quand la basse descend",
                8.0 to "Lève les bras dans le maquis ce soir",
                16.0 to "On coupe le son pour entendre le tambour",
                24.0 to "Abidjan minuit, le rythme ne dort jamais",
                32.0 to "Refrain : Ouvre les pistes, fais vibrer la salle",
                40.0 to "Chaque téléphone devient un instrument",
                48.0 to "Du Plateau jusqu'à Yopougon, on cale le temps",
                56.0 to "Abidjan minuit, la lignée continue"
            ),
            splits = listOf(
                RightsSplit("pty_arafel", "DJ Arafel (Auteur/Prod.)", "author", 5000, "BURIDA"),
                RightsSplit("pty_yapi", "Kouassi Yapi (Compositeur)", "composer", 3000, "BURIDA"),
                RightsSplit("pty_marcory_rec", "Marcory Sound Lab", "producer", 2000, "BURIDA/SACEM")
            ),
            isDownloadedOffline = true
        ),
        // 2. Abidjan Minuit (Reprise Zouglou Acoustique) — cover_of
        buildTrack(
            id = "trk_02_abidjan_zouglou",
            workId = "wrk_01_abidjan_minuit",
            title = "Abidjan Minuit (Reprise Zouglou)",
            artistId = "art_yao_band",
            artistName = "Yao & Orchestre Cocody",
            releaseTitle = "Sessions Acoustiques",
            genre = GenreStyle.ZOUGLOU,
            isrc = "CIABJ2600102",
            iswc = "T-901.224.810-1",
            bpm = 98.0,
            keyTonic = "C",
            keyMode = "minor",
            chordLoop = listOf("Cm", "Ab", "Eb", "Bb"),
            durationSec = 76,
            coverResId = R.drawable.img_cover_zouglou,
            rootMidi = 60,
            stemSource = StemSource.OFFICIAL,
            chordsVerified = true,
            chordConfidence = 0.89,
            lyricsLines = listOf(
                4.0 to "À la guitare sèche sous le manguier",
                10.0 to "Marcory s'éveille doucement ce soir",
                20.0 to "On pose les accords sur le tam-tam parleur",
                30.0 to "Refrain : Ouvre les pistes, écoute l'harmonie",
                42.0 to "Même mélodie, autre tempo, même cœur",
                54.0 to "Cocody chante la reprise jusqu'à l'aube"
            ),
            splits = listOf(
                RightsSplit("pty_arafel", "DJ Arafel (Œuvre d'origine)", "author", 5000, "BURIDA"),
                RightsSplit("pty_yao", "Yao Konan (Interprète)", "performer", 5000, "BURIDA")
            )
        ),
        // 3. Abidjan Minuit (Live Maquis Yopougon) — live_of (variable tempo)
        buildTrack(
            id = "trk_03_abidjan_live",
            workId = "wrk_01_abidjan_minuit",
            title = "Abidjan Minuit (Live Yopougon)",
            artistId = "art_koffi_dj",
            artistName = "DJ Arafel — Live Rue Princesse",
            releaseTitle = "Live Maquis 2026",
            genre = GenreStyle.COUPE_DECALE,
            isrc = "CIABJ2600103",
            iswc = "T-901.224.810-1",
            bpm = 128.0,
            isVariableTempo = true,
            keyTonic = "A",
            keyMode = "minor",
            chordLoop = listOf("Am", "F", "C", "G"),
            durationSec = 72,
            coverResId = R.drawable.img_cover_coupe,
            rootMidi = 57,
            stemSource = StemSource.AI,
            chordsVerified = false,
            chordConfidence = 0.61, // Triggers "Accords non vérifiés" per CA 5.3!
            lyricsLines = listOf(
                3.0 to "(Foule en direct) Yopougon est prêt !",
                12.0 to "Faites monter le djembé sur le premier temps !",
                26.0 to "Refrain : Ouvre les pistes ce soir !",
                44.0 to "Solo de batterie en direct de la scène"
            ),
            splits = listOf(
                RightsSplit("pty_arafel", "DJ Arafel", "author", 7000, "BURIDA"),
                RightsSplit("pty_marcory_rec", "Marcory Sound Lab", "producer", 3000, "BURIDA")
            )
        ),
        // 4. Soleil de Bassam — Zouglou (Lineage Family 2)
        buildTrack(
            id = "trk_04_soleil_bassam",
            workId = "wrk_02_soleil_bassam",
            title = "Soleil de Bassam",
            artistId = "art_awa_kone",
            artistName = "Awa Koné & Les Frères",
            releaseTitle = "Horizon Grand-Bassam",
            genre = GenreStyle.ZOUGLOU,
            isrc = "CIABJ2600201",
            iswc = "T-901.310.442-5",
            bpm = 96.0,
            keyTonic = "C",
            keyMode = "major",
            chordLoop = listOf("C", "G", "Am", "F"),
            durationSec = 80,
            coverResId = R.drawable.img_cover_zouglou,
            rootMidi = 60,
            stemSource = StemSource.OFFICIAL,
            chordsVerified = true,
            chordConfidence = 0.94,
            lyricsLines = listOf(
                5.0 to "Sur la plage de Bassam le matin se lève",
                14.0 to "Un vieux sample de kora guide nos pas",
                24.0 to "La basse caresse les vagues de l'océan",
                34.0 to "Refrain : Soleil de Bassam, garde le tempo",
                46.0 to "Chante avec nous, coupe la voix si tu veux jouer",
                60.0 to "Le zouglou rassemble toute la cour"
            ),
            splits = listOf(
                RightsSplit("pty_awa_k", "Awa Koné (Auteure/Voix)", "author", 6000, "BURIDA"),
                RightsSplit("pty_bassam_pub", "Éditions Lagune", "publisher", 4000, "BURIDA")
            ),
            isDownloadedOffline = true
        ),
        // 5. Kora Matin 1984 — sample_of source for Soleil de Bassam
        buildTrack(
            id = "trk_05_kora_1984",
            workId = "wrk_03_kora_1984",
            title = "Kora Matin 1984 (Sample d'origine)",
            artistId = "art_diallo_m",
            artistName = "Moussa Diallo (Archives)",
            releaseTitle = "Patrimoine Mandingue",
            genre = GenreStyle.AFROBEATS,
            isrc = "CIABJ8400012",
            iswc = "T-104.880.119-0",
            bpm = 88.0,
            keyTonic = "C",
            keyMode = "major",
            chordLoop = listOf("C", "F", "C", "G"),
            durationSec = 48,
            coverResId = R.drawable.img_cover_zouglou,
            rootMidi = 60,
            stemSource = StemSource.AI,
            chordsVerified = true,
            chordConfidence = 0.85,
            lyricsLines = listOf(
                4.0 to "(Instrumentale — Kora & Balafon d'origine)",
                18.0 to "Passage samplé à 0:18 dans Soleil de Bassam",
                32.0 to "Thème traditionnel en Do majeur"
            ),
            splits = listOf(
                RightsSplit("pty_diallo", "Succession Moussa Diallo", "composer", 10000, "BURIDA")
            )
        ),
        // 6. Plateau Flow — Rap Ivoire (Lineage Family 3)
        buildTrack(
            id = "trk_06_plateau_flow",
            workId = "wrk_04_plateau_flow",
            title = "Plateau Flow",
            artistId = "art_mariam_prod",
            artistName = "Kader B feat. Mariam",
            releaseTitle = "Tours d'Ivoire",
            genre = GenreStyle.RAP_IVOIRE,
            isrc = "CIABJ2600301",
            iswc = "T-902.512.008-9",
            bpm = 102.0,
            keyTonic = "F",
            keyMode = "minor",
            chordLoop = listOf("Fm", "Db", "Ab", "Eb"),
            durationSec = 74,
            coverResId = R.drawable.img_cover_rapivoire,
            rootMidi = 53,
            stemSource = StemSource.OFFICIAL,
            chordsVerified = true,
            chordConfidence = 0.87,
            lyricsLines = listOf(
                4.0 to "Depuis les tours du Plateau, on regarde la lagune",
                12.0 to "808 lourde sous les pavés d'Abidjan",
                22.0 to "Mariam découpe les stems en studio à Bingerville",
                32.0 to "Refrain : Plateau Flow, quatre pistes en synchro",
                44.0 to "Fork le couplet, garde la part pour les créateurs",
                58.0 to "Chaque mesure compte dans le grand livre"
            ),
            splits = listOf(
                RightsSplit("pty_kader", "Kader B (Auteur/Rap)", "author", 4500, "BURIDA"),
                RightsSplit("pty_mariam", "Mariam Beatmaker", "composer", 4500, "BURIDA"),
                RightsSplit("pty_plateau_rec", "Plateau Records", "publisher", 1000, "BURIDA")
            )
        ),
        // 7. Plateau Flow (Démo Studio) — demo_of
        buildTrack(
            id = "trk_07_plateau_demo",
            workId = "wrk_04_plateau_flow",
            title = "Plateau Flow (Démo Studio)",
            artistId = "art_mariam_prod",
            artistName = "Mariam Beatmaker (Maquette)",
            releaseTitle = "Archives Bingerville",
            genre = GenreStyle.RAP_IVOIRE,
            isrc = "CIABJ2600302",
            iswc = "T-902.512.008-9",
            bpm = 100.0,
            keyTonic = "F",
            keyMode = "minor",
            chordLoop = listOf("Fm", "Db", "Ab", "Eb"),
            durationSec = 68,
            coverResId = R.drawable.img_cover_rapivoire,
            rootMidi = 53,
            stemSource = StemSource.OFFICIAL,
            chordsVerified = true,
            chordConfidence = 0.90,
            lyricsLines = listOf(
                4.0 to "(Maquette instrumentale + voix témoin)",
                20.0 to "Test de ligne de basse 808 sans batterie finale",
                38.0 to "Première prise du refrain à 100 BPM"
            ),
            splits = listOf(
                RightsSplit("pty_mariam", "Mariam Beatmaker", "composer", 10000, "BURIDA")
            )
        ),
        // 8. Grâce à Yamoussoukro — Gospel
        buildTrack(
            id = "trk_08_grace_yamoussoukro",
            workId = "wrk_05_grace_yamoussoukro",
            title = "Grâce à Yamoussoukro",
            artistId = "art_chorale_espoir",
            artistName = "Chorale Espoir & Yao",
            releaseTitle = "Louange Ouverte",
            genre = GenreStyle.GOSPEL,
            isrc = "CIABJ2600401",
            iswc = "T-903.004.712-3",
            bpm = 84.0,
            keyTonic = "G",
            keyMode = "major",
            chordLoop = listOf("G", "D", "Em", "C"),
            durationSec = 78,
            coverResId = R.drawable.img_hero_strata,
            rootMidi = 55,
            stemSource = StemSource.OFFICIAL,
            chordsVerified = true,
            chordConfidence = 0.96,
            lyricsLines = listOf(
                5.0 to "Le chœur s'élève dans la paix du matin",
                16.0 to "Isole la basse pour répéter avec l'église",
                28.0 to "Refrain : Grâce infinie sur la terre d'Ivoire",
                42.0 to "Transpose d'un demi-ton pour ta voix",
                56.0 to "Répétition à 75 % avant le culte de dimanche"
            ),
            splits = listOf(
                RightsSplit("pty_chorale", "Chorale Espoir", "performer", 6000, "BURIDA"),
                RightsSplit("pty_yao", "Yao Konan (Arrangements)", "composer", 4000, "BURIDA")
            )
        )
    )

    val versionLinks: List<VersionLink> = listOf(
        // Family 1: Abidjan Minuit <-> Reprise Zouglou <-> Live
        VersionLink(
            id = "lnk_01",
            sourceTrackId = "trk_01_abidjan_minuit",
            targetTrackId = "trk_02_abidjan_zouglou",
            linkType = VersionLinkType.COVER_OF,
            provenance = LinkProvenance.ARTIST,
            confidence = 0.95,
            coverage = 1.0,
            mapping = listOf(
                AlignmentPoint(0.0, 0.0),
                AlignmentPoint(16.0, 20.2),
                AlignmentPoint(32.0, 40.5),
                AlignmentPoint(48.0, 60.8),
                AlignmentPoint(76.0, 76.0)
            )
        ),
        VersionLink(
            id = "lnk_02",
            sourceTrackId = "trk_01_abidjan_minuit",
            targetTrackId = "trk_03_abidjan_live",
            linkType = VersionLinkType.LIVE_OF,
            provenance = LinkProvenance.COMMUNITY,
            confidence = 0.88,
            coverage = 0.94,
            mapping = listOf(
                AlignmentPoint(0.0, 0.0),
                AlignmentPoint(20.0, 19.3),
                AlignmentPoint(40.0, 38.6),
                AlignmentPoint(72.0, 70.0)
            )
        ),
        // Family 2: Soleil de Bassam <-> Kora Matin 1984 (sample_of segment)
        VersionLink(
            id = "lnk_03",
            sourceTrackId = "trk_04_soleil_bassam",
            targetTrackId = "trk_05_kora_1984",
            linkType = VersionLinkType.SAMPLE_OF,
            provenance = LinkProvenance.STAFF,
            confidence = 0.97,
            coverage = 0.42,
            mapping = listOf(
                AlignmentPoint(12.0, 16.0),
                AlignmentPoint(24.0, 29.1),
                AlignmentPoint(36.0, 42.2)
            )
        ),
        // Family 3: Plateau Flow <-> Démo Studio
        VersionLink(
            id = "lnk_04",
            sourceTrackId = "trk_06_plateau_flow",
            targetTrackId = "trk_07_plateau_demo",
            linkType = VersionLinkType.DEMO_OF,
            provenance = LinkProvenance.ARTIST,
            confidence = 0.93,
            coverage = 0.90,
            mapping = listOf(
                AlignmentPoint(0.0, 0.0),
                AlignmentPoint(32.0, 32.6),
                AlignmentPoint(64.0, 65.2)
            )
        )
    )

    val sampleForks: List<ForkSpec> = listOf(
        ForkSpec(
            forkId = "frk_01_mariam_bass",
            baseTrackId = "trk_01_abidjan_minuit",
            authorUserId = "usr_mariam",
            authorName = "Mariam Beatmaker",
            title = "Abidjan Minuit — Mariam Sub & Drums Edit",
            timeline = listOf(
                TimelineSlice(0, 32, 16),
                TimelineSlice(32, 64, 0)
            ),
            stemGainsDb = mapOf(
                StemType.VOCALS to -12f,
                StemType.DRUMS to 3.5f,
                StemType.BASS to 4.5f,
                StemType.OTHER to -2f
            ),
            hasOriginalAddedTrack = true,
            tempoBpm = 126.0,
            depthFromOriginal = 1
        )
    )

    val initialArtistAnalytics: Map<String, ArtistAnalytics> = mapOf(
        "trk_01_abidjan_minuit" to ArtistAnalytics(
            totalPlays = 14820,
            stemIsolationCounts = mapOf(
                StemType.BASS to 4210,
                StemType.DRUMS to 3190,
                StemType.VOCALS to 5120,
                StemType.OTHER to 1450
            ),
            topLoopedSection = "Refrain (0:32 – 0:48)",
            activeForksCount = 19,
            orchestraSessionsCount = 84
        ),
        "trk_04_soleil_bassam" to ArtistAnalytics(
            totalPlays = 9340,
            stemIsolationCounts = mapOf(
                StemType.BASS to 2980,
                StemType.DRUMS to 1420,
                StemType.VOCALS to 3100,
                StemType.OTHER to 2410
            ),
            topLoopedSection = "Couplet 1 (0:14 – 0:34)",
            activeForksCount = 7,
            orchestraSessionsCount = 41
        )
    )

    fun linksForTrack(trackId: String): List<VersionLink> {
        return versionLinks.filter { it.sourceTrackId == trackId || it.targetTrackId == trackId }
    }

    fun getTrackById(trackId: String): Track? = tracks.firstOrNull { it.id == trackId }

    private fun buildTrack(
        id: String,
        workId: String,
        title: String,
        artistId: String,
        artistName: String,
        releaseTitle: String,
        genre: GenreStyle,
        isrc: String,
        iswc: String,
        bpm: Double,
        keyTonic: String,
        keyMode: String,
        chordLoop: List<String>,
        durationSec: Int,
        coverResId: Int,
        rootMidi: Int,
        stemSource: StemSource,
        chordsVerified: Boolean,
        chordConfidence: Double,
        lyricsLines: List<Pair<Double, String>>,
        splits: List<RightsSplit>,
        isVariableTempo: Boolean = false,
        isDownloadedOffline: Boolean = false
    ): Track {
        val beatSec = 60.0 / bpm
        val totalBeats = (durationSec / beatSec).toInt()
        val beats = (0 until totalBeats).map { idx ->
            BeatPoint(tSec = idx * beatSec, beatInBar = (idx % 4) + 1)
        }
        val barSec = beatSec * 4.0
        val numBars = (durationSec / barSec).toInt().coerceAtLeast(1)
        val chords = (0 until numBars).map { barIdx ->
            ChordEvent(
                startSec = barIdx * barSec,
                endSec = (barIdx + 1) * barSec,
                label = chordLoop[barIdx % chordLoop.size],
                confidence = chordConfidence
            )
        }
        val d = durationSec.toDouble()
        val sections = listOf(
            SectionSegment(0.0, d * 0.14, "Intro"),
            SectionSegment(d * 0.14, d * 0.42, "Couplet 1"),
            SectionSegment(d * 0.42, d * 0.68, "Refrain"),
            SectionSegment(d * 0.68, d * 0.86, "Pont"),
            SectionSegment(d * 0.86, d, "Outro")
        )
        val lyrics = lyricsLines.mapIndexed { idx, (start, text) ->
            val end = lyricsLines.getOrNull(idx + 1)?.first ?: (start + 5.5).coerceAtMost(d)
            val wordsRaw = text.split(" ")
            val wordSpan = ((end - start) / wordsRaw.size.coerceAtLeast(1)).coerceAtLeast(0.25)
            val words = wordsRaw.mapIndexed { wIdx, w ->
                LyricWord(
                    startSec = start + wIdx * wordSpan,
                    endSec = start + (wIdx + 1) * wordSpan,
                    text = w
                )
            }
            LyricLine(start, end, text, words)
        }
        val stems = StemType.entries.mapIndexed { sIdx, st ->
            val peaks = List(64) { i ->
                val wave = abs(sin((i + sIdx * 7) * 0.37)) * 0.65f + 0.2f
                wave.toFloat().coerceIn(0.12f, 0.96f)
            }
            StemInfo(
                type = st,
                source = stemSource,
                model = if (stemSource == StemSource.OFFICIAL) "studio_master_24b" else "htdemucs_ft@4.0",
                waveformPeaks = peaks
            )
        }
        return Track(
            id = id,
            workId = workId,
            title = title,
            artistId = artistId,
            artistName = artistName,
            releaseTitle = releaseTitle,
            genre = genre,
            isrc = isrc,
            iswc = iswc,
            durationMs = durationSec * 1000L,
            coverResId = coverResId,
            analysis = TrackAnalysis(
                trackId = id,
                durationMs = durationSec * 1000L,
                tempo = TempoInfo(bpm = bpm, confidence = 0.94, isVariable = isVariableTempo),
                beats = beats,
                key = KeyInfo(keyTonic, keyMode, 0.89),
                chords = chords,
                sections = sections,
                lyrics = lyrics,
                stems = stems,
                humanReviewed = HumanReviewed(chords = chordsVerified, sections = true, lyrics = true)
            ),
            license = LicenseConfig(),
            splits = splits,
            rootPitchMidi = rootMidi,
            isDownloadedOffline = isDownloadedOffline
        )
    }
}
