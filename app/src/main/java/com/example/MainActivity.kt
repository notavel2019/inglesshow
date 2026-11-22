package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.RecordVoiceOver
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.RecordVoiceOver
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.configureAppCheck
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.MouthLabScreen
import com.example.ui.screens.ProgressScreen
import com.example.ui.screens.ShadowingScreen
import com.example.ui.screens.StudioHomeScreen
import com.example.ui.theme.VoxFormTheme
import com.example.viewmodel.StudioViewModel
import com.example.viewmodel.signOutUser
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // STEP 1: Configure Firebase App Check with launch intent before any Compose UI, ViewModels, or Firebase calls
        configureAppCheck(this, intent)

        enableEdgeToEdge()

        setContent {
            VoxFormTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    VoxFormAppRoot()
                }
            }
        }
    }
}

private enum class StudioTab(
    val route: String,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    HOME("studio_home", R.string.nav_studio, Icons.Rounded.Home, Icons.Outlined.Home),
    SHADOW("shadowing_stage", R.string.nav_shadow, Icons.Rounded.GraphicEq, Icons.Outlined.GraphicEq),
    MOUTH_LAB("mouth_lab", R.string.nav_mouth_lab, Icons.Rounded.RecordVoiceOver, Icons.Outlined.RecordVoiceOver),
    PROGRESS("mastery_progress", R.string.nav_progress, Icons.Rounded.Insights, Icons.Outlined.Insights)
}

@Composable
private fun VoxFormAppRoot() {
    val context = LocalContext.current
    val firebaseReady = remember(context) { com.example.data.isFirebaseAvailable(context) }

    if (!firebaseReady) {
        key("local_studio") {
            AuthenticatedStudioShell(
                uid = "local_studio",
                displayName = "VoxForm Studio",
                email = "Modo local (adicione GOOGLE_SERVICES_JSON no GitHub para nuvem/IA)",
                onSignedOut = {}
            )
        }
        return
    }

    var currentUser by remember { mutableStateOf(Firebase.auth.currentUser) }

    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            currentUser = auth.currentUser
        }
        Firebase.auth.addAuthStateListener(listener)
        onDispose { Firebase.auth.removeAuthStateListener(listener) }
    }

    if (currentUser == null) {
        AuthScreen(onAuthSuccess = { currentUser = Firebase.auth.currentUser })
    } else {
        val uid = currentUser!!.uid
        val displayName = currentUser!!.displayName ?: "Studio Learner"
        val email = currentUser!!.email
        key(uid) {
            AuthenticatedStudioShell(
                uid = uid,
                displayName = displayName,
                email = email,
                onSignedOut = { currentUser = null }
            )
        }
    }
}

@Composable
private fun AuthenticatedStudioShell(
    uid: String,
    displayName: String,
    email: String?,
    onSignedOut: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val studioViewModel: StudioViewModel = viewModel(
        key = uid,
        factory = StudioViewModel.Factory(
            context = context.applicationContext,
            userId = uid,
            displayName = displayName
        )
    )

    var selectedTab by rememberSaveable { mutableStateOf(StudioTab.HOME) }

    // Return to Studio Home on back press when on secondary tabs
    BackHandler(enabled = selectedTab != StudioTab.HOME) {
        selectedTab = StudioTab.HOME
    }

    val profileState by studioViewModel.profileState.collectAsStateWithLifecycle()
    val masteryState by studioViewModel.masteryState.collectAsStateWithLifecycle()
    val sessionsState by studioViewModel.sessionsState.collectAsStateWithLifecycle()
    val focusQueue by studioViewModel.focusQueue.collectAsStateWithLifecycle()
    val readinessScore by studioViewModel.readinessScore.collectAsStateWithLifecycle()
    val shadowingState by studioViewModel.shadowingState.collectAsStateWithLifecycle()
    val mouthLabState by studioViewModel.mouthLabState.collectAsStateWithLifecycle()
    val isSpeakingReference by studioViewModel.isSpeakingReference.collectAsStateWithLifecycle()
    val isRecording by studioViewModel.isRecording.collectAsStateWithLifecycle()
    val isPlayingRecording by studioViewModel.isPlayingRecording.collectAsStateWithLifecycle()
    val liveAmplitudes by studioViewModel.liveAmplitudes.collectAsStateWithLifecycle()
    val recordedDurationSec by studioViewModel.recordedDurationSec.collectAsStateWithLifecycle()
    val updateState by studioViewModel.updateState.collectAsStateWithLifecycle()
    val configuredGithubRepo by studioViewModel.configuredGithubRepo.collectAsStateWithLifecycle()

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useNavRail = maxWidth >= 600.dp

        if (useNavRail) {
            Scaffold(
                containerColor = MaterialTheme.colorScheme.background,
                contentWindowInsets = WindowInsets.safeDrawing
            ) { innerPadding ->
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    NavigationRail(
                        containerColor = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.fillMaxHeight()
                    ) {
                        StudioTab.entries.forEach { tab ->
                            val selected = tab == selectedTab
                            NavigationRailItem(
                                selected = selected,
                                onClick = { selectedTab = tab },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = stringResource(tab.labelRes)
                                    )
                                },
                                label = { Text(stringResource(tab.labelRes)) },
                                modifier = Modifier.testTag("nav_${tab.route}")
                            )
                        }
                    }
                    Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                        StudioTabContent(
                            selectedTab = selectedTab,
                            displayName = displayName,
                            email = email,
                            profileState = profileState,
                            masteryState = masteryState,
                            sessionsState = sessionsState,
                            focusQueue = focusQueue,
                            readinessScore = readinessScore,
                            shadowingState = shadowingState,
                            mouthLabState = mouthLabState,
                            isSpeakingReference = isSpeakingReference,
                            isRecording = isRecording,
                            isPlayingRecording = isPlayingRecording,
                            liveAmplitudes = liveAmplitudes,
                            recordedDurationSec = recordedDurationSec,
                            updateState = updateState,
                            configuredGithubRepo = configuredGithubRepo,
                            studioViewModel = studioViewModel,
                            onDownloadApk = { url -> studioViewModel.downloadApk(context, url) },
                            onNavigateTab = { selectedTab = it },
                            onSignOut = {
                                signOutUser(credentialManager, scope, onSignOutComplete = onSignedOut)
                            }
                        )
                    }
                }
            }
        } else {
            Scaffold(
                containerColor = MaterialTheme.colorScheme.background,
                contentWindowInsets = WindowInsets.safeDrawing,
                bottomBar = {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        windowInsets = WindowInsets.navigationBars
                    ) {
                        StudioTab.entries.forEach { tab ->
                            val selected = tab == selectedTab
                            NavigationBarItem(
                                selected = selected,
                                onClick = { selectedTab = tab },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = stringResource(tab.labelRes)
                                    )
                                },
                                label = { Text(stringResource(tab.labelRes)) },
                                modifier = Modifier.testTag("nav_${tab.route}")
                            )
                        }
                    }
                }
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    StudioTabContent(
                        selectedTab = selectedTab,
                        displayName = displayName,
                        email = email,
                        profileState = profileState,
                        masteryState = masteryState,
                        sessionsState = sessionsState,
                        focusQueue = focusQueue,
                        readinessScore = readinessScore,
                        shadowingState = shadowingState,
                        mouthLabState = mouthLabState,
                        isSpeakingReference = isSpeakingReference,
                        isRecording = isRecording,
                        isPlayingRecording = isPlayingRecording,
                        liveAmplitudes = liveAmplitudes,
                        recordedDurationSec = recordedDurationSec,
                        updateState = updateState,
                        configuredGithubRepo = configuredGithubRepo,
                        studioViewModel = studioViewModel,
                        onDownloadApk = { url -> studioViewModel.downloadApk(context, url) },
                        onNavigateTab = { selectedTab = it },
                        onSignOut = {
                            signOutUser(credentialManager, scope, onSignOutComplete = onSignedOut)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun StudioTabContent(
    selectedTab: StudioTab,
    displayName: String,
    email: String?,
    profileState: com.example.viewmodel.DataLoadState<com.example.data.LearnerProfileDoc?>,
    masteryState: com.example.viewmodel.DataLoadState<List<com.example.data.PhonemeMasteryDoc>>,
    sessionsState: com.example.viewmodel.DataLoadState<List<com.example.data.PracticeSessionDoc>>,
    focusQueue: List<com.example.domain.FocusQueueItem>,
    readinessScore: Int,
    shadowingState: com.example.viewmodel.ShadowingStudioState,
    mouthLabState: com.example.viewmodel.MouthLabState,
    isSpeakingReference: Boolean,
    isRecording: Boolean,
    isPlayingRecording: Boolean,
    liveAmplitudes: List<Float>,
    recordedDurationSec: Float,
    updateState: com.example.viewmodel.AppUpdateUiState,
    configuredGithubRepo: String,
    studioViewModel: StudioViewModel,
    onDownloadApk: (String) -> Unit,
    onNavigateTab: (StudioTab) -> Unit,
    onSignOut: () -> Unit
) {
    when (selectedTab) {
        StudioTab.HOME -> StudioHomeScreen(
            displayName = displayName,
            profileState = profileState,
            readinessScore = readinessScore,
            focusQueue = focusQueue,
            generatingDrill = shadowingState.generatingDrill,
            drillError = shadowingState.drillErrorMessage,
            updateState = updateState,
            onDownloadUpdateApk = onDownloadApk,
            onLaunchShadowingForPhoneme = { phonemeId, speed ->
                studioViewModel.launchDrillForPhoneme(phonemeId, speed)
                onNavigateTab(StudioTab.SHADOW)
            },
            onOpenMouthLabForPhoneme = { phonemeId ->
                studioViewModel.selectMouthLabPhoneme(phonemeId)
                onNavigateTab(StudioTab.MOUTH_LAB)
            },
            onSelectPhraseAndShadow = { phrase ->
                studioViewModel.selectPhrase(phrase)
                onNavigateTab(StudioTab.SHADOW)
            },
            onGenerateAdaptiveDrill = {
                studioViewModel.generateAdaptiveDrill(onReady = {
                    onNavigateTab(StudioTab.SHADOW)
                })
            }
        )

        StudioTab.SHADOW -> ShadowingScreen(
            state = shadowingState,
            isSpeakingReference = isSpeakingReference,
            isRecording = isRecording,
            isPlayingRecording = isPlayingRecording,
            liveAmplitudes = liveAmplitudes,
            recordedDurationSec = recordedDurationSec,
            onSelectPhrase = studioViewModel::selectPhrase,
            onSelectSpeed = studioViewModel::selectSpeed,
            onPlayReference = studioViewModel::playOrStopReference,
            onSpeakWord = studioViewModel::speakSingleWord,
            onToggleRecording = studioViewModel::toggleRecording,
            onToggleReplay = studioViewModel::toggleReplayRecording,
            onEvaluateTake = studioViewModel::evaluateCurrentTake,
            onOpenInlineMouth = studioViewModel::openInlineVisualizer,
            onOpenFullMouthLab = { phonemeId ->
                studioViewModel.selectMouthLabPhoneme(phonemeId)
                onNavigateTab(StudioTab.MOUTH_LAB)
            }
        )

        StudioTab.MOUTH_LAB -> MouthLabScreen(
            state = mouthLabState,
            onSelectPhoneme = studioViewModel::selectMouthLabPhoneme,
            onToggleContrast = studioViewModel::toggleContrastOverlay,
            onToggleLabels = studioViewModel::toggleAnatomicalLabels,
            onToggleLoop = studioViewModel::toggleLoopingAnimation,
            onScrubPhase = studioViewModel::updateManualPhase,
            onSpeakExample = studioViewModel::speakSingleWord,
            onPracticeSound = { phonemeId ->
                studioViewModel.launchDrillForPhoneme(phonemeId)
                onNavigateTab(StudioTab.SHADOW)
            }
        )

        StudioTab.PROGRESS -> ProgressScreen(
            displayName = displayName,
            email = email,
            masteryState = masteryState,
            sessionsState = sessionsState,
            generatingDrill = shadowingState.generatingDrill,
            updateState = updateState,
            configuredGithubRepo = configuredGithubRepo,
            onCheckForUpdate = studioViewModel::checkForAppUpdate,
            onSaveGithubRepo = studioViewModel::saveGithubRepoAndCheck,
            onDownloadApk = onDownloadApk,
            onSelectPhonemeForLab = { phonemeId ->
                studioViewModel.selectMouthLabPhoneme(phonemeId)
                onNavigateTab(StudioTab.MOUTH_LAB)
            },
            onGenerateAdaptiveDrill = {
                studioViewModel.generateAdaptiveDrill(onReady = {
                    onNavigateTab(StudioTab.SHADOW)
                })
            },
            onDeleteSession = studioViewModel::deleteSession,
            onSignOut = onSignOut
        )
    }
}
