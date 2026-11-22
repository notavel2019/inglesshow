package com.example.domain

import kotlin.math.roundToInt

/**
 * KMP-ready adaptive coaching engine:
 * - Updates per-phoneme EWMA mastery scores
 * - Computes progressive speed stepping recommendations
 * - Ranks the learner's personalized Focus Queue
 */
object MasteryFeedbackEngine {

    private const val EWMA_ALPHA = 0.42f
    private const val STEP_UP_THRESHOLD = 85

    fun computeUpdatedMastery(currentScore: Int, attemptCount: Int, newScore: Int): Int {
        val clamped = newScore.coerceIn(0, 100)
        if (attemptCount <= 0) return clamped
        val blended = (currentScore * (1f - EWMA_ALPHA)) + (clamped * EWMA_ALPHA)
        return blended.roundToInt().coerceIn(0, 100)
    }

    fun recommendSpeedForMastery(masteryScore: Int, currentStep: TempoStep): TempoStep {
        return when {
            masteryScore >= 90 -> currentStep.nextStep() ?: TempoStep.FLUENCY
            masteryScore >= STEP_UP_THRESHOLD -> currentStep.nextStep() ?: TempoStep.NATIVE
            masteryScore < 60 -> TempoStep.BREAKDOWN
            masteryScore < 75 -> TempoStep.GUIDED
            else -> currentStep
        }
    }

    fun computeOverallReadiness(masteryById: Map<String, Int>): Int {
        if (masteryById.isEmpty()) return 0
        val scores = masteryById.values.filter { it > 0 }
        if (scores.isEmpty()) return 0
        return scores.average().roundToInt().coerceIn(0, 100)
    }

    fun buildFocusQueue(
        masteryMap: Map<String, PhonemeMasterySnapshot>,
        limit: Int = 5
    ): List<FocusQueueItem> {
        val nowMs = System.currentTimeMillis()
        val items = GenAmCatalog.phonemes.map { phoneme ->
            val record = masteryMap[phoneme.id]
            val score = record?.masteryScore ?: 55
            val attempts = record?.attemptCount ?: 0
            val speed = TempoStep.nearest(record?.recommendedSpeed ?: 0.80)
            val note = record?.recentIssueNote?.takeIf { it.isNotBlank() } ?: phoneme.placementCue

            // Priority favors low mastery, plus gentle boost for unpracticed or stale phonemes
            val weaknessWeight = (100 - score).toFloat()
            val stalenessHours = if (record?.lastPracticedMs != null && record.lastPracticedMs > 0L) {
                ((nowMs - record.lastPracticedMs) / 3_600_000f).coerceIn(0f, 72f)
            } else {
                24f
            }
            val freshnessBonus = if (attempts == 0) 12f else (stalenessHours * 0.25f)
            val priority = weaknessWeight + freshnessBonus

            FocusQueueItem(
                phoneme = phoneme,
                masteryScore = score,
                attemptCount = attempts,
                recommendedSpeed = speed,
                recentIssueNote = note,
                priorityScore = priority
            )
        }
        return items.sortedByDescending { it.priorityScore }.take(limit)
    }

    fun findBestPhraseForPhoneme(phonemeId: String, availablePhrases: List<PracticePhrase>): PracticePhrase {
        return availablePhrases.firstOrNull { phonemeId in it.targetPhonemeIds }
            ?: GenAmCatalog.starterPhrases.first()
    }
}

data class PhonemeMasterySnapshot(
    val phonemeId: String,
    val masteryScore: Int,
    val attemptCount: Int,
    val recommendedSpeed: Double,
    val recentIssueNote: String,
    val lastPracticedMs: Long?
)
