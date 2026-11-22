package com.example.domain

/**
 * Pure Kotlin KMP-ready domain models for General American phonetics,
 * biomechanical 2D vocal tract keyframes, progressive shadowing tempo stepping,
 * and structured multimodal acoustic evaluation.
 */

enum class PhonemeCategory(val label: String) {
    DENTAL("Dental"),
    RHOTIC("Rhotic & Liquid"),
    VOWEL("Vowels"),
    STOP_FLAP("Flap & Stops"),
    NASAL("Nasals")
}

enum class AirflowType {
    CONTINUOUS_ORAL,     // Fricatives (/θ/, /ð/, /s/)
    LAMINAR_OPEN,        // Vowels & approximants (/ɹ/, /æ/, /iː/)
    PLOSIVE_TAP,         // Flap /ɾ/ or brief tap
    NASAL_CHANNEL        // Velum lowered (/ŋ/)
}

/**
 * Normalized midsagittal vocal tract coordinates (0f..1f within the anatomical viewport).
 * Designed for smooth cubic Bezier spline interpolation across rest, onset, hold, and release.
 */
data class ArticulationPose(
    val jawDrop: Float,          // 0f = closed, 1f = wide open
    val lipRounding: Float,      // 0f = spread/unrounded, 1f = strongly rounded/protruded
    val lipAperture: Float,      // 0f = sealed, 1f = wide opening
    val tongueTipX: Float,       // 0.14 (interdental) .. 0.45 (retroflex bunched)
    val tongueTipY: Float,       // 0.34 (alveolar contact) .. 0.70 (low floor)
    val tongueBladeX: Float,
    val tongueBladeY: Float,
    val tongueDorsumX: Float,
    val tongueDorsumY: Float,
    val tongueRootX: Float,
    val tongueRootY: Float,
    val velumOpen: Float,        // 0f = sealed against pharynx (oral), 1f = lowered (nasal)
    val voiced: Boolean,
    val airflowType: AirflowType,
    val contactLabel: String
)

data class GenAmPhoneme(
    val id: String,
    val ipa: String,
    val name: String,
    val category: PhonemeCategory,
    val exampleWord: String,
    val exampleIpa: String,
    val placementCue: String,
    val airflowCue: String,
    val commonMistake: String,
    val contrastPhonemeId: String?,
    val contrastLabel: String?,
    val pose: ArticulationPose
)

data class PracticePhrase(
    val id: String,
    val title: String,
    val text: String,
    val ipa: String,
    val stressGuide: String,
    val targetPhonemeIds: List<String>,
    val difficulty: String,
    val category: PhonemeCategory,
    val generatedByAi: Boolean = false
)

enum class TempoStep(val multiplier: Float, val label: String, val caption: String) {
    BREAKDOWN(0.65f, "0.65×", "Slow Breakdown"),
    GUIDED(0.80f, "0.80×", "Guided"),
    BRIDGE(0.90f, "0.90×", "Natural Bridge"),
    NATIVE(1.00f, "1.00×", "Native GenAm"),
    FLUENCY(1.10f, "1.10×", "Fluency Stretch");

    fun nextStep(): TempoStep? {
        val all = entries
        val idx = all.indexOf(this)
        return if (idx in 0 until all.lastIndex) all[idx + 1] else null
    }

    companion object {
        fun nearest(speed: Double): TempoStep {
            return entries.minByOrNull { kotlin.math.abs(it.multiplier - speed.toFloat()) } ?: GUIDED
        }
    }
}

enum class ClarityTier {
    CLEAR,       // >= 85
    CLOSE,       // 65..84
    ADJUST       // < 65
}

data class WordEvaluation(
    val word: String,
    val expectedIpa: String,
    val status: ClarityTier,
    val detectedIssue: String,
    val phonemeId: String?
)

data class PhonemeEvaluationResult(
    val phonemeId: String,
    val ipa: String,
    val score: Int,
    val physicalTip: String
)

data class AcousticEvaluation(
    val overallScore: Int,
    val phonemeAccuracy: Int,
    val rhythmScore: Int,
    val flowScore: Int,
    val coachingSummary: String,
    val nextStepRecommendation: String,
    val shouldStepUpSpeed: Boolean,
    val wordBreakdown: List<WordEvaluation>,
    val phonemeResults: List<PhonemeEvaluationResult>
)

data class FocusQueueItem(
    val phoneme: GenAmPhoneme,
    val masteryScore: Int,
    val attemptCount: Int,
    val recommendedSpeed: TempoStep,
    val recentIssueNote: String,
    val priorityScore: Float
)
