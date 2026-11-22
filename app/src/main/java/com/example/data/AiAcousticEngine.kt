package com.example.data

import android.util.Log
import com.example.domain.AcousticEvaluation
import com.example.domain.ClarityTier
import com.example.domain.GenAmCatalog
import com.example.domain.GenAmPhoneme
import com.example.domain.PhonemeEvaluationResult
import com.example.domain.PracticePhrase
import com.example.domain.TempoStep
import com.example.domain.WordEvaluation
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

private const val TAG = "AiAcousticEngine"

/**
 * Multimodal General American acoustic evaluation and personalized drill generator
 * powered by Firebase AI Logic (gemini-3.8-flash) with strict structured JSON schemas.
 */
class AiAcousticEngine {

    private val evaluationSchema = Schema.obj(
        properties = mapOf(
            "overallScore" to Schema.integer(description = "Overall General American intelligibility score from 0 to 100"),
            "phonemeAccuracy" to Schema.integer(description = "Phoneme precision score from 0 to 100"),
            "rhythmScore" to Schema.integer(description = "Syllable stress and natural American cadence score from 0 to 100"),
            "flowScore" to Schema.integer(description = "Connected speech smoothness score from 0 to 100"),
            "coachingSummary" to Schema.string(description = "Concise 1-2 sentence acoustic coach summary referencing what was heard"),
            "nextStepRecommendation" to Schema.string(description = "Specific next shadowing speed or mouth posture step"),
            "shouldStepUpSpeed" to Schema.boolean(description = "True if overall clarity is >= 85 and ready for next tempo tier"),
            "wordBreakdown" to Schema.array(
                items = Schema.obj(
                    properties = mapOf(
                        "word" to Schema.string(description = "Target word from the phrase"),
                        "expectedIpa" to Schema.string(description = "General American IPA transcription for the word"),
                        "status" to Schema.enumeration(
                            values = listOf("CLEAR", "CLOSE", "ADJUST"),
                            description = "Pronunciation quality tier for this word"
                        ),
                        "detectedIssue" to Schema.string(description = "Concrete acoustic observation or empty string if clear"),
                        "phonemeId" to Schema.string(
                            description = "Matching catalog phoneme ID from target list if applicable, or empty string",
                            nullable = true
                        )
                    ),
                    optionalProperties = listOf("phonemeId")
                ),
                description = "Word-by-word pronunciation assessment focusing on key words in the phrase"
            ),
            "phonemeResults" to Schema.array(
                items = Schema.obj(
                    properties = mapOf(
                        "phonemeId" to Schema.string(description = "Exact phonemeId from the provided target catalog"),
                        "ipa" to Schema.string(description = "IPA symbol such as /θ/ or /ɹ/"),
                        "score" to Schema.integer(description = "Accuracy score 0 to 100 for this phoneme"),
                        "physicalTip" to Schema.string(description = "Concrete tongue, lip, jaw, or airflow placement cue")
                    )
                ),
                description = "Per-phoneme accuracy and physical coaching cue for each target phoneme"
            )
        )
    )

    private val customDrillSchema = Schema.obj(
        properties = mapOf(
            "title" to Schema.string(description = "Short 2-4 word title for this custom drill"),
            "text" to Schema.string(description = "Natural 10-16 word General American English sentence rich in the requested weak phonemes"),
            "ipa" to Schema.string(description = "Full General American IPA transcription enclosed in slashes"),
            "stressGuide" to Schema.string(description = "Same sentence with primary stressed syllables in UPPERCASE"),
            "difficulty" to Schema.enumeration(
                values = listOf("Foundation", "Core", "Intermediate", "Fluency"),
                description = "Difficulty tier"
            )
        )
    )

    // Lazily initialized so FirebaseApp and App Check are fully configured before first inference
    private val evaluationModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                temperature = 0.25f
                responseMimeType = "application/json"
                responseSchema = evaluationSchema
            }
        )
    }

    private val drillGeneratorModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                temperature = 0.7f
                responseMimeType = "application/json"
                responseSchema = customDrillSchema
            }
        )
    }

    private fun isFirebaseInitialized(): Boolean {
        return runCatching { FirebaseApp.getInstance() }.isSuccess
    }

    suspend fun evaluatePronunciation(
        wavBytes: ByteArray,
        phrase: PracticePhrase,
        speed: TempoStep
    ): Result<AcousticEvaluation> {
        if (!isFirebaseInitialized()) {
            return Result.failure(
                IllegalStateException(
                    "IA Gemini indisponível neste APK (falta app/google-services.json). Adicione o secret GOOGLE_SERVICES_JSON no GitHub Actions e gere um novo APK."
                )
            )
        }
        if (wavBytes.isEmpty()) {
            return Result.failure(IllegalArgumentException("No recorded audio found. Please record your voice first."))
        }
        // Keep inline audio safely under 20MB limit
        if (wavBytes.size > 15 * 1024 * 1024) {
            return Result.failure(IllegalArgumentException("Recording is too long. Please keep clips under 15 seconds."))
        }

        val targetCatalogDesc = phrase.targetPhonemeIds.mapNotNull { id ->
            GenAmCatalog.phonemeById[id]?.let { "- ${it.id}: ${it.ipa} (${it.name})" }
        }.joinToString("\n")

        val prompt = """
            You are an expert General American English (GenAm) phonetician and acoustic pronunciation coach.
            Listen carefully to the user's audio recording shadowing the target sentence below.
            Evaluate authentic acoustic clarity—detect real vowel quality, dental /θ/ and /ð/ frication, American rhoticity (/ɹ/, /ɝ/), flap /ɾ/, dark /ɫ/, syllable stress, and rhythm.
            If the audio is silent, noise-only, or completely unrelated speech, score honestly low (under 30) and explain what was heard.

            Target Sentence: "${phrase.text}"
            Expected GenAm IPA: ${phrase.ipa}
            Shadowing Speed Tier: ${speed.label} (${speed.caption})
            Target Phoneme Catalog IDs to score in phonemeResults (use these exact phonemeId keys):
            $targetCatalogDesc

            Provide:
            - Realistic scores (0-100) for overallScore, phonemeAccuracy, rhythmScore, flowScore.
            - Concrete, encouraging physical mouth/tongue/lip/airflow coaching tips.
            - Word breakdown for the 5-8 most informative content words in the sentence, linking each tricky word to its matching catalog phonemeId when relevant.
            - Set shouldStepUpSpeed = true only if overallScore >= 85.
        """.trimIndent()

        return try {
            val response = withTimeout(60_000L) {
                evaluationModel.generateContent(
                    content {
                        inlineData(wavBytes, "audio/wav")
                        text(prompt)
                    }
                )
            }
            val rawJson = response.text?.takeIf { it.isNotBlank() }
                ?: return Result.failure(IllegalStateException("AI acoustic evaluator returned an empty response."))

            Result.success(parseEvaluationJson(rawJson, phrase))
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Acoustic evaluation timed out", e)
            Result.failure(Exception("Acoustic evaluation timed out after 60 seconds. Please check your connection and try again."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Firebase AI acoustic evaluation failed", e)
            Result.failure(Exception(e.localizedMessage ?: "Acoustic evaluation failed. Please retry."))
        }
    }

    suspend fun generatePersonalizedDrill(
        focusPhonemes: List<GenAmPhoneme>,
        recentIssueNotes: List<String>
    ): Result<PracticePhrase> {
        if (!isFirebaseInitialized()) {
            return Result.failure(
                IllegalStateException(
                    "Geração de exercícios por IA requer o secret GOOGLE_SERVICES_JSON configurado no GitHub Actions."
                )
            )
        }
        val selected = focusPhonemes.take(3).ifEmpty { GenAmCatalog.phonemes.take(2) }
        val targetList = selected.joinToString("\n") { "- ${it.id}: ${it.ipa} (${it.name}), example: '${it.exampleWord}'" }
        val notesBlock = recentIssueNotes.filter { it.isNotBlank() }.take(3).joinToString("\n") { "- $it" }

        val prompt = """
            Create ONE fresh, natural, everyday General American English practice sentence (10 to 15 words) for shadowing practice.
            It must naturally weave together these target phonemes multiple times without sounding like a silly tongue twister:
            $targetList

            Learner's recent coaching notes to address:
            ${notesBlock.ifBlank { "- Focus on clean General American articulation and steady rhythm." }}

            Return valid JSON strictly matching the schema.
        """.trimIndent()

        return try {
            val response = withTimeout(45_000L) {
                drillGeneratorModel.generateContent(prompt)
            }
            val rawJson = response.text?.takeIf { it.isNotBlank() }
                ?: return Result.failure(IllegalStateException("AI drill generator returned an empty response."))

            val json = JSONObject(rawJson)
            val title = json.optString("title", "Targeted GenAm Drill").take(48)
            val text = json.optString("text", "").trim()
            if (text.isBlank()) {
                return Result.failure(IllegalStateException("Generated drill sentence was empty."))
            }
            val ipa = json.optString("ipa", "").ifBlank { "/.../" }
            val stressGuide = json.optString("stressGuide", text)
            val difficulty = json.optString("difficulty", "Core")

            val drill = PracticePhrase(
                id = "ai_drill_${System.currentTimeMillis()}",
                title = title,
                text = text,
                ipa = ipa,
                stressGuide = stressGuide,
                targetPhonemeIds = selected.map { it.id },
                difficulty = difficulty,
                category = selected.first().category,
                generatedByAi = true
            )
            Result.success(drill)
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Custom drill generation timed out", e)
            Result.failure(Exception("Drill generation timed out. Please try again."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Custom drill generation failed", e)
            Result.failure(Exception(e.localizedMessage ?: "Failed to generate personalized drill."))
        }
    }

    private fun parseEvaluationJson(rawJson: String, phrase: PracticePhrase): AcousticEvaluation {
        val json = JSONObject(rawJson)
        val overall = json.optInt("overallScore", 70).coerceIn(0, 100)
        val phonemeAcc = json.optInt("phonemeAccuracy", overall).coerceIn(0, 100)
        val rhythm = json.optInt("rhythmScore", overall).coerceIn(0, 100)
        val flow = json.optInt("flowScore", overall).coerceIn(0, 100)
        val summary = json.optString("coachingSummary", "Acoustic analysis complete.").take(600)
        val nextStep = json.optString("nextStepRecommendation", "Repeat at your current tempo to lock in clarity.").take(300)
        val shouldStepUp = json.optBoolean("shouldStepUpSpeed", overall >= 85)

        val wordsArray = json.optJSONArray("wordBreakdown")
        val words = mutableListOf<WordEvaluation>()
        if (wordsArray != null) {
            for (i in 0 until wordsArray.length()) {
                val obj = wordsArray.optJSONObject(i) ?: continue
                val statusRaw = obj.optString("status", "CLOSE").uppercase()
                val tier = ClarityTier.entries.firstOrNull { it.name == statusRaw } ?: ClarityTier.CLOSE
                val pId = obj.optString("phonemeId", "").takeIf { it.isNotBlank() && GenAmCatalog.phonemeById.containsKey(it) }
                words.add(
                    WordEvaluation(
                        word = obj.optString("word", ""),
                        expectedIpa = obj.optString("expectedIpa", ""),
                        status = tier,
                        detectedIssue = obj.optString("detectedIssue", ""),
                        phonemeId = pId
                    )
                )
            }
        }

        val phonemesArray = json.optJSONArray("phonemeResults")
        val phonemeResults = mutableListOf<PhonemeEvaluationResult>()
        if (phonemesArray != null) {
            for (i in 0 until phonemesArray.length()) {
                val obj = phonemesArray.optJSONObject(i) ?: continue
                val pid = obj.optString("phonemeId", "")
                val catalogMatch = GenAmCatalog.phonemeById[pid] ?: continue
                phonemeResults.add(
                    PhonemeEvaluationResult(
                        phonemeId = catalogMatch.id,
                        ipa = obj.optString("ipa", catalogMatch.ipa).ifBlank { catalogMatch.ipa },
                        score = obj.optInt("score", phonemeAcc).coerceIn(0, 100),
                        physicalTip = obj.optString("physicalTip", catalogMatch.placementCue).take(300)
                    )
                )
            }
        }

        if (phonemeResults.isEmpty()) {
            phrase.targetPhonemeIds.mapNotNull { GenAmCatalog.phonemeById[it] }.forEach { p ->
                phonemeResults.add(
                    PhonemeEvaluationResult(
                        phonemeId = p.id,
                        ipa = p.ipa,
                        score = phonemeAcc,
                        physicalTip = p.placementCue
                    )
                )
            }
        }

        return AcousticEvaluation(
            overallScore = overall,
            phonemeAccuracy = phonemeAcc,
            rhythmScore = rhythm,
            flowScore = flow,
            coachingSummary = summary,
            nextStepRecommendation = nextStep,
            shouldStepUpSpeed = shouldStepUp,
            wordBreakdown = words,
            phonemeResults = phonemeResults
        )
    }
}
