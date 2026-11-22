package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Compare
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.domain.GenAmCatalog
import com.example.domain.PhonemeCategory
import com.example.ui.components.MouthMechanicsCanvas
import com.example.ui.components.StudioCard
import com.example.ui.theme.LocalStudioColors
import com.example.viewmodel.MouthLabState

@Composable
fun MouthLabScreen(
    state: MouthLabState,
    onSelectPhoneme: (String) -> Unit,
    onToggleContrast: () -> Unit,
    onToggleLabels: () -> Unit,
    onToggleLoop: () -> Unit,
    onScrubPhase: (Float) -> Unit,
    onSpeakExample: (String) -> Unit,
    onPracticeSound: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val studioColors = LocalStudioColors.current
    var selectedCategory by remember { mutableStateOf<PhonemeCategory?>(null) }

    val visiblePhonemes = remember(selectedCategory) {
        if (selectedCategory == null) GenAmCatalog.phonemes
        else GenAmCatalog.phonemes.filter { it.category == selectedCategory }
    }
    val phoneme = state.selectedPhoneme
    val contrastPair = if (state.showContrastOverlay) {
        GenAmCatalog.resolveContrastPose(phoneme.contrastPhonemeId)
    } else {
        null
    }

    BoxWithConstraints(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        val wideLayout = maxWidth >= 820.dp

        LazyColumn(
            modifier = Modifier
                .widthIn(max = if (wideLayout) 1040.dp else 720.dp)
                .fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Column {
                    Text(
                        text = "2D MIDSAGITTAL VOCAL TRACT LAB",
                        style = MaterialTheme.typography.labelSmall,
                        color = colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Mouth Mechanics",
                        style = MaterialTheme.typography.headlineLarge,
                        color = colorScheme.onBackground
                    )
                }
            }

            // Category Filter Strip
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        FilterChip(
                            selected = selectedCategory == null,
                            onClick = { selectedCategory = null },
                            label = { Text("All GenAm (12)") }
                        )
                    }
                    items(PhonemeCategory.entries) { cat ->
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = { selectedCategory = cat },
                            label = { Text(cat.label) }
                        )
                    }
                }
            }

            // Phoneme Selector Strip
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(visiblePhonemes, key = { it.id }) { p ->
                        val active = p.id == phoneme.id
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = if (active) colorScheme.primary else colorScheme.surface,
                            border = BorderStroke(
                                1.dp,
                                if (active) colorScheme.primary else studioColors.hairline
                            ),
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .clickable { onSelectPhoneme(p.id) }
                                .testTag("mouth_phoneme_${p.id}")
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = p.ipa,
                                    style = MaterialTheme.typography.titleLarge,
                                    color = if (active) colorScheme.onPrimary else colorScheme.onSurface
                                )
                                Text(
                                    text = p.exampleWord,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (active) {
                                        colorScheme.onPrimary.copy(alpha = 0.85f)
                                    } else {
                                        colorScheme.onSurfaceVariant
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Main Visualizer + Coaching (side-by-side on wide screens, stacked on phones)
            item {
                if (wideLayout) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(18.dp)
                    ) {
                        Column(
                            modifier = Modifier.weight(1.1f),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            MouthVisualizerDeck(
                                state = state,
                                contrastPair = contrastPair,
                                onToggleContrast = onToggleContrast,
                                onToggleLabels = onToggleLabels,
                                onToggleLoop = onToggleLoop,
                                onScrubPhase = onScrubPhase,
                                onSpeakExample = { onSpeakExample(phoneme.exampleWord) }
                            )
                        }
                        Column(
                            modifier = Modifier.weight(0.9f),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            ArticulationGuideCard(
                                state = state,
                                onPracticeSound = { onPracticeSound(phoneme.id) }
                            )
                        }
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        MouthVisualizerDeck(
                            state = state,
                            contrastPair = contrastPair,
                            onToggleContrast = onToggleContrast,
                            onToggleLabels = onToggleLabels,
                            onToggleLoop = onToggleLoop,
                            onScrubPhase = onScrubPhase,
                            onSpeakExample = { onSpeakExample(phoneme.exampleWord) }
                        )
                        ArticulationGuideCard(
                            state = state,
                            onPracticeSound = { onPracticeSound(phoneme.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MouthVisualizerDeck(
    state: MouthLabState,
    contrastPair: Pair<String, com.example.domain.ArticulationPose>?,
    onToggleContrast: () -> Unit,
    onToggleLabels: () -> Unit,
    onToggleLoop: () -> Unit,
    onScrubPhase: (Float) -> Unit,
    onSpeakExample: () -> Unit
) {
    val phoneme = state.selectedPhoneme
    val colorScheme = MaterialTheme.colorScheme

    MouthMechanicsCanvas(
        phoneme = phoneme,
        contrastOverlay = contrastPair,
        showLabels = state.showAnatomicalLabels,
        loopingAnimation = state.loopingAnimation,
        manualPhase = state.manualPhase,
        canvasHeight = 290.dp
    )

    StudioCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${phoneme.ipa}  ${phoneme.name}",
                    style = MaterialTheme.typography.titleLarge,
                    color = colorScheme.onSurface
                )
                Text(
                    text = "Anchor word: ${phoneme.exampleWord} ${phoneme.exampleIpa}",
                    style = MaterialTheme.typography.labelMedium,
                    color = colorScheme.primary
                )
            }
            FilledTonalButton(
                onClick = onSpeakExample,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag("mouth_hear_example_button")
            ) {
                Icon(Icons.AutoMirrored.Rounded.VolumeUp, contentDescription = "Hear example word", modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Hear")
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Slow-Mo Articulation Phase Scrubber
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                onClick = onToggleLoop,
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                modifier = Modifier.testTag("mouth_loop_toggle")
            ) {
                Icon(
                    imageVector = if (state.loopingAnimation) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    contentDescription = if (state.loopingAnimation) "Pause animation" else "Play animation",
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(if (state.loopingAnimation) "Loop" else "Scrub")
            }

            Column(modifier = Modifier.weight(1f)) {
                Slider(
                    value = if (state.loopingAnimation) 0.55f else state.manualPhase,
                    onValueChange = onScrubPhase,
                    valueRange = 0f..1f,
                    modifier = Modifier.testTag("mouth_phase_slider")
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Rest", style = MaterialTheme.typography.labelSmall, color = colorScheme.onSurfaceVariant)
                    Text("Contact Peak", style = MaterialTheme.typography.labelSmall, color = colorScheme.secondary)
                    Text("Release", style = MaterialTheme.typography.labelSmall, color = colorScheme.onSurfaceVariant)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Contrast & Labels Toggles
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (phoneme.contrastLabel != null) {
                FilledTonalButton(
                    onClick = onToggleContrast,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1.3f)
                        .minimumInteractiveComponentSize()
                        .testTag("mouth_contrast_toggle")
                ) {
                    Icon(Icons.Rounded.Compare, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (state.showContrastOverlay) "Hide Contrast" else phoneme.contrastLabel,
                        maxLines = 1
                    )
                }
            }
            OutlinedButton(
                onClick = onToggleLabels,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .weight(0.8f)
                    .minimumInteractiveComponentSize()
                    .testTag("mouth_labels_toggle")
            ) {
                Icon(Icons.Rounded.Visibility, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(if (state.showAnatomicalLabels) "Labels On" else "Labels Off")
            }
        }
    }
}

@Composable
private fun ArticulationGuideCard(
    state: MouthLabState,
    onPracticeSound: () -> Unit
) {
    val phoneme = state.selectedPhoneme
    val colorScheme = MaterialTheme.colorScheme

    StudioCard {
        Text(
            text = "BIOMECHANICS BREAKDOWN",
            style = MaterialTheme.typography.labelSmall,
            color = colorScheme.primary
        )
        Spacer(modifier = Modifier.height(12.dp))

        GuideBlock(
            badge = "1. TONGUE & JAW PLACEMENT",
            body = phoneme.placementCue,
            tint = colorScheme.surfaceVariant
        )
        Spacer(modifier = Modifier.height(10.dp))
        GuideBlock(
            badge = "2. AIRFLOW & VOCAL FOLDS",
            body = phoneme.airflowCue,
            tint = colorScheme.primaryContainer.copy(alpha = 0.45f)
        )
        Spacer(modifier = Modifier.height(10.dp))
        GuideBlock(
            badge = "3. COMMON PITFALL TO AVOID",
            body = phoneme.commonMistake,
            tint = colorScheme.secondaryContainer.copy(alpha = 0.5f)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onPracticeSound,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("practice_sound_from_lab_button")
        ) {
            Icon(Icons.Rounded.Mic, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Shadow ${phoneme.ipa} in Practice Studio")
        }
    }
}

@Composable
private fun GuideBlock(
    badge: String,
    body: String,
    tint: androidx.compose.ui.graphics.Color
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(tint)
            .padding(12.dp)
    ) {
        Text(
            text = badge,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
