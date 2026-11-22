package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.RecordVoiceOver
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.R
import com.example.domain.AcousticEvaluation
import com.example.domain.ClarityTier
import com.example.domain.GenAmCatalog
import com.example.domain.PracticePhrase
import com.example.domain.TempoStep
import com.example.ui.components.LiveOscillogramCanvas
import com.example.ui.components.MasteryPill
import com.example.ui.components.MouthMechanicsCanvas
import com.example.ui.components.ScoreMetricTile
import com.example.ui.components.SpeedStepperDeck
import com.example.ui.components.StudioCard
import com.example.ui.theme.LocalStudioColors
import com.example.viewmodel.EvaluationUiState
import com.example.viewmodel.ShadowingStudioState
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ShadowingScreen(
    state: ShadowingStudioState,
    isSpeakingReference: Boolean,
    isRecording: Boolean,
    isPlayingRecording: Boolean,
    liveAmplitudes: List<Float>,
    recordedDurationSec: Float,
    onSelectPhrase: (PracticePhrase) -> Unit,
    onSelectSpeed: (TempoStep) -> Unit,
    onPlayReference: () -> Unit,
    onSpeakWord: (String) -> Unit,
    onToggleRecording: () -> Unit,
    onToggleReplay: () -> Unit,
    onEvaluateTake: () -> Unit,
    onOpenInlineMouth: (String?) -> Unit,
    onOpenFullMouthLab: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val colorScheme = MaterialTheme.colorScheme
    val studioColors = LocalStudioColors.current

    var hasMicPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    var micPermissionDenied by remember { mutableStateOf(false) }

    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasMicPermission = granted
        micPermissionDenied = !granted
        if (granted) {
            onToggleRecording()
        }
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 720.dp)
                .fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            item {
                Column {
                    Text(
                        text = "PROGRESSIVE SHADOWING ENGINE",
                        style = MaterialTheme.typography.labelSmall,
                        color = colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Shadow & Evaluate",
                        style = MaterialTheme.typography.headlineLarge,
                        color = colorScheme.onBackground
                    )
                }
            }

            // Phrase Selector Row
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(state.availablePhrases, key = { it.id }) { phrase ->
                        FilterChip(
                            selected = phrase.id == state.activePhrase.id,
                            onClick = { onSelectPhrase(phrase) },
                            label = {
                                Text(
                                    text = if (phrase.generatedByAi) "★ ${phrase.title}" else phrase.title,
                                    style = MaterialTheme.typography.labelMedium
                                )
                            },
                            modifier = Modifier.testTag("phrase_chip_${phrase.id}")
                        )
                    }
                }
            }

            // Active Script & Target Phonemes Card
            item {
                StudioCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = state.activePhrase.title.uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            color = colorScheme.primary
                        )
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (state.activePhrase.generatedByAi) {
                                colorScheme.tertiaryContainer
                            } else {
                                colorScheme.surfaceVariant
                            }
                        ) {
                            Text(
                                text = if (state.activePhrase.generatedByAi) "AI TARGETED" else state.activePhrase.difficulty,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (state.activePhrase.generatedByAi) {
                                    colorScheme.onTertiaryContainer
                                } else {
                                    colorScheme.onSurfaceVariant
                                },
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = state.activePhrase.text,
                        style = MaterialTheme.typography.headlineMedium,
                        color = colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = state.activePhrase.ipa,
                        style = MaterialTheme.typography.labelMedium,
                        color = colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = colorScheme.surfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "STRESS & RHYTHM CUE",
                                style = MaterialTheme.typography.labelSmall,
                                color = colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = state.activePhrase.stressGuide,
                                style = MaterialTheme.typography.bodyMedium,
                                color = colorScheme.onSurface
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "TAP A TARGET SOUND FOR 2D MOUTH MECHANICS",
                        style = MaterialTheme.typography.labelSmall,
                        color = colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        state.activePhrase.targetPhonemeIds.mapNotNull { GenAmCatalog.phonemeById[it] }
                            .forEach { phoneme ->
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = colorScheme.secondaryContainer.copy(alpha = 0.65f),
                                    border = BorderStroke(1.dp, colorScheme.secondary.copy(alpha = 0.25f)),
                                    modifier = Modifier
                                        .minimumInteractiveComponentSize()
                                        .clickable { onOpenInlineMouth(phoneme.id) }
                                        .testTag("inline_phoneme_${phoneme.id}")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = phoneme.ipa,
                                            style = MaterialTheme.typography.titleMedium,
                                            color = colorScheme.onSecondaryContainer
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = phoneme.exampleWord,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = colorScheme.onSecondaryContainer
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Icon(
                                            imageVector = Icons.Rounded.RecordVoiceOver,
                                            contentDescription = "Inspect ${phoneme.name} mouth mechanics",
                                            tint = colorScheme.secondary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                    }
                }
            }

            // Progressive Speed Stepper & Acoustic Studio Deck
            item {
                StudioCard {
                    SpeedStepperDeck(
                        selectedSpeed = state.selectedSpeed,
                        onSelectSpeed = onSelectSpeed
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = when {
                                isRecording -> "RECORDING • ${String.format(Locale.US, "%.1fs", recordedDurationSec)}"
                                isSpeakingReference -> "PLAYING GENAM REFERENCE (${state.selectedSpeed.label})"
                                state.recordedWav != null -> "TAKE READY • ${String.format(Locale.US, "%.1fs", recordedDurationSec)}"
                                else -> "STUDIO SIGNAL MONITOR"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isRecording) colorScheme.secondary else colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "16kHz PCM",
                            style = MaterialTheme.typography.labelSmall,
                            color = colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    LiveOscillogramCanvas(
                        amplitudes = liveAmplitudes,
                        isRecording = isRecording,
                        isSpeakingReference = isSpeakingReference
                    )

                    if (micPermissionDenied && !hasMicPermission) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = colorScheme.tertiaryContainer,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = stringResource(R.string.mic_permission_rationale),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colorScheme.onTertiaryContainer
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedButton(
                                    onClick = { micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO) },
                                    modifier = Modifier.testTag("grant_mic_button")
                                ) {
                                    Text(stringResource(R.string.grant_mic_permission))
                                }
                            }
                        }
                    }

                    if (state.audioErrorMessage != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = state.audioErrorMessage,
                            style = MaterialTheme.typography.bodySmall,
                            color = colorScheme.error
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Primary Control Deck: Listen + Record + Replay
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        FilledTonalButton(
                            onClick = onPlayReference,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp)
                                .testTag("listen_reference_button")
                        ) {
                            Icon(
                                imageVector = if (isSpeakingReference) Icons.Rounded.Stop else Icons.AutoMirrored.Rounded.VolumeUp,
                                contentDescription = "Listen to reference voice",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (isSpeakingReference) "Stop" else "Listen")
                        }

                        Button(
                            onClick = {
                                if (isRecording) {
                                    onToggleRecording()
                                } else if (hasMicPermission) {
                                    onToggleRecording()
                                } else {
                                    micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                }
                            },
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isRecording) colorScheme.secondary else colorScheme.primary,
                                contentColor = if (isRecording) colorScheme.onSecondary else colorScheme.onPrimary
                            ),
                            modifier = Modifier
                                .weight(1.25f)
                                .height(52.dp)
                                .testTag("record_shadow_button")
                        ) {
                            Icon(
                                imageVector = if (isRecording) Icons.Rounded.Stop else Icons.Rounded.Mic,
                                contentDescription = if (isRecording) "Stop recording" else "Start recording",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (isRecording) "Stop Take" else "Shadow Mic")
                        }
                    }

                    if (state.recordedWav != null && !isRecording) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = onToggleReplay,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(50.dp)
                                    .testTag("replay_recording_button")
                            ) {
                                Icon(
                                    imageVector = if (isPlayingRecording) Icons.Rounded.Stop else Icons.Rounded.PlayArrow,
                                    contentDescription = "Replay your recording",
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (isPlayingRecording) "Stop" else "Hear My Take")
                            }

                            Button(
                                onClick = onEvaluateTake,
                                enabled = state.evaluationState !is EvaluationUiState.Evaluating,
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = colorScheme.secondary,
                                    contentColor = colorScheme.onSecondary
                                ),
                                modifier = Modifier
                                    .weight(1.3f)
                                    .height(50.dp)
                                    .testTag("analyze_ai_button")
                            ) {
                                if (state.evaluationState is EvaluationUiState.Evaluating) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        color = colorScheme.onSecondary,
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Analyzing…")
                                } else {
                                    Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Analyze Clarity")
                                }
                            }
                        }
                    }
                }
            }

            // AI Acoustic Evaluation Result Area
            item {
                when (val evalState = state.evaluationState) {
                    is EvaluationUiState.Idle -> {
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = BorderStroke(1.dp, studioColors.hairline),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.GraphicEq,
                                    contentDescription = null,
                                    tint = colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "Listen at ${state.selectedSpeed.label}, tap Shadow Mic to record your voice, then run Analyze Clarity for phoneme-level AI coaching.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    is EvaluationUiState.Evaluating -> {
                        StudioCard {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(28.dp),
                                    color = colorScheme.primary,
                                    strokeWidth = 3.dp
                                )
                                Column {
                                    Text(
                                        text = "Running Multimodal Acoustic Analysis…",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Inspecting General American vowels, rhotics, stress cadence, and consonant articulation.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    is EvaluationUiState.Error -> {
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = colorScheme.errorContainer,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "Evaluation Could Not Complete",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = colorScheme.onErrorContainer
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = evalState.message,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colorScheme.onErrorContainer
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                OutlinedButton(
                                    onClick = onEvaluateTake,
                                    modifier = Modifier.testTag("retry_evaluation_button")
                                ) {
                                    Text("Retry Acoustic Analysis")
                                }
                            }
                        }
                    }

                    is EvaluationUiState.Success -> {
                        AcousticScorecardSection(
                            evaluation = evalState.evaluation,
                            currentSpeed = state.selectedSpeed,
                            savedBanner = state.lastSavedBanner,
                            onStepUpSpeed = { next -> onSelectSpeed(next) },
                            onWordTap = { word, phonemeId ->
                                onSpeakWord(word)
                                if (phonemeId != null) onOpenInlineMouth(phonemeId)
                            },
                            onInspectPhoneme = { pid -> onOpenInlineMouth(pid) }
                        )
                    }
                }
            }
        }
    }

    // Inline 2D Mouth Mechanics Bottom Sheet
    val inlinePhoneme = state.inlinePhonemeId?.let { GenAmCatalog.phonemeById[it] }
    if (inlinePhoneme != null) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { onOpenInlineMouth(null) },
            sheetState = sheetState,
            containerColor = colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "${inlinePhoneme.ipa}  ${inlinePhoneme.name}",
                            style = MaterialTheme.typography.headlineSmall,
                            color = colorScheme.onBackground
                        )
                        Text(
                            text = "Example: ${inlinePhoneme.exampleWord} ${inlinePhoneme.exampleIpa}",
                            style = MaterialTheme.typography.labelMedium,
                            color = colorScheme.primary
                        )
                    }
                    FilledTonalButton(onClick = { onSpeakWord(inlinePhoneme.exampleWord) }) {
                        Icon(Icons.AutoMirrored.Rounded.VolumeUp, contentDescription = "Hear word", modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Hear")
                    }
                }

                MouthMechanicsCanvas(
                    phoneme = inlinePhoneme,
                    contrastOverlay = null,
                    showLabels = true,
                    loopingAnimation = true,
                    manualPhase = 0.72f,
                    canvasHeight = 230.dp
                )

                Text(
                    text = inlinePhoneme.placementCue,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colorScheme.onBackground
                )
                Text(
                    text = "Airflow: ${inlinePhoneme.airflowCue}",
                    style = MaterialTheme.typography.bodySmall,
                    color = colorScheme.onSurfaceVariant
                )

                Button(
                    onClick = {
                        onOpenInlineMouth(null)
                        onOpenFullMouthLab(inlinePhoneme.id)
                    },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Open Full Mouth Mechanics Lab & Contrast Overlay")
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AcousticScorecardSection(
    evaluation: AcousticEvaluation,
    currentSpeed: TempoStep,
    savedBanner: String?,
    onStepUpSpeed: (TempoStep) -> Unit,
    onWordTap: (String, String?) -> Unit,
    onInspectPhoneme: (String) -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val studioColors = LocalStudioColors.current
    val nextSpeed = currentSpeed.nextStep()

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        StudioCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "AI ACOUSTIC SCORECARD",
                        style = MaterialTheme.typography.labelSmall,
                        color = colorScheme.primary
                    )
                    Text(
                        text = "Clarity Index: ${evaluation.overallScore}/100",
                        style = MaterialTheme.typography.headlineMedium,
                        color = colorScheme.onSurface
                    )
                }
                MasteryPill(score = evaluation.overallScore)
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ScoreMetricTile("PHONEMES", evaluation.phonemeAccuracy, Modifier.weight(1f))
                ScoreMetricTile("RHYTHM", evaluation.rhythmScore, Modifier.weight(1f))
                ScoreMetricTile("FLOW", evaluation.flowScore, Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = evaluation.coachingSummary,
                style = MaterialTheme.typography.bodyMedium,
                color = colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(8.dp))

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = colorScheme.primaryContainer.copy(alpha = 0.55f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "NEXT STEP",
                        style = MaterialTheme.typography.labelSmall,
                        color = colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = evaluation.nextStepRecommendation,
                        style = MaterialTheme.typography.bodySmall,
                        color = colorScheme.onPrimaryContainer
                    )
                    if (evaluation.shouldStepUpSpeed && nextSpeed != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = { onStepUpSpeed(nextSpeed) },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("step_up_speed_button")
                        ) {
                            Icon(Icons.Rounded.TrendingUp, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Step Up to ${nextSpeed.label} (${nextSpeed.caption})")
                        }
                    }
                }
            }

            if (savedBanner != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.CheckCircle,
                        contentDescription = null,
                        tint = studioColors.masteryHigh,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = savedBanner,
                        style = MaterialTheme.typography.bodySmall,
                        color = studioColors.masteryHigh
                    )
                }
            }
        }

        // Word-by-Word Breakdown
        if (evaluation.wordBreakdown.isNotEmpty()) {
            StudioCard {
                Text(
                    text = "WORD-BY-WORD CLARITY",
                    style = MaterialTheme.typography.labelSmall,
                    color = colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(10.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    evaluation.wordBreakdown.forEach { item ->
                        val (bg, fg) = when (item.status) {
                            ClarityTier.CLEAR -> studioColors.masteryHighContainer to studioColors.masteryHigh
                            ClarityTier.CLOSE -> studioColors.masteryMidContainer to studioColors.masteryMid
                            ClarityTier.ADJUST -> studioColors.masteryLowContainer to studioColors.masteryLow
                        }
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = bg,
                            modifier = Modifier.clickable { onWordTap(item.word, item.phonemeId) }
                        ) {
                            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                                Text(
                                    text = item.word,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = fg
                                )
                                if (item.expectedIpa.isNotBlank()) {
                                    Text(
                                        text = item.expectedIpa,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = fg.copy(alpha = 0.85f)
                                    )
                                }
                                if (item.detectedIssue.isNotBlank()) {
                                    Text(
                                        text = item.detectedIssue,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Physical Phoneme Articulation Coaching
        if (evaluation.phonemeResults.isNotEmpty()) {
            StudioCard {
                Text(
                    text = "PHONEME ARTICULATION COACHING",
                    style = MaterialTheme.typography.labelSmall,
                    color = colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(10.dp))
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    evaluation.phonemeResults.forEach { pRes ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(colorScheme.surfaceVariant.copy(alpha = 0.6f))
                                .clickable { onInspectPhoneme(pRes.phonemeId) }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = pRes.ipa,
                                style = MaterialTheme.typography.headlineSmall,
                                color = colorScheme.primary,
                                modifier = Modifier.width(46.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = GenAmCatalog.phonemeById[pRes.phonemeId]?.name ?: pRes.phonemeId,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = colorScheme.onSurface
                                    )
                                    MasteryPill(score = pRes.score)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = pRes.physicalTip,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
