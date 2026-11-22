package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.RecordVoiceOver
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.LearnerProfileDoc
import com.example.domain.FocusQueueItem
import com.example.domain.GenAmCatalog
import com.example.domain.PracticePhrase
import com.example.domain.TempoStep
import com.example.ui.components.ClarityReadinessRing
import com.example.ui.components.MasteryPill
import com.example.ui.components.StudioCard
import com.example.ui.theme.LocalStudioColors
import com.example.viewmodel.AppUpdateUiState
import com.example.viewmodel.DataLoadState

@Composable
fun StudioHomeScreen(
    displayName: String,
    profileState: DataLoadState<LearnerProfileDoc?>,
    readinessScore: Int,
    focusQueue: List<FocusQueueItem>,
    generatingDrill: Boolean,
    drillError: String?,
    updateState: AppUpdateUiState,
    onDownloadUpdateApk: (String) -> Unit,
    onLaunchShadowingForPhoneme: (String, TempoStep) -> Unit,
    onOpenMouthLabForPhoneme: (String) -> Unit,
    onSelectPhraseAndShadow: (PracticePhrase) -> Unit,
    onGenerateAdaptiveDrill: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val studioColors = LocalStudioColors.current
    val profile = (profileState as? DataLoadState.Success)?.data
    val firstName = displayName.trim().substringBefore(" ").ifBlank { "Speaker" }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 720.dp)
                .fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // Studio Masthead
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "GENERAL AMERICAN STUDIO",
                            style = MaterialTheme.typography.labelSmall,
                            color = colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Welcome back, $firstName",
                            style = MaterialTheme.typography.headlineLarge,
                            color = colorScheme.onBackground
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = colorScheme.primaryContainer
                    ) {
                        Text(
                            text = "GenAm",
                            style = MaterialTheme.typography.labelMedium,
                            color = colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            if (updateState is AppUpdateUiState.UpdateAvailable) {
                item {
                    StudioCard(containerColor = colorScheme.secondaryContainer.copy(alpha = 0.7f)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.SystemUpdate,
                                contentDescription = "Nova atualização disponível",
                                tint = colorScheme.secondary,
                                modifier = Modifier.size(24.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Nova versão ${updateState.info.tagName} disponível",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = colorScheme.onSecondaryContainer
                                )
                                Text(
                                    text = "Baixe o novo APK gerado no GitHub Actions.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colorScheme.onSecondaryContainer
                                )
                            }
                            Button(
                                onClick = { onDownloadUpdateApk(updateState.info.apkDownloadUrl) },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = colorScheme.secondary,
                                    contentColor = colorScheme.onSecondary
                                ),
                                modifier = Modifier.testTag("home_update_apk_button")
                            ) {
                                Text("Atualizar")
                            }
                        }
                    }
                }
            }

            if (profileState is DataLoadState.Error) {
                item {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = colorScheme.errorContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = profileState.message,
                            style = MaterialTheme.typography.bodySmall,
                            color = colorScheme.onErrorContainer,
                            modifier = Modifier.padding(14.dp)
                        )
                    }
                }
            }

            // Clarity Readiness & Studio Telemetry Card
            item {
                StudioCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ClarityReadinessRing(score = readinessScore)
                        Spacer(modifier = Modifier.width(20.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Acoustic Readiness",
                                style = MaterialTheme.typography.titleLarge,
                                color = colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if ((profile?.totalSessions ?: 0L) == 0L) {
                                    "Complete your first shadowing take to calibrate your GenAm profile."
                                } else {
                                    "Your adaptive queue updates after every AI phoneme evaluation."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                MiniTelemetryStat(
                                    label = "SESSIONS",
                                    value = "${profile?.totalSessions ?: 0}"
                                )
                                MiniTelemetryStat(
                                    label = "PACE",
                                    value = TempoStep.nearest(profile?.preferredStartSpeed ?: 0.80).label
                                )
                                MiniTelemetryStat(
                                    label = "STREAK",
                                    value = "${profile?.currentStreakDays ?: 1}d"
                                )
                            }
                        }
                    }
                }
            }

            // Personalized AI Adaptive Drill Generator
            item {
                StudioCard(containerColor = colorScheme.primaryContainer.copy(alpha = 0.45f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.AutoAwesome,
                            contentDescription = null,
                            tint = colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                        Text(
                            text = "Adaptive Feedback Loop",
                            style = MaterialTheme.typography.titleMedium,
                            color = colorScheme.onSurface
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Generate a fresh General American shadowing sentence targeting your lowest-mastery phonemes (${focusQueue.take(3).joinToString(" ") { it.phoneme.ipa }}).",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colorScheme.onSurfaceVariant
                    )
                    if (drillError != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = drillError,
                            style = MaterialTheme.typography.bodySmall,
                            color = colorScheme.error
                        )
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    Button(
                        onClick = onGenerateAdaptiveDrill,
                        enabled = !generatingDrill,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colorScheme.primary,
                            contentColor = colorScheme.onPrimary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .minimumInteractiveComponentSize()
                            .testTag("generate_adaptive_drill_button")
                    ) {
                        if (generatingDrill) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Crafting Custom GenAm Drill…")
                        } else {
                            Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Generate Personalized AI Drill")
                        }
                    }
                }
            }

            // Today's Focus Queue Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Today's Focus Queue",
                        style = MaterialTheme.typography.headlineSmall,
                        color = colorScheme.onBackground
                    )
                    Text(
                        text = "RANKED BY PRIORITY",
                        style = MaterialTheme.typography.labelSmall,
                        color = colorScheme.onSurfaceVariant
                    )
                }
            }

            items(focusQueue, key = { it.phoneme.id }) { item ->
                FocusPhonemeCard(
                    item = item,
                    onShadow = { onLaunchShadowingForPhoneme(item.phoneme.id, item.recommendedSpeed) },
                    onOpenMouth = { onOpenMouthLabForPhoneme(item.phoneme.id) }
                )
            }

            // Starter Shadowing Tracks
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Core GenAm Shadowing Packs",
                    style = MaterialTheme.typography.headlineSmall,
                    color = colorScheme.onBackground
                )
            }

            items(GenAmCatalog.starterPhrases.take(4), key = { it.id }) { phrase ->
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = colorScheme.surface,
                    border = BorderStroke(1.dp, studioColors.hairline),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelectPhraseAndShadow(phrase) }
                        .testTag("starter_pack_${phrase.id}")
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.GraphicEq,
                                contentDescription = null,
                                tint = colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = phrase.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = colorScheme.onSurface
                                )
                                Text(
                                    text = phrase.difficulty.uppercase(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = colorScheme.primary
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = phrase.text,
                                style = MaterialTheme.typography.bodySmall,
                                color = colorScheme.onSurfaceVariant,
                                maxLines = 2
                            )
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                            contentDescription = "Open ${phrase.title}",
                            tint = colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MiniTelemetryStat(label: String, value: String) {
    Column {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun FocusPhonemeCard(
    item: FocusQueueItem,
    onShadow: () -> Unit,
    onOpenMouth: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    StudioCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = item.phoneme.ipa,
                    style = MaterialTheme.typography.headlineMedium,
                    color = colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = item.phoneme.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = colorScheme.onSurface
                    )
                    MasteryPill(score = if (item.attemptCount == 0) 0 else item.masteryScore)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Example: ${item.phoneme.exampleWord} ${item.phoneme.exampleIpa}  •  Start at ${item.recommendedSpeed.label}",
                    style = MaterialTheme.typography.labelSmall,
                    color = colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = item.recentIssueNote,
            style = MaterialTheme.typography.bodySmall,
            color = colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(14.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            FilledTonalButton(
                onClick = onShadow,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .weight(1f)
                    .minimumInteractiveComponentSize()
                    .testTag("focus_shadow_${item.phoneme.id}")
            ) {
                Icon(Icons.Rounded.Mic, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Shadow Drill")
            }
            OutlinedButton(
                onClick = onOpenMouth,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .weight(1f)
                    .minimumInteractiveComponentSize()
                    .testTag("focus_mouth_${item.phoneme.id}")
            ) {
                Icon(Icons.Rounded.RecordVoiceOver, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Mouth Lab")
            }
        }
    }
}
