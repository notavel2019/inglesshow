package com.example.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.AiAcousticEngine
import com.example.data.AppUpdateChecker
import com.example.data.AppUpdateInfo
import com.example.data.AudioPipeline
import com.example.data.LearnerProfileDoc
import com.example.data.PhonemeMasteryDoc
import com.example.data.PracticeSessionDoc
import com.example.data.VoxFormRepository
import com.example.domain.AcousticEvaluation
import com.example.domain.FocusQueueItem
import com.example.domain.GenAmCatalog
import com.example.domain.GenAmPhoneme
import com.example.domain.MasteryFeedbackEngine
import com.example.domain.PhonemeMasterySnapshot
import com.example.domain.PracticePhrase
import com.example.domain.TempoStep
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface DataLoadState<out T> {
    data object Loading : DataLoadState<Nothing>
    data class Success<T>(val data: T) : DataLoadState<T>
    data class Error(val message: String) : DataLoadState<Nothing>
}

sealed interface EvaluationUiState {
    data object Idle : EvaluationUiState
    data object Evaluating : EvaluationUiState
    data class Success(val evaluation: AcousticEvaluation) : EvaluationUiState
    data class Error(val message: String) : EvaluationUiState
}

sealed interface AppUpdateUiState {
    data object Idle : AppUpdateUiState
    data object Checking : AppUpdateUiState
    data class UpToDate(val info: AppUpdateInfo) : AppUpdateUiState
    data class UpdateAvailable(val info: AppUpdateInfo) : AppUpdateUiState
    data class Error(val message: String) : AppUpdateUiState
}

data class ShadowingStudioState(
    val activePhrase: PracticePhrase = GenAmCatalog.starterPhrases.first(),
    val availablePhrases: List<PracticePhrase> = GenAmCatalog.starterPhrases,
    val selectedSpeed: TempoStep = TempoStep.GUIDED,
    val recordedWav: ByteArray? = null,
    val audioErrorMessage: String? = null,
    val evaluationState: EvaluationUiState = EvaluationUiState.Idle,
    val inlinePhonemeId: String? = null,
    val generatingDrill: Boolean = false,
    val drillErrorMessage: String? = null,
    val lastSavedBanner: String? = null
)

data class MouthLabState(
    val selectedPhoneme: GenAmPhoneme = GenAmCatalog.phonemes.first(),
    val showContrastOverlay: Boolean = false,
    val showAnatomicalLabels: Boolean = true,
    val loopingAnimation: Boolean = true,
    val manualPhase: Float = 0.72f
)

class StudioViewModel(
    private val context: Context,
    private val userId: String,
    private val userDisplayName: String,
    private val repository: VoxFormRepository = VoxFormRepository(context),
    private val aiEngine: AiAcousticEngine = AiAcousticEngine()
) : ViewModel() {

    private val audioPipeline = AudioPipeline(context, viewModelScope)
    private val updateChecker = AppUpdateChecker(context)

    private val _configuredGithubRepo = MutableStateFlow(updateChecker.getConfiguredRepo())
    val configuredGithubRepo: StateFlow<String> = _configuredGithubRepo.asStateFlow()

    private val _updateState = MutableStateFlow<AppUpdateUiState>(AppUpdateUiState.Idle)
    val updateState: StateFlow<AppUpdateUiState> = _updateState.asStateFlow()

    val isSpeakingReference: StateFlow<Boolean> = audioPipeline.isSpeakingReference
    val isRecording: StateFlow<Boolean> = audioPipeline.isRecording
    val isPlayingRecording: StateFlow<Boolean> = audioPipeline.isPlayingRecording
    val liveAmplitudes: StateFlow<List<Float>> = audioPipeline.liveAmplitudes
    val recordedDurationSec: StateFlow<Float> = audioPipeline.recordedDurationSec

    val profileState: StateFlow<DataLoadState<LearnerProfileDoc?>> = repository
        .observeLearnerProfile()
        .map<LearnerProfileDoc?, DataLoadState<LearnerProfileDoc?>> { DataLoadState.Success(it) }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val root = firestoreError ?: error
            emit(DataLoadState.Error(root.localizedMessage ?: "Failed to sync learner profile."))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = DataLoadState.Loading
        )

    val masteryState: StateFlow<DataLoadState<List<PhonemeMasteryDoc>>> = repository
        .observePhonemeMastery()
        .map<List<PhonemeMasteryDoc>, DataLoadState<List<PhonemeMasteryDoc>>> { DataLoadState.Success(it) }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val root = firestoreError ?: error
            emit(DataLoadState.Error(root.localizedMessage ?: "Failed to sync phoneme mastery."))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = DataLoadState.Loading
        )

    val sessionsState: StateFlow<DataLoadState<List<PracticeSessionDoc>>> = repository
        .observePracticeSessions()
        .map<List<PracticeSessionDoc>, DataLoadState<List<PracticeSessionDoc>>> { DataLoadState.Success(it) }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val root = firestoreError ?: error
            emit(DataLoadState.Error(root.localizedMessage ?: "Failed to sync practice history."))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = DataLoadState.Loading
        )

    val focusQueue: StateFlow<List<FocusQueueItem>> = masteryState
        .map { state ->
            val docs = (state as? DataLoadState.Success)?.data.orEmpty()
            val snapshots = docs.associate { doc ->
                doc.phonemeId to PhonemeMasterySnapshot(
                    phonemeId = doc.phonemeId,
                    masteryScore = doc.masteryScore.toInt(),
                    attemptCount = doc.attemptCount.toInt(),
                    recommendedSpeed = doc.recommendedSpeed,
                    recentIssueNote = doc.recentIssueNote.orEmpty(),
                    lastPracticedMs = doc.lastPracticedAt?.toDate()?.time
                )
            }
            MasteryFeedbackEngine.buildFocusQueue(snapshots, limit = 5)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = MasteryFeedbackEngine.buildFocusQueue(emptyMap(), limit = 5)
        )

    val readinessScore: StateFlow<Int> = combine(profileState, masteryState) { pState, mState ->
        val masteryDocs = (mState as? DataLoadState.Success)?.data.orEmpty()
        if (masteryDocs.isNotEmpty()) {
            MasteryFeedbackEngine.computeOverallReadiness(
                masteryDocs.associate { it.phonemeId to it.masteryScore.toInt() }
            )
        } else {
            (pState as? DataLoadState.Success)?.data?.overallClarityScore?.toInt() ?: 0
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = 0
    )

    private val _shadowingState = MutableStateFlow(ShadowingStudioState())
    val shadowingState: StateFlow<ShadowingStudioState> = _shadowingState.asStateFlow()

    private val _mouthLabState = MutableStateFlow(MouthLabState())
    val mouthLabState: StateFlow<MouthLabState> = _mouthLabState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.ensureLearnerProfile(userDisplayName.ifBlank { "Studio Learner" })
        }
        if (_configuredGithubRepo.value.isNotBlank()) {
            checkForAppUpdate()
        }
    }

    fun saveGithubRepoAndCheck(rawRepo: String) {
        updateChecker.saveConfiguredRepo(rawRepo)
            .onSuccess { cleanSlug ->
                _configuredGithubRepo.value = cleanSlug
                checkForAppUpdate()
            }
            .onFailure { err ->
                _updateState.value = AppUpdateUiState.Error(
                    err.message ?: "Repositório inválido."
                )
            }
    }

    fun checkForAppUpdate() {
        val repo = _configuredGithubRepo.value
        if (repo.isBlank()) {
            _updateState.value = AppUpdateUiState.Error(
                "Informe seu repositório GitHub (usuario/repositorio) para buscar o APK."
            )
            return
        }
        viewModelScope.launch {
            _updateState.value = AppUpdateUiState.Checking
            val result = updateChecker.checkLatestRelease(repo)
            result.onSuccess { info ->
                _updateState.value = if (info.updateAvailable) {
                    AppUpdateUiState.UpdateAvailable(info)
                } else {
                    AppUpdateUiState.UpToDate(info)
                }
            }.onFailure { err ->
                _updateState.value = AppUpdateUiState.Error(
                    err.message ?: "Não foi possível verificar atualizações."
                )
            }
        }
    }

    fun downloadApk(targetContext: Context, url: String) {
        val opened = updateChecker.openApkDownload(targetContext, url)
        if (!opened) {
            _updateState.value = AppUpdateUiState.Error(
                "Não foi possível abrir o link de download do APK."
            )
        }
    }

    fun selectPhrase(phrase: PracticePhrase) {
        audioPipeline.stopReference()
        audioPipeline.stopUserPlayback()
        _shadowingState.update {
            it.copy(
                activePhrase = phrase,
                recordedWav = null,
                audioErrorMessage = null,
                evaluationState = EvaluationUiState.Idle,
                lastSavedBanner = null
            )
        }
    }

    fun launchDrillForPhoneme(phonemeId: String, recommendedSpeed: TempoStep? = null) {
        val phrase = MasteryFeedbackEngine.findBestPhraseForPhoneme(
            phonemeId = phonemeId,
            availablePhrases = _shadowingState.value.availablePhrases
        )
        selectPhrase(phrase)
        if (recommendedSpeed != null) {
            selectSpeed(recommendedSpeed)
        }
    }

    fun selectSpeed(step: TempoStep) {
        _shadowingState.update { it.copy(selectedSpeed = step) }
        viewModelScope.launch {
            repository.updatePreferredSpeed(step)
        }
    }

    fun playOrStopReference() {
        if (audioPipeline.isSpeakingReference.value) {
            audioPipeline.stopReference()
        } else {
            val current = _shadowingState.value
            audioPipeline.speakReference(current.activePhrase.text, current.selectedSpeed.multiplier)
        }
    }

    fun speakSingleWord(word: String) {
        audioPipeline.speakReference(word, 0.80f)
    }

    fun toggleRecording() {
        if (audioPipeline.isRecording.value) {
            viewModelScope.launch {
                val wavResult = audioPipeline.stopRecording()
                wavResult.onSuccess { wavBytes ->
                    _shadowingState.update {
                        it.copy(
                            recordedWav = wavBytes,
                            audioErrorMessage = null,
                            evaluationState = EvaluationUiState.Idle
                        )
                    }
                }.onFailure { err ->
                    _shadowingState.update {
                        it.copy(
                            recordedWav = null,
                            audioErrorMessage = err.message ?: "Recording failed."
                        )
                    }
                }
            }
        } else {
            _shadowingState.update {
                it.copy(
                    recordedWav = null,
                    audioErrorMessage = null,
                    evaluationState = EvaluationUiState.Idle,
                    lastSavedBanner = null
                )
            }
            audioPipeline.startRecording(
                onMaxDurationReached = { wavBytes ->
                    _shadowingState.update {
                        it.copy(
                            recordedWav = wavBytes,
                            audioErrorMessage = null
                        )
                    }
                },
                onError = { msg ->
                    _shadowingState.update { it.copy(audioErrorMessage = msg) }
                }
            )
        }
    }

    fun toggleReplayRecording() {
        val wav = _shadowingState.value.recordedWav ?: return
        if (audioPipeline.isPlayingRecording.value) {
            audioPipeline.stopUserPlayback()
        } else {
            audioPipeline.playUserRecording(wav)
        }
    }

    fun evaluateCurrentTake() {
        val current = _shadowingState.value
        val wav = current.recordedWav
        if (wav == null || wav.isEmpty()) {
            _shadowingState.update {
                it.copy(audioErrorMessage = "Record your shadowing take before running AI evaluation.")
            }
            return
        }

        viewModelScope.launch {
            _shadowingState.update {
                it.copy(
                    evaluationState = EvaluationUiState.Evaluating,
                    audioErrorMessage = null,
                    lastSavedBanner = null
                )
            }
            val result = aiEngine.evaluatePronunciation(
                wavBytes = wav,
                phrase = current.activePhrase,
                speed = current.selectedSpeed
            )
            result.onSuccess { evaluation ->
                _shadowingState.update {
                    it.copy(evaluationState = EvaluationUiState.Success(evaluation))
                }
                val existingMasteryMap = ((masteryState.value as? DataLoadState.Success)?.data.orEmpty())
                    .associateBy { it.phonemeId }
                val currentProfile = (profileState.value as? DataLoadState.Success)?.data
                val saveResult = repository.recordEvaluation(
                    phrase = current.activePhrase,
                    speed = current.selectedSpeed,
                    evaluation = evaluation,
                    existingMastery = existingMasteryMap,
                    currentProfile = currentProfile
                )
                saveResult.onSuccess {
                    _shadowingState.update {
                        it.copy(lastSavedBanner = "Saved to your cloud mastery profile.")
                    }
                }.onFailure { err ->
                    _shadowingState.update {
                        it.copy(lastSavedBanner = "Evaluated locally; cloud sync retry needed: ${err.localizedMessage ?: "error"}")
                    }
                }
            }.onFailure { err ->
                _shadowingState.update {
                    it.copy(
                        evaluationState = EvaluationUiState.Error(
                            err.localizedMessage ?: "Acoustic evaluation failed."
                        )
                    )
                }
            }
        }
    }

    fun generateAdaptiveDrill(onReady: () -> Unit = {}) {
        viewModelScope.launch {
            _shadowingState.update {
                it.copy(generatingDrill = true, drillErrorMessage = null)
            }
            val topFocus = focusQueue.value.take(3)
            val targetPhonemes = topFocus.map { it.phoneme }
            val notes = topFocus.map { it.recentIssueNote }
            val result = aiEngine.generatePersonalizedDrill(targetPhonemes, notes)
            result.onSuccess { generatedPhrase ->
                _shadowingState.update { state ->
                    state.copy(
                        generatingDrill = false,
                        availablePhrases = listOf(generatedPhrase) + state.availablePhrases,
                        activePhrase = generatedPhrase,
                        recordedWav = null,
                        evaluationState = EvaluationUiState.Idle,
                        lastSavedBanner = "Custom AI drill generated for your focus sounds."
                    )
                }
                onReady()
            }.onFailure { err ->
                _shadowingState.update {
                    it.copy(
                        generatingDrill = false,
                        drillErrorMessage = err.localizedMessage ?: "Could not generate personalized drill."
                    )
                }
            }
        }
    }

    fun openInlineVisualizer(phonemeId: String?) {
        _shadowingState.update { it.copy(inlinePhonemeId = phonemeId) }
    }

    fun selectMouthLabPhoneme(phonemeId: String) {
        val match = GenAmCatalog.phonemeById[phonemeId] ?: return
        _mouthLabState.update {
            it.copy(
                selectedPhoneme = match,
                showContrastOverlay = false
            )
        }
    }

    fun toggleContrastOverlay() {
        _mouthLabState.update { it.copy(showContrastOverlay = !it.showContrastOverlay) }
    }

    fun toggleAnatomicalLabels() {
        _mouthLabState.update { it.copy(showAnatomicalLabels = !it.showAnatomicalLabels) }
    }

    fun toggleLoopingAnimation() {
        _mouthLabState.update { it.copy(loopingAnimation = !it.loopingAnimation) }
    }

    fun updateManualPhase(phase: Float) {
        _mouthLabState.update {
            it.copy(
                loopingAnimation = false,
                manualPhase = phase.coerceIn(0f, 1f)
            )
        }
    }

    fun deleteSession(sessionId: String) {
        viewModelScope.launch {
            repository.deleteSession(sessionId)
        }
    }

    override fun onCleared() {
        super.onCleared()
        audioPipeline.release()
    }

    class Factory(
        private val context: Context,
        private val userId: String,
        private val displayName: String
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return StudioViewModel(
                context = context.applicationContext,
                userId = userId,
                userDisplayName = displayName
            ) as T
        }
    }
}
