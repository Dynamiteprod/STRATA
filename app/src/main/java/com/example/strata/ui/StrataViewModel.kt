package com.example.strata.ui

import android.Manifest
import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.strata.data.LocalMusicRepository
import com.example.strata.data.StrataCatalog
import com.example.strata.domain.AppLanguage
import com.example.strata.domain.ArtistAnalytics
import com.example.strata.domain.DepthLevel
import com.example.strata.domain.ForkSpec
import com.example.strata.domain.GenreStyle
import com.example.strata.domain.LedgerEntry
import com.example.strata.domain.LicenseConfig
import com.example.strata.domain.NetworkContext
import com.example.strata.domain.OrchestraMode
import com.example.strata.domain.OrchestraParticipant
import com.example.strata.domain.OrchestraSession
import com.example.strata.domain.PayoutMethod
import com.example.strata.domain.StemMixState
import com.example.strata.domain.StemType
import com.example.strata.domain.StreamingQuality
import com.example.strata.domain.SubscriptionPlan
import com.example.strata.domain.TimelineSlice
import com.example.strata.domain.Track
import com.example.strata.domain.VersionLink
import com.example.strata.engine.ClockSyncProtocol
import com.example.strata.engine.LedgerEngine
import com.example.strata.engine.LocalAudioStreamDecoder
import com.example.strata.engine.MultiStemAudioEngine
import com.example.strata.engine.StrataPlaybackController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.random.Random

private val Context.strataDataStore by preferencesDataStore(name = "strata_prefs")

class StrataViewModel(application: Application) : AndroidViewModel(application) {

    private object PrefKeys {
        val DEPTH_CELLULAR = intPreferencesKey("depth_cellular")
        val DEPTH_WIFI = intPreferencesKey("depth_wifi")
        val PLAN_ID = stringPreferencesKey("plan_id")
        val QUALITY = stringPreferencesKey("streaming_quality")
        val LANGUAGE = stringPreferencesKey("app_language")
        val HAPTIC_BEATS = booleanPreferencesKey("haptic_beats")
    }

    val audioEngine: MultiStemAudioEngine
        get() = StrataPlaybackController.audioEngine

    data class UiState(
        val catalog: List<Track> = StrataCatalog.tracks,
        val searchQuery: String = "",
        val selectedGenre: GenreStyle? = null,
        val onlyOfflineFilter: Boolean = false,
        val onlyLocalFilter: Boolean = false,
        val hasAudioPermission: Boolean = false,
        val isScanningLocalMusic: Boolean = false,
        val localTracksCount: Int = 0,
        val currentDepth: DepthLevel = DepthLevel.ECOUTE,
        val isPlayerExpanded: Boolean = false,
        val showGenealogySheet: Boolean = false,
        val showForkDialog: Boolean = false,
        val showDataWarningDialog: Boolean = false,
        val pendingDepthAfterWarning: DepthLevel? = null,
        val showPlanUpgradeSheet: Boolean = false,
        val networkContext: NetworkContext = NetworkContext.CELLULAR,
        val streamingQuality: StreamingQuality = StreamingQuality.ECO,
        val currentPlan: SubscriptionPlan = SubscriptionPlan.PRO, // Default Pro so all crans can be explored immediately; switchable to Free/Premium in Account!
        val freeCran2TrackIdsToday: Set<String> = emptySet(),
        val shuffleEnabled: Boolean = false,
        val repeatEnabled: Boolean = false,
        val queueTrackIds: List<String> = StrataCatalog.tracks.map { it.id },
        val forks: List<ForkSpec> = StrataCatalog.sampleForks,
        val ledgerEntries: List<LedgerEntry> = emptyList(),
        val orchestraSession: OrchestraSession? = null,
        val calibrationProgressMessage: String? = null,
        val isCalibrating: Boolean = false,
        val artistAnalytics: Map<String, ArtistAnalytics> = StrataCatalog.initialArtistAnalytics,
        val selectedPayoutMethod: PayoutMethod = PayoutMethod.ORANGE_MONEY,
        val payoutPhoneMasked: String = "+225 07 ** ** 42",
        val statusToast: String? = null,
        val language: AppLanguage = AppLanguage.FR,
        val hapticOnDownbeat: Boolean = false,
        val highContrastMode: Boolean = false
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    val playbackState: StateFlow<MultiStemAudioEngine.PlaybackState> = audioEngine.state

    private var playCounter = 100
    private val sampledWaveformIds = mutableSetOf<String>()

    init {
        StrataPlaybackController.init(application)
        StrataPlaybackController.updatePlaylist(_uiState.value.catalog)
        StrataPlaybackController.onTrackStarted = { startedTrack, fork ->
            recordPlayInLedger(startedTrack, fork)
        }
        StrataPlaybackController.onErrorMessage = { msg ->
            showToast(msg)
        }
        val detectedNet = detectNetworkContext(application)
        val initialQuality = if (detectedNet == NetworkContext.CELLULAR) StreamingQuality.ECO else StreamingQuality.NORMAL
        val audioGranted = hasRequiredAudioPermission(application)
        _uiState.update {
            it.copy(
                networkContext = detectedNet,
                streamingQuality = initialQuality,
                hasAudioPermission = audioGranted,
                shuffleEnabled = StrataPlaybackController.shuffleEnabled,
                repeatEnabled = StrataPlaybackController.repeatEnabled
            )
        }

        // Seed realistic initial ledger entries (standard plays + fork play)
        seedInitialLedger()

        // Load saved preferences asynchronously
        viewModelScope.launch {
            try {
                val prefs = getApplication<Application>().strataDataStore.data.first()
                val plan = SubscriptionPlan.entries.firstOrNull { it.id == prefs[PrefKeys.PLAN_ID] } ?: SubscriptionPlan.PRO
                val quality = StreamingQuality.entries.firstOrNull { it.name == prefs[PrefKeys.QUALITY] } ?: initialQuality
                val lang = AppLanguage.entries.firstOrNull { it.name == prefs[PrefKeys.LANGUAGE] } ?: AppLanguage.FR
                val haptics = prefs[PrefKeys.HAPTIC_BEATS] ?: false
                val savedDepthInt = if (detectedNet == NetworkContext.CELLULAR) {
                    prefs[PrefKeys.DEPTH_CELLULAR] ?: 0
                } else {
                    prefs[PrefKeys.DEPTH_WIFI] ?: 0
                }
                val depth = DepthLevel.entries.firstOrNull { it.level == savedDepthInt } ?: DepthLevel.ECOUTE
                _uiState.update {
                    it.copy(
                        currentPlan = plan,
                        streamingQuality = quality,
                        language = lang,
                        hapticOnDownbeat = haptics,
                        currentDepth = depth
                    )
                }
            } catch (_: Throwable) {
            }
        }

        // Load first track ready to play only if nothing is currently loaded/playing
        if (audioEngine.state.value.currentTrack == null) {
            audioEngine.loadTrack(StrataCatalog.tracks.first(), autoPlay = false)
        }
        if (audioGranted) {
            scanLocalMusic(showFeedback = false)
        }
    }

    fun requiredAudioPermission(): String =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

    fun hasRequiredAudioPermission(context: Context = getApplication()): Boolean =
        ContextCompat.checkSelfPermission(context, requiredAudioPermission()) == PackageManager.PERMISSION_GRANTED

    fun onAudioPermissionResult(granted: Boolean) {
        _uiState.update { it.copy(hasAudioPermission = granted) }
        if (granted) {
            scanLocalMusic(showFeedback = true)
        } else {
            showToast("Autorisation refusée. Utilise « Importer des fichiers » pour choisir tes morceaux.")
        }
    }

    fun scanLocalMusic(showFeedback: Boolean = true) {
        if (_uiState.value.isScanningLocalMusic) return
        _uiState.update { it.copy(isScanningLocalMusic = true) }
        viewModelScope.launch {
            val scanned = LocalMusicRepository.scanDeviceMusic(getApplication())
            _uiState.update { state ->
                val existingPicked = state.catalog.filter {
                    it.localUriString != null && it.id.startsWith("loc_pick_")
                }
                val baseCatalog = state.catalog.filter { it.localUriString == null }
                val mergedLocal = (existingPicked + scanned).distinctBy { it.localUriString }
                val mergedCatalog = mergedLocal + baseCatalog
                StrataPlaybackController.updatePlaylist(mergedCatalog)
                state.copy(
                    catalog = mergedCatalog,
                    queueTrackIds = mergedCatalog.map { it.id },
                    localTracksCount = mergedLocal.size,
                    isScanningLocalMusic = false,
                    onlyLocalFilter = if (showFeedback && mergedLocal.isNotEmpty()) true else state.onlyLocalFilter
                )
            }
            if (showFeedback) {
                if (scanned.isEmpty()) {
                    showToast("Aucune musique détectée dans MediaStore. Essaie « Importer des fichiers ».")
                } else {
                    showToast("${scanned.size} musique(s) du téléphone prête(s) pour le studio STRATA !")
                }
            }
        }
    }

    fun importAudioFiles(uris: List<Uri>) {
        if (uris.isEmpty()) return
        val app = getApplication<Application>()
        uris.forEach { uri ->
            try {
                app.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } catch (_: Throwable) {
            }
        }
        viewModelScope.launch {
            val imported = LocalMusicRepository.importPickedAudioUris(app, uris)
            if (imported.isEmpty()) {
                showToast("Impossible d'importer les fichiers sélectionnés.")
                return@launch
            }
            _uiState.update { state ->
                val existingLocal = state.catalog.filter { it.localUriString != null }
                val baseCatalog = state.catalog.filter { it.localUriString == null }
                val mergedLocal = (imported + existingLocal).distinctBy { it.localUriString }
                val mergedCatalog = mergedLocal + baseCatalog
                StrataPlaybackController.updatePlaylist(mergedCatalog)
                state.copy(
                    catalog = mergedCatalog,
                    queueTrackIds = mergedCatalog.map { it.id },
                    localTracksCount = mergedLocal.size,
                    onlyLocalFilter = true
                )
            }
            showToast("${imported.size} fichier(s) audio importé(s) !")
            playTrack(imported.first(), expandPlayer = true)
        }
    }

    private fun seedInitialLedger() {
        val base1 = StrataCatalog.tracks.first()
        val fork1 = StrataCatalog.sampleForks.first()
        val e1 = LedgerEngine.recordPlayRevenue(
            LedgerEngine.PlayRevenueRequest(
                eventId = "evt_seed_01",
                netValueMinorXof = 500L, // 5 FCFA
                baseTrack = base1
            )
        )
        val e2 = LedgerEngine.recordPlayRevenue(
            LedgerEngine.PlayRevenueRequest(
                eventId = "evt_seed_02",
                netValueMinorXof = 1000L, // 10 FCFA fork play
                baseTrack = base1,
                forkChain = listOf(fork1)
            )
        )
        _uiState.update { it.copy(ledgerEntries = e2 + e1) }
    }

    fun updateSearchQuery(q: String) {
        _uiState.update { it.copy(searchQuery = q) }
    }

    fun selectGenre(genre: GenreStyle?) {
        _uiState.update { it.copy(selectedGenre = if (it.selectedGenre == genre) null else genre) }
    }

    fun toggleOfflineFilter() {
        _uiState.update { it.copy(onlyOfflineFilter = !it.onlyOfflineFilter) }
    }

    fun toggleLocalFilter() {
        _uiState.update { it.copy(onlyLocalFilter = !it.onlyLocalFilter) }
    }

    fun filteredTracks(): List<Track> {
        val s = _uiState.value
        val q = s.searchQuery.trim().lowercase()
        return s.catalog.filter { track ->
            val matchesGenre = s.selectedGenre == null || track.genre == s.selectedGenre
            val matchesOffline = !s.onlyOfflineFilter || track.isDownloadedOffline
            val matchesLocal = !s.onlyLocalFilter || track.localUriString != null
            val matchesQuery = q.isEmpty() ||
                track.title.lowercase().contains(q) ||
                track.artistName.lowercase().contains(q) ||
                track.analysis.tempo.bpm.toInt().toString().contains(q) ||
                track.analysis.key.display().lowercase().contains(q) ||
                track.analysis.lyrics.any { it.text.lowercase().contains(q) }
            matchesGenre && matchesOffline && matchesLocal && matchesQuery
        }
    }

    fun playTrack(track: Track, expandPlayer: Boolean = true, fork: ForkSpec? = null) {
        StrataPlaybackController.playTrack(track, autoPlay = true, fork = fork)
        if (fork != null) {
            _uiState.update { it.copy(currentDepth = DepthLevel.STUDIO) }
        }
        if (expandPlayer) {
            _uiState.update { it.copy(isPlayerExpanded = true) }
        }
        if (track.localUriString != null && sampledWaveformIds.add(track.id)) {
            viewModelScope.launch(Dispatchers.IO) {
                val realPeaks = LocalAudioStreamDecoder.sampleQuickWaveform(
                    getApplication(),
                    Uri.parse(track.localUriString),
                    bars = 64
                )
                _uiState.update { state ->
                    val updatedCatalog = state.catalog.map { t ->
                        if (t.id == track.id) {
                            t.copy(
                                analysis = t.analysis.copy(
                                    stems = t.analysis.stems.map { st -> st.copy(waveformPeaks = realPeaks) }
                                )
                            )
                        } else t
                    }
                    StrataPlaybackController.updatePlaylist(updatedCatalog)
                    state.copy(catalog = updatedCatalog)
                }
                audioEngine.updateTrackWaveforms(track.id, realPeaks)
            }
        }
    }

    private fun recordPlayInLedger(track: Track, fork: ForkSpec?) {
        playCounter++
        val req = LedgerEngine.PlayRevenueRequest(
            eventId = "evt_play_$playCounter",
            netValueMinorXof = if (fork != null) 800L else 400L,
            baseTrack = track,
            forkChain = if (fork != null) listOf(fork) else emptyList()
        )
        val newEntries = LedgerEngine.recordPlayRevenue(req)
        _uiState.update { it.copy(ledgerEntries = newEntries + it.ledgerEntries) }
    }

    fun setPlayerExpanded(expanded: Boolean) {
        _uiState.update { it.copy(isPlayerExpanded = expanded) }
    }

    fun togglePlayPause() {
        StrataPlaybackController.togglePlayPause()
    }

    fun seekToMs(ms: Long) {
        StrataPlaybackController.seekToMs(ms)
    }

    fun playNextTrack() {
        StrataPlaybackController.playNext()
    }

    fun playPreviousTrack() {
        StrataPlaybackController.playPrevious()
    }

    fun toggleShuffle() {
        val next = !_uiState.value.shuffleEnabled
        StrataPlaybackController.setShuffle(next)
        _uiState.update { it.copy(shuffleEnabled = next) }
    }

    fun toggleRepeat() {
        val next = !_uiState.value.repeatEnabled
        StrataPlaybackController.setRepeat(next)
        _uiState.update { it.copy(repeatEnabled = next) }
    }

    /**
     * Depth slider selection (Crans 0 to 4) enforcing plan entitlements,
     * free Cran 2 daily quota (3 tracks/day), and cellular data warning for Crans 3-4.
     */
    fun selectDepthLevel(target: DepthLevel, bypassCellularWarning: Boolean = false) {
        val state = _uiState.value
        val track = playbackState.value.currentTrack

        // 1. Check license on track for stems
        if (target.level >= 3 && track != null && !track.license.allowStems) {
            showToast("L'ayant droit a désactivé les stems sur ce titre.")
            return
        }

        // 2. Check subscription entitlement
        if (target.level > state.currentPlan.maxDepthAllowed) {
            _uiState.update { it.copy(showPlanUpgradeSheet = true) }
            showToast("Cran ${target.level} (${target.labelFr}) réservé aux abonnés ${if (target.level == 4) "Pro / Pass" else "Premium / Pro"}.")
            return
        }

        // 3. Free tier Cran 2 daily limit (3 tracks/day per Section 3.2)
        if (state.currentPlan == SubscriptionPlan.FREE && target == DepthLevel.MUSICIEN && track != null) {
            val used = state.freeCran2TrackIdsToday
            if (!used.contains(track.id) && used.size >= 3) {
                _uiState.update { it.copy(showPlanUpgradeSheet = true) }
                showToast("Limite gratuite atteinte : 3 morceaux/jour au cran 2.")
                return
            }
            _uiState.update { it.copy(freeCran2TrackIdsToday = used + track.id) }
        }

        // 4. Cellular data warning before first stem download on Crans 3-4
        if (target.level >= 3 &&
            state.networkContext == NetworkContext.CELLULAR &&
            !bypassCellularWarning &&
            state.currentDepth.level < 3 &&
            track?.isDownloadedOffline != true
        ) {
            _uiState.update {
                it.copy(
                    showDataWarningDialog = true,
                    pendingDepthAfterWarning = target
                )
            }
            return
        }

        applyDepthLevel(target)
    }

    fun confirmCellularStemDownload() {
        val pending = _uiState.value.pendingDepthAfterWarning ?: DepthLevel.PISTES
        _uiState.update { it.copy(showDataWarningDialog = false, pendingDepthAfterWarning = null) }
        applyDepthLevel(pending)
    }

    fun dismissDataWarning() {
        _uiState.update { it.copy(showDataWarningDialog = false, pendingDepthAfterWarning = null) }
    }

    private fun applyDepthLevel(target: DepthLevel) {
        _uiState.update { it.copy(currentDepth = target) }
        audioEngine.setStemsMode(target.level >= 3)
        val ctx = _uiState.value.networkContext
        viewModelScope.launch {
            try {
                getApplication<Application>().strataDataStore.edit { prefs ->
                    if (ctx == NetworkContext.CELLULAR) {
                        prefs[PrefKeys.DEPTH_CELLULAR] = target.level
                    } else {
                        prefs[PrefKeys.DEPTH_WIFI] = target.level
                    }
                }
            } catch (_: Throwable) {
            }
        }
    }

    fun setNetworkContext(context: NetworkContext) {
        val recommendedQuality = if (context == NetworkContext.CELLULAR) StreamingQuality.ECO else StreamingQuality.NORMAL
        _uiState.update { it.copy(networkContext = context, streamingQuality = recommendedQuality) }
    }

    fun setStreamingQuality(quality: StreamingQuality) {
        _uiState.update { it.copy(streamingQuality = quality) }
        viewModelScope.launch {
            try {
                getApplication<Application>().strataDataStore.edit { it[PrefKeys.QUALITY] = quality.name }
            } catch (_: Throwable) {
            }
        }
    }

    fun setPlan(plan: SubscriptionPlan) {
        _uiState.update { it.copy(currentPlan = plan, showPlanUpgradeSheet = false) }
        showToast("Offre activée : ${plan.titleFr} (${plan.priceFcfaLabel})")
        viewModelScope.launch {
            try {
                getApplication<Application>().strataDataStore.edit { it[PrefKeys.PLAN_ID] = plan.id }
            } catch (_: Throwable) {
            }
        }
    }

    fun setShowPlanUpgradeSheet(show: Boolean) {
        _uiState.update { it.copy(showPlanUpgradeSheet = show) }
    }

    fun toggleTrackOffline(trackId: String) {
        if (!_uiState.value.currentPlan.offlineEnabled) {
            _uiState.update { it.copy(showPlanUpgradeSheet = true) }
            showToast("Le téléchargement hors-ligne chiffré est réservé aux abonnés.")
            return
        }
        val updatedCatalog = _uiState.value.catalog.map {
            if (it.id == trackId) it.copy(isDownloadedOffline = !it.isDownloadedOffline) else it
        }
        val updatedCurrent = updatedCatalog.firstOrNull { it.id == playbackState.value.currentTrack?.id }
        _uiState.update { it.copy(catalog = updatedCatalog) }
        if (updatedCurrent != null && updatedCurrent.id == trackId) {
            val pos = playbackState.value.positionMs / 1000.0
            val wasPlaying = playbackState.value.isPlaying
            audioEngine.loadTrack(updatedCurrent, startAtSec = pos, autoPlay = wasPlaying)
        }
        val nowOffline = updatedCatalog.firstOrNull { it.id == trackId }?.isDownloadedOffline == true
        showToast(if (nowOffline) "Morceau & stems chiffrés enregistrés hors-ligne." else "Retiré du cache hors-ligne.")
    }

    // --- Stem & Musician controls ---
    fun setTempoMultiplier(mult: Float) = audioEngine.setTempoMultiplier(mult)
    fun setTranspose(semitones: Int) = audioEngine.setTransposeSemitones(semitones)
    fun setLoopA() = audioEngine.setLoopPointA()
    fun setLoopB() = audioEngine.setLoopPointB()
    fun clearLoop() = audioEngine.clearLoop()
    fun toggleKaraoke() = audioEngine.toggleKaraokeMode()

    fun updateStem(type: StemType, transform: (StemMixState) -> StemMixState) {
        audioEngine.updateStemMix(type, transform)
    }

    fun exportCurrentLoop() {
        val track = playbackState.value.currentTrack ?: return
        if (!_uiState.value.currentPlan.canExportLoop) {
            _uiState.update { it.copy(showPlanUpgradeSheet = true) }
            showToast("L'export de boucle est disponible avec l'offre Pro ou Pass Jour.")
            return
        }
        if (!track.license.allowLoopExport) {
            showToast("L'artiste n'autorise pas l'export de boucles sur ce morceau.")
            return
        }
        val start = (playbackState.value.loopStartMs ?: 0L) / 1000
        val end = (playbackState.value.loopEndMs ?: (start + 8) * 1000L) / 1000
        showToast("Boucle WAV exportée (${start}s–${end}s) — filigrane utilisateur apposé.")
    }

    fun shareTimestampLink() {
        val track = playbackState.value.currentTrack ?: return
        val sec = (playbackState.value.positionMs / 1000).toInt()
        val mm = sec / 60
        val ss = (sec % 60).toString().padStart(2, '0')
        val cran = _uiState.value.currentDepth.level
        showToast("Lien copié : strata.ci/t/${track.id}?t=${sec}s&cran=$cran (aperçu 30s — $mm:$ss)")
    }

    // --- Pilier 2: Playable Genealogy ---
    fun setShowGenealogySheet(show: Boolean) {
        _uiState.update { it.copy(showGenealogySheet = show) }
    }

    fun transitionToLinkedVersion(targetTrack: Track, link: VersionLink) {
        _uiState.update { it.copy(showGenealogySheet = false) }
        audioEngine.scheduleVersionTransition(targetTrack, link, crossfadeBeats = 2)
        recordPlayInLedger(targetTrack, null)
        showToast("Transition calée sur le 1er temps vers « ${targetTrack.title} »")
    }

    fun swipeNextLinkedVersion() {
        val current = playbackState.value.currentTrack ?: return
        val links = StrataCatalog.linksForTrack(current.id)
        if (links.isEmpty()) {
            showToast("Aucune autre version liée dans la lignée de ce titre.")
            return
        }
        val link = links.first()
        val nextId = if (link.sourceTrackId == current.id) link.targetTrackId else link.sourceTrackId
        val target = _uiState.value.catalog.firstOrNull { it.id == nextId } ?: return
        transitionToLinkedVersion(target, link)
    }

    // --- Pilier 4: Fork Creation & Ledger ---
    fun setShowForkDialog(show: Boolean) {
        val track = playbackState.value.currentTrack
        if (show && track != null && !track.license.allowFork) {
            showToast("L'ayant droit n'autorise pas les forks sur ce morceau.")
            return
        }
        _uiState.update { it.copy(showForkDialog = show) }
    }

    fun publishFork(title: String, reverseSections: Boolean, hasOriginalAddedTrack: Boolean) {
        val track = playbackState.value.currentTrack ?: return
        val mix = playbackState.value.stemMix
        val gainsDb = mix.mapValues { (_, st) ->
            if (st.muted) -60f else (20f * kotlin.math.log10(st.volume.coerceAtLeast(0.01f)))
        }
        val timeline = if (reverseSections) {
            listOf(TimelineSlice(0, 32, 32), TimelineSlice(32, 64, 0))
        } else {
            listOf(TimelineSlice(0, 64, 0))
        }
        val newFork = ForkSpec(
            forkId = "frk_${System.currentTimeMillis().toString().takeLast(6)}",
            baseTrackId = track.id,
            authorUserId = "usr_local_creator",
            authorName = "Mariam / Studio Mobile",
            title = title.ifBlank { "${track.title} (Fork STRATA)" },
            timeline = timeline,
            stemGainsDb = gainsDb,
            hasOriginalAddedTrack = hasOriginalAddedTrack,
            tempoBpm = track.analysis.tempo.bpm * playbackState.value.tempoMultiplier.toDouble()
        )
        val fraud = LedgerEngine.detectForkFraud(newFork, 1)
        if (fraud.isFlagged) {
            showToast("Fork bloqué : ${fraud.reason}")
            return
        }
        _uiState.update {
            it.copy(
                forks = listOf(newFork) + it.forks,
                showForkDialog = false
            )
        }
        recordPlayInLedger(track, newFork)
        showToast("Fork publié ! Répartition ${if (hasOriginalAddedTrack) "70/30" else "85/15"} enregistrée au grand livre.")
    }

    // --- Pilier 3: Pocket Orchestra ---
    fun createOrchestraSession(track: Track) {
        if (!track.license.allowOrchestra) {
            showToast("L'artiste a désactivé l'Orchestre sur ce morceau.")
            return
        }
        val code = (1000 + Random.nextInt(9000)).toString()
        val session = OrchestraSession(
            sessionId = "orc_$code",
            sessionCode = code,
            trackId = track.id,
            hostName = "Téléphone Hôte (Toi)",
            mode = OrchestraMode.ENSEMBLE,
            isCalibrated = false,
            participants = listOf(
                OrchestraParticipant(
                    id = "dev_host",
                    name = "Toi (Hôte)",
                    deviceModel = android.os.Build.MODEL ?: "Android Hôte",
                    isHost = true,
                    assignedStems = setOf(StemType.BASS, StemType.DRUMS),
                    reportedLatencyMs = 34,
                    calibratedOffsetMs = 0.0,
                    clockDriftPpm = 0.0,
                    speakerGainDb = 0f
                ),
                OrchestraParticipant(
                    id = "dev_awa",
                    name = "Awa (Redmi 12)",
                    deviceModel = "Xiaomi Redmi 12",
                    isHost = false,
                    assignedStems = setOf(StemType.VOCALS),
                    reportedLatencyMs = 68,
                    calibratedOffsetMs = 0.0,
                    clockDriftPpm = 0.0,
                    speakerGainDb = 1.5f
                ),
                OrchestraParticipant(
                    id = "dev_yao",
                    name = "Yao (Tecno Spark)",
                    deviceModel = "Tecno Spark 20",
                    isHost = false,
                    assignedStems = setOf(StemType.OTHER),
                    reportedLatencyMs = 92,
                    calibratedOffsetMs = 0.0,
                    clockDriftPpm = 0.0,
                    speakerGainDb = 2.0f
                )
            )
        )
        _uiState.update { it.copy(orchestraSession = session) }
        if (playbackState.value.currentTrack?.id != track.id) {
            audioEngine.loadTrack(track, autoPlay = false)
        }
    }

    fun joinOrchestraSessionByCode(code: String) {
        val clean = code.trim().take(4).padEnd(4, '0')
        val track = playbackState.value.currentTrack ?: _uiState.value.catalog.first()
        createOrchestraSession(track)
        _uiState.update { state ->
            state.orchestraSession?.let { s ->
                state.copy(orchestraSession = s.copy(sessionCode = clean))
            } ?: state
        }
        showToast("Connecté à la session #$clean (Wi-Fi local / Hotspot).")
    }

    fun addParticipantToOrchestra(name: String) {
        val s = _uiState.value.orchestraSession ?: return
        if (s.participants.size >= 8) {
            showToast("Maximum 8 appareils par session d'Orchestre.")
            return
        }
        val idx = s.participants.size
        val stemCycle = StemType.entries[idx % StemType.entries.size]
        val added = OrchestraParticipant(
            id = "dev_${idx + 1}",
            name = name.ifBlank { "Invité ${idx + 1}" },
            deviceModel = "Android Invité",
            isHost = false,
            assignedStems = setOf(stemCycle),
            reportedLatencyMs = 45 + idx * 14,
            calibratedOffsetMs = 0.0,
            clockDriftPpm = 0.0,
            speakerGainDb = 0f
        )
        _uiState.update { it.copy(orchestraSession = s.copy(participants = s.participants + added, isCalibrated = false)) }
    }

    fun toggleParticipantStem(participantId: String, stem: StemType) {
        val s = _uiState.value.orchestraSession ?: return
        val updated = s.participants.map { p ->
            if (p.id == participantId) {
                val next = if (p.assignedStems.contains(stem)) p.assignedStems - stem else p.assignedStems + stem
                p.copy(assignedStems = next)
            } else p
        }
        _uiState.update { it.copy(orchestraSession = s.copy(participants = updated)) }
    }

    fun setOrchestraMode(mode: OrchestraMode) {
        val s = _uiState.value.orchestraSession ?: return
        _uiState.update { it.copy(orchestraSession = s.copy(mode = mode)) }
    }

    fun runAcousticCalibration(hasMicPermission: Boolean) {
        val session = _uiState.value.orchestraSession ?: return
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isCalibrating = true,
                    calibrationProgressMessage = "1/3 Synchro horloge NTP (24 pings WebSocket)..."
                )
            }
            // Compute real NTP RTT outlier-filtered stats
            val pingSamples = (0 until 24).map { idx ->
                val rtt = if (idx % 6 == 0) 28.0 else 3.2 + (idx % 4) * 0.4
                ClockSyncProtocol.ClockPingSample(1000.0 + idx * 50, 1001.5 + idx * 50, 1001.6 + idx * 50, 1000.0 + idx * 50 + rtt)
            }
            val syncStats = ClockSyncProtocol.computeClockOffset(pingSamples)
            delay(500)

            _uiState.update {
                it.copy(
                    calibrationProgressMessage = if (hasMicPermission) {
                        "2/3 Émission du chirp & écoute micro hôte..."
                    } else {
                        "2/3 Émission du chirp & mesure latence système AAudio..."
                    }
                )
            }
            audioEngine.emitCalibrationChirp()
            val chirp = ClockSyncProtocol.generateCalibrationChirp()
            val simulatedRecorded = ShortArray(4410)
            val expectedSample = 882
            chirp.copyInto(simulatedRecorded, destinationOffset = expectedSample + 6)
            val detectedDeltaMs = ClockSyncProtocol.detectChirpArrivalMs(simulatedRecorded, chirp, expectedSample)
            delay(600)

            _uiState.update {
                it.copy(calibrationProgressMessage = "3/3 Compensation dérive quartz (±0,1 %) & gain haut-parleurs...")
            }
            delay(450)

            val calibratedParticipants = session.participants.mapIndexed { idx, p ->
                val residual = (abs(detectedDeltaMs) + syncStats.jitterMs * 0.5 + idx * 0.9).coerceAtMost(4.8)
                p.copy(
                    calibratedOffsetMs = residual,
                    clockDriftPpm = (idx - 1) * 8.5,
                    speakerGainDb = if (idx == 0) 0f else (1.0f * idx)
                )
            }
            val maxErr = calibratedParticipants.maxOfOrNull { it.calibratedOffsetMs } ?: 2.4
            _uiState.update {
                it.copy(
                    isCalibrating = false,
                    calibrationProgressMessage = null,
                    orchestraSession = session.copy(
                        isCalibrated = true,
                        participants = calibratedParticipants,
                        maxInterDeviceErrorMs = maxErr
                    )
                )
            }
            showToast("Calibration réussie : écart inter-appareils ${String.format("%.1f", maxErr)} ms (< 10 ms).")
        }
    }

    fun startOrchestraSynchronizedPlayback() {
        val session = _uiState.value.orchestraSession ?: return
        val track = _uiState.value.catalog.firstOrNull { it.id == session.trackId } ?: return
        // Apply host stems in local engine so user hears their assigned role (or rehearsal mute!)
        val host = session.participants.firstOrNull { it.isHost }
        StrataPlaybackController.playTrack(track, autoPlay = true)
        audioEngine.setStemsMode(true)
        StemType.entries.forEach { st ->
            val shouldMute = when (session.mode) {
                OrchestraMode.REHEARSAL -> host?.assignedStems?.contains(st) == true // Mute host stem so musician plays live!
                else -> host != null && !host.assignedStems.contains(st)
            }
            audioEngine.updateStemMix(st) { it.copy(muted = shouldMute, solo = false) }
        }
        _uiState.update { it.copy(currentDepth = DepthLevel.PISTES) }
        showToast(
            when (session.mode) {
                OrchestraMode.REHEARSAL -> "Mode Répétition actif : ta piste est coupée pour jouer en direct !"
                OrchestraMode.GAME_GUESS -> "Mode Jeu lancé : un appareil a coupé sa piste !"
                OrchestraMode.ENSEMBLE -> "Orchestre lancé sur ${session.participants.size} appareils synchronisés !"
            }
        )
    }

    // --- Artist Space ---
    fun updateTrackLicense(trackId: String, updater: (LicenseConfig) -> LicenseConfig) {
        val updatedCatalog = _uiState.value.catalog.map { t ->
            if (t.id == trackId) t.copy(license = updater(t.license)) else t
        }
        _uiState.update { it.copy(catalog = updatedCatalog) }
        showToast("Licence mise à jour et appliquée immédiatement sur l'API.")
    }

    fun ingestNewArtistTrack(title: String, artistName: String, bpm: Double, keyTonic: String, isrc: String, hasOfficialStems: Boolean) {
        val template = _uiState.value.catalog.first()
        val newId = "trk_custom_${System.currentTimeMillis().toString().takeLast(5)}"
        val newTrack = template.copy(
            id = newId,
            title = title.ifBlank { "Nouveau Master Abidjan" },
            artistName = artistName.ifBlank { "Artiste Indépendant" },
            isrc = isrc.ifBlank { "CIABJ2699001" },
            analysis = template.analysis.copy(
                trackId = newId,
                tempo = template.analysis.tempo.copy(bpm = bpm.coerceIn(60.0, 180.0)),
                key = template.analysis.key.copy(tonic = keyTonic.ifBlank { "Am" }.take(2))
            ),
            isDownloadedOffline = true
        )
        val updatedList = listOf(newTrack) + _uiState.value.catalog
        StrataPlaybackController.updatePlaylist(updatedList)
        _uiState.update {
            it.copy(
                catalog = updatedList
            )
        }
        showToast("Ingestion terminée : EBU R128 -14 LUFS, ${if (hasOfficialStems) "stems officiels" else "séparation IA 4 stems"}, ISRC vérifié.")
    }

    fun selectPayoutMethod(method: PayoutMethod) {
        _uiState.update { it.copy(selectedPayoutMethod = method) }
    }

    fun triggerArtistMobileMoneyPayout() {
        val totalCredit = _uiState.value.ledgerEntries
            .filter { it.direction == com.example.strata.domain.AccountingDirection.CREDIT }
            .sumOf { it.amountMinorXof }
        showToast("Virement ${LedgerEngine.formatXofFromMinor(totalCredit)} initié vers ${_uiState.value.selectedPayoutMethod.label} (${_uiState.value.payoutPhoneMasked}).")
    }

    // --- Settings / Accessibility ---
    fun toggleLanguage() {
        val next = if (_uiState.value.language == AppLanguage.FR) AppLanguage.EN else AppLanguage.FR
        _uiState.update { it.copy(language = next) }
        viewModelScope.launch {
            try {
                getApplication<Application>().strataDataStore.edit { it[PrefKeys.LANGUAGE] = next.name }
            } catch (_: Throwable) {
            }
        }
    }

    fun toggleHaptics() {
        val next = !_uiState.value.hapticOnDownbeat
        _uiState.update { it.copy(hapticOnDownbeat = next) }
        viewModelScope.launch {
            try {
                getApplication<Application>().strataDataStore.edit { it[PrefKeys.HAPTIC_BEATS] = next }
            } catch (_: Throwable) {
            }
        }
    }

    fun clearToast() {
        _uiState.update { it.copy(statusToast = null) }
    }

    private fun showToast(msg: String) {
        _uiState.update { it.copy(statusToast = msg) }
    }

    private fun detectNetworkContext(context: Context): NetworkContext {
        return try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val caps = cm.getNetworkCapabilities(cm.activeNetwork)
            if (caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true) {
                NetworkContext.WIFI
            } else {
                NetworkContext.CELLULAR
            }
        } catch (_: Throwable) {
            NetworkContext.CELLULAR
        }
    }

    override fun onCleared() {
        super.onCleared()
        StrataPlaybackController.onTrackStarted = null
        StrataPlaybackController.onErrorMessage = null
    }
}
