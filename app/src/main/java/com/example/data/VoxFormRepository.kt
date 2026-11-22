package com.example.data

import android.content.Context
import com.example.R
import com.example.domain.AcousticEvaluation
import com.example.domain.GenAmCatalog
import com.example.domain.MasteryFeedbackEngine
import com.example.domain.PracticePhrase
import com.example.domain.TempoStep
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

/**
 * Authenticated Firestore repository targeting the provisioned named database
 * via R.string.firestore_database_id. Never self-authenticates.
 */
class VoxFormRepository(private val db: FirebaseFirestore?) {

    constructor(context: Context) : this(
        if (isFirebaseAvailable(context)) {
            runCatching {
                FirebaseFirestore.getInstance(
                    context.applicationContext.getString(R.string.firestore_database_id)
                )
            }.getOrNull()
        } else {
            null
        }
    )

    private val auth
        get() = if (db != null) runCatching { Firebase.auth }.getOrNull() else null

    private fun requireUserId(): String {
        val uid = auth?.currentUser?.uid
        require(!uid.isNullOrBlank()) {
            "User must be signed in with Google before accessing Firestore."
        }
        return uid
    }

    fun observeLearnerProfile(): Flow<LearnerProfileDoc?> = flow {
        val firestore = db
        if (firestore == null) {
            emit(null)
            return@flow
        }
        val uid = requireUserId()
        val path = "users/$uid"
        emitAll(
            firestore.collection("users").document(uid)
                .snapshots()
                .map { snapshot ->
                    if (snapshot.exists()) {
                        snapshot.toObject(
                            LearnerProfileDoc::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        )
                    } else {
                        null
                    }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth?.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.GET, path)
                    }
                    throw error
                }
        )
    }

    fun observePhonemeMastery(): Flow<List<PhonemeMasteryDoc>> = flow {
        val firestore = db
        if (firestore == null) {
            emit(emptyList())
            return@flow
        }
        val uid = requireUserId()
        val path = "users/$uid/phonemeMastery"
        emitAll(
            firestore.collection("users").document(uid)
                .collection("phonemeMastery")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        PhonemeMasteryDoc::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth?.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    fun observePracticeSessions(limitCount: Long = 25L): Flow<List<PracticeSessionDoc>> = flow {
        val firestore = db
        if (firestore == null) {
            emit(emptyList())
            return@flow
        }
        val uid = requireUserId()
        val path = "users/$uid/practiceSessions"
        emitAll(
            firestore.collection("users").document(uid)
                .collection("practiceSessions")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(limitCount)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        PracticeSessionDoc::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth?.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun ensureLearnerProfile(fallbackName: String): Result<Unit> {
        val firestore = db ?: return Result.success(Unit)
        var docPath = "users/unauthenticated"
        return try {
            val uid = requireUserId()
            val docRef = firestore.collection("users").document(uid)
            docPath = docRef.path
            val existing = docRef.get().await()
            if (!existing.exists()) {
                val safeName = fallbackName.trim().ifBlank { "Studio Learner" }.take(80)
                val payload = mapOf(
                    "userId" to uid,
                    "displayName" to safeName,
                    "currentStreakDays" to 1L,
                    "totalSessions" to 0L,
                    "overallClarityScore" to 0L,
                    "preferredStartSpeed" to 0.80,
                    "focusPhonemeIds" to listOf("th_voiceless", "r_american", "flap_t"),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(payload).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun updatePreferredSpeed(speed: TempoStep): Result<Unit> {
        val firestore = db ?: return Result.success(Unit)
        var docPath = "users/unauthenticated"
        return try {
            val uid = requireUserId()
            val docRef = firestore.collection("users").document(uid)
            docPath = docRef.path
            docRef.update(
                mapOf(
                    "preferredStartSpeed" to speed.multiplier.toDouble().coerceIn(0.5, 1.5),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    /**
     * Persists a completed acoustic evaluation session, updates per-phoneme EWMA mastery docs,
     * and refreshes the learner profile readiness metrics.
     */
    suspend fun recordEvaluation(
        phrase: PracticePhrase,
        speed: TempoStep,
        evaluation: AcousticEvaluation,
        existingMastery: Map<String, PhonemeMasteryDoc>,
        currentProfile: LearnerProfileDoc?
    ): Result<Unit> {
        val firestore = db ?: return Result.failure(
            IllegalStateException("Modo local ativo — configure GOOGLE_SERVICES_JSON no GitHub para sincronizar na nuvem.")
        )
        var activePath = "users/unauthenticated/practiceSessions"
        return try {
            val uid = requireUserId()
            val userDoc = firestore.collection("users").document(uid)

            // 1. Append immutable PracticeSession document
            val sessionRef = userDoc.collection("practiceSessions").document()
            activePath = sessionRef.path
            val safeTargets = phrase.targetPhonemeIds
                .filter { it.matches(Regex("^[a-zA-Z0-9_\\-]+$")) }
                .take(10)

            val sessionPayload = mapOf(
                "phraseId" to phrase.id.take(80).ifBlank { "custom_drill" },
                "phraseText" to phrase.text.take(300).ifBlank { "Practice phrase" },
                "targetPhonemes" to safeTargets,
                "speedMultiplier" to speed.multiplier.toDouble().coerceIn(0.5, 1.5),
                "overallScore" to evaluation.overallScore.coerceIn(0, 100).toLong(),
                "phonemeAccuracy" to evaluation.phonemeAccuracy.coerceIn(0, 100).toLong(),
                "rhythmScore" to evaluation.rhythmScore.coerceIn(0, 100).toLong(),
                "flowScore" to evaluation.flowScore.coerceIn(0, 100).toLong(),
                "coachingSummary" to evaluation.coachingSummary.take(600).ifBlank { "Evaluation completed." },
                "nextStepRecommendation" to evaluation.nextStepRecommendation.take(300).ifBlank { "Continue progressive shadowing." },
                "createdAt" to FieldValue.serverTimestamp()
            )
            sessionRef.set(sessionPayload).await()

            // 2. Update per-phoneme mastery scores for every evaluated phoneme in catalog
            val updatedScoresMap = existingMastery.mapValues { it.value.masteryScore.toInt() }.toMutableMap()
            val evaluatedPhonemes = if (evaluation.phonemeResults.isNotEmpty()) {
                evaluation.phonemeResults
            } else {
                safeTargets.mapNotNull { id ->
                    GenAmCatalog.phonemeById[id]?.let {
                        com.example.domain.PhonemeEvaluationResult(
                            phonemeId = id,
                            ipa = it.ipa,
                            score = evaluation.phonemeAccuracy,
                            physicalTip = it.placementCue
                        )
                    }
                }
            }

            for (result in evaluatedPhonemes) {
                val catalogPhoneme = GenAmCatalog.phonemeById[result.phonemeId] ?: continue
                val masteryRef = userDoc.collection("phonemeMastery").document(catalogPhoneme.id)
                activePath = masteryRef.path

                val prior = existingMastery[catalogPhoneme.id]
                val priorAttempts = prior?.attemptCount?.toInt() ?: 0
                val priorScore = prior?.masteryScore?.toInt() ?: 0
                val nextMastery = MasteryFeedbackEngine.computeUpdatedMastery(
                    currentScore = priorScore,
                    attemptCount = priorAttempts,
                    newScore = result.score
                )
                val nextSpeed = MasteryFeedbackEngine.recommendSpeedForMastery(nextMastery, speed)
                val issueNote = result.physicalTip.trim().take(300).takeIf { it.isNotBlank() }

                updatedScoresMap[catalogPhoneme.id] = nextMastery

                if (prior == null) {
                    val createPayload = mapOf<String, Any?>(
                        "phonemeId" to catalogPhoneme.id,
                        "ipaSymbol" to catalogPhoneme.ipa.take(16),
                        "category" to catalogPhoneme.category.label.take(40),
                        "masteryScore" to nextMastery.toLong(),
                        "attemptCount" to 1L,
                        "recommendedSpeed" to nextSpeed.multiplier.toDouble(),
                        "recentIssueNote" to issueNote,
                        "lastPracticedAt" to FieldValue.serverTimestamp(),
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    ).filterValues { it != null }
                    masteryRef.set(createPayload).await()
                } else {
                    val updatePayload = mapOf<String, Any?>(
                        "ipaSymbol" to catalogPhoneme.ipa.take(16),
                        "category" to catalogPhoneme.category.label.take(40),
                        "masteryScore" to nextMastery.toLong(),
                        "attemptCount" to (priorAttempts + 1L),
                        "recommendedSpeed" to nextSpeed.multiplier.toDouble(),
                        "recentIssueNote" to issueNote,
                        "lastPracticedAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    ).filterValues { it != null }
                    masteryRef.update(updatePayload).await()
                }
            }

            // 3. Update learner profile summary metrics
            activePath = userDoc.path
            val overallReadiness = MasteryFeedbackEngine.computeOverallReadiness(updatedScoresMap)
                .takeIf { it > 0 } ?: evaluation.overallScore.coerceIn(0, 100)
            val weakestIds = updatedScoresMap.entries
                .sortedBy { it.value }
                .take(4)
                .map { it.key }
                .ifEmpty { listOf("th_voiceless", "r_american", "flap_t") }

            val nextPreferredSpeed = if (evaluation.shouldStepUpSpeed) {
                (speed.nextStep() ?: speed).multiplier.toDouble()
            } else {
                speed.multiplier.toDouble()
            }

            if (currentProfile != null) {
                val profileUpdate = mapOf(
                    "totalSessions" to (currentProfile.totalSessions + 1L),
                    "overallClarityScore" to overallReadiness.toLong(),
                    "preferredStartSpeed" to nextPreferredSpeed.coerceIn(0.5, 1.5),
                    "focusPhonemeIds" to weakestIds,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                userDoc.update(profileUpdate).await()
            }

            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, activePath)
            Result.failure(e)
        }
    }

    suspend fun deleteSession(sessionId: String): Result<Unit> {
        val firestore = db ?: return Result.success(Unit)
        var docPath = "users/unauthenticated/practiceSessions/$sessionId"
        return try {
            val uid = requireUserId()
            require(sessionId.isNotBlank()) { "Session ID cannot be blank" }
            val ref = firestore.collection("users").document(uid)
                .collection("practiceSessions").document(sessionId)
            docPath = ref.path
            ref.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }
}
