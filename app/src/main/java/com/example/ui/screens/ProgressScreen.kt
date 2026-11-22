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
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.BuildConfig
import com.example.R
import com.example.data.PhonemeMasteryDoc
import com.example.data.PracticeSessionDoc
import com.example.domain.GenAmCatalog
import com.example.domain.TempoStep
import com.example.ui.components.MasteryPill
import com.example.ui.components.StudioCard
import com.example.ui.theme.LocalStudioColors
import com.example.viewmodel.AppUpdateUiState
import com.example.viewmodel.DataLoadState

@Composable
fun ProgressScreen(
    displayName: String,
    email: String?,
    masteryState: DataLoadState<List<PhonemeMasteryDoc>>,
    sessionsState: DataLoadState<List<PracticeSessionDoc>>,
    generatingDrill: Boolean,
    updateState: AppUpdateUiState,
    configuredGithubRepo: String,
    onCheckForUpdate: () -> Unit,
    onSaveGithubRepo: (String) -> Unit,
    onDownloadApk: (String) -> Unit,
    onSelectPhonemeForLab: (String) -> Unit,
    onGenerateAdaptiveDrill: () -> Unit,
    onDeleteSession: (String) -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val studioColors = LocalStudioColors.current
    val masteryDocs = (masteryState as? DataLoadState.Success)?.data.orEmpty().associateBy { it.phonemeId }
    val sessions = (sessionsState as? DataLoadState.Success)?.data.orEmpty()

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
            // Header & Account Sign-Out
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "PERSONALIZED FEEDBACK LOOP",
                            style = MaterialTheme.typography.labelSmall,
                            color = colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Mastery & History",
                            style = MaterialTheme.typography.headlineLarge,
                            color = colorScheme.onBackground
                        )
                        Text(
                            text = email?.takeIf { it.isNotBlank() } ?: displayName,
                            style = MaterialTheme.typography.bodySmall,
                            color = colorScheme.onSurfaceVariant
                        )
                    }
                    OutlinedButton(
                        onClick = onSignOut,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("sign_out_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.Logout,
                            contentDescription = "Sign out",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.sign_out))
                    }
                }
            }

            // In-App GitHub Actions APK Update Card
            item {
                AppUpdateCard(
                    updateState = updateState,
                    configuredRepo = configuredGithubRepo,
                    onCheckForUpdate = onCheckForUpdate,
                    onSaveRepo = onSaveGithubRepo,
                    onDownloadApk = onDownloadApk
                )
            }

            if (masteryState is DataLoadState.Error) {
                item {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = colorScheme.errorContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = masteryState.message,
                            style = MaterialTheme.typography.bodySmall,
                            color = colorScheme.onErrorContainer,
                            modifier = Modifier.padding(14.dp)
                        )
                    }
                }
            }

            // GenAm Phoneme Mastery Heatmap (3-column studio grid)
            item {
                StudioCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "GENAM PHONEME HEATMAP",
                                style = MaterialTheme.typography.labelSmall,
                                color = colorScheme.primary
                            )
                            Text(
                                text = "Tap any sound to inspect mechanics",
                                style = MaterialTheme.typography.bodySmall,
                                color = colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    val rows = GenAmCatalog.phonemes.chunked(3)
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        rows.forEach { rowPhonemes ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                rowPhonemes.forEach { p ->
                                    val doc = masteryDocs[p.id]
                                    val score = doc?.masteryScore?.toInt() ?: 0
                                    val attempts = doc?.attemptCount?.toInt() ?: 0
                                    val (tileBg, tileFg) = when {
                                        attempts == 0 -> colorScheme.surfaceVariant.copy(alpha = 0.65f) to colorScheme.onSurfaceVariant
                                        score >= 85 -> studioColors.masteryHighContainer to studioColors.masteryHigh
                                        score >= 65 -> studioColors.masteryMidContainer to studioColors.masteryMid
                                        else -> studioColors.masteryLowContainer to studioColors.masteryLow
                                    }
                                    Column(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(tileBg)
                                            .clickable { onSelectPhonemeForLab(p.id) }
                                            .padding(vertical = 12.dp, horizontal = 8.dp)
                                            .testTag("heatmap_${p.id}"),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = p.ipa,
                                            style = MaterialTheme.typography.titleLarge,
                                            color = tileFg
                                        )
                                        Text(
                                            text = p.exampleWord,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = colorScheme.onSurface
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = if (attempts == 0) "Untested" else "$score%",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = tileFg
                                        )
                                    }
                                }
                                repeat(3 - rowPhonemes.size) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = onGenerateAdaptiveDrill,
                        enabled = !generatingDrill,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .minimumInteractiveComponentSize()
                            .testTag("progress_generate_drill_button")
                    ) {
                        if (generatingDrill) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Building Targeted Drill…")
                        } else {
                            Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Generate Drill for Weakest Sounds")
                        }
                    }
                }
            }

            // Recent Sessions Header
            item {
                Text(
                    text = "Recent Acoustic Evaluations",
                    style = MaterialTheme.typography.headlineSmall,
                    color = colorScheme.onBackground
                )
            }

            if (sessionsState is DataLoadState.Loading) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            } else if (sessionsState is DataLoadState.Error) {
                item {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = colorScheme.errorContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = sessionsState.message,
                            style = MaterialTheme.typography.bodySmall,
                            color = colorScheme.onErrorContainer,
                            modifier = Modifier.padding(14.dp)
                        )
                    }
                }
            } else if (sessions.isEmpty()) {
                item {
                    StudioCard {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.History,
                                contentDescription = null,
                                tint = colorScheme.primary,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "No Evaluations Logged Yet",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = colorScheme.onSurface
                                )
                                Text(
                                    text = "Record your first take in the Shadow tab to track your General American clarity timeline here.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            } else {
                items(sessions, key = { it.id }) { session ->
                    StudioCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    MasteryPill(score = session.overallScore.toInt())
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = colorScheme.surfaceVariant
                                    ) {
                                        Text(
                                            text = TempoStep.nearest(session.speedMultiplier).label,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = session.phraseText,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = session.coachingSummary,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(
                                onClick = { onDeleteSession(session.id) },
                                modifier = Modifier.testTag("delete_session_${session.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.DeleteOutline,
                                    contentDescription = "Delete session log",
                                    tint = colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(
                                text = "Phonemes ${session.phonemeAccuracy}",
                                style = MaterialTheme.typography.labelSmall,
                                color = colorScheme.primary
                            )
                            Text(
                                text = "Rhythm ${session.rhythmScore}",
                                style = MaterialTheme.typography.labelSmall,
                                color = colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Flow ${session.flowScore}",
                                style = MaterialTheme.typography.labelSmall,
                                color = colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AppUpdateCard(
    updateState: AppUpdateUiState,
    configuredRepo: String,
    onCheckForUpdate: () -> Unit,
    onSaveRepo: (String) -> Unit,
    onDownloadApk: (String) -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val studioColors = LocalStudioColors.current
    var editingRepo by remember(configuredRepo) { mutableStateOf(configuredRepo.isBlank()) }
    var repoDraft by remember(configuredRepo) { mutableStateOf(configuredRepo) }

    StudioCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.SystemUpdate,
                    contentDescription = null,
                    tint = colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
                Column {
                    Text(
                        text = stringResource(R.string.update_section_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = colorScheme.onSurface
                    )
                    Text(
                        text = "Versão instalada: v${BuildConfig.VERSION_NAME} (build ${BuildConfig.VERSION_CODE})",
                        style = MaterialTheme.typography.labelSmall,
                        color = colorScheme.onSurfaceVariant
                    )
                }
            }
            if (configuredRepo.isNotBlank() && !editingRepo) {
                OutlinedButton(
                    onClick = { editingRepo = true },
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("edit_github_repo_button")
                ) {
                    Text("Repo", style = MaterialTheme.typography.labelSmall)
                }
            }
        }

        if (configuredRepo.isNotBlank() && !editingRepo) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "GitHub: $configuredRepo",
                style = MaterialTheme.typography.labelSmall,
                color = colorScheme.primary
            )
        }

        if (editingRepo) {
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedTextField(
                value = repoDraft,
                onValueChange = { repoDraft = it },
                label = { Text("Repositório GitHub (usuario/repositorio)") },
                placeholder = { Text("ex: seuusuario/voxform") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("github_repo_input")
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        editingRepo = false
                        onSaveRepo(repoDraft)
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("save_github_repo_button")
                ) {
                    Text("Salvar e verificar")
                }
                if (configuredRepo.isNotBlank()) {
                    OutlinedButton(
                        onClick = {
                            repoDraft = configuredRepo
                            editingRepo = false
                        },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Cancelar")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        when (updateState) {
            is AppUpdateUiState.Idle -> {
                Text(
                    text = "Verifique se há um novo APK gerado pelo GitHub Actions para baixar e instalar.",
                    style = MaterialTheme.typography.bodySmall,
                    color = colorScheme.onSurfaceVariant
                )
            }

            is AppUpdateUiState.Checking -> {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    Text(
                        text = "Consultando GitHub Releases…",
                        style = MaterialTheme.typography.bodySmall,
                        color = colorScheme.onSurfaceVariant
                    )
                }
            }

            is AppUpdateUiState.UpdateAvailable -> {
                val sizeMb = if (updateState.info.apkSizeBytes > 0L) {
                    String.format(java.util.Locale.US, " • %.1f MB", updateState.info.apkSizeBytes / (1024f * 1024f))
                } else {
                    ""
                }
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = colorScheme.secondaryContainer.copy(alpha = 0.65f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Nova atualização: ${updateState.info.tagName}$sizeMb",
                            style = MaterialTheme.typography.titleSmall,
                            color = colorScheme.onSecondaryContainer
                        )
                        if (updateState.info.releaseNotes.isNotBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = updateState.info.releaseNotes,
                                style = MaterialTheme.typography.bodySmall,
                                color = colorScheme.onSecondaryContainer,
                                maxLines = 4
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = { onDownloadApk(updateState.info.apkDownloadUrl) },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colorScheme.secondary,
                                contentColor = colorScheme.onSecondary
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .minimumInteractiveComponentSize()
                                .testTag("download_apk_button")
                        ) {
                            Icon(Icons.Rounded.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.update_download_apk))
                        }
                    }
                }
            }

            is AppUpdateUiState.UpToDate -> {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = studioColors.masteryHighContainer.copy(alpha = 0.65f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Você está na versão mais recente (${updateState.info.tagName}).",
                            style = MaterialTheme.typography.titleSmall,
                            color = studioColors.masteryHigh
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = { onDownloadApk(updateState.info.apkDownloadUrl) },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("download_latest_apk_button")
                        ) {
                            Icon(Icons.Rounded.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Baixar APK (${updateState.info.tagName})")
                        }
                    }
                }
            }

            is AppUpdateUiState.Error -> {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = colorScheme.errorContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = updateState.message,
                        style = MaterialTheme.typography.bodySmall,
                        color = colorScheme.onErrorContainer,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        }

        if (!editingRepo) {
            Spacer(modifier = Modifier.height(12.dp))
            FilledTonalButton(
                onClick = onCheckForUpdate,
                enabled = updateState !is AppUpdateUiState.Checking,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .minimumInteractiveComponentSize()
                    .testTag("check_update_button")
            ) {
                Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.update_check_button))
            }
        }
    }
}

