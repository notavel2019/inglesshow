package com.example.data

import android.util.Log
import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
import kotlinx.coroutines.CancellationException
import org.json.JSONArray
import org.json.JSONObject

@Keep
@IgnoreExtraProperties
data class LearnerProfileDoc(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val currentStreakDays: Long = 1L,
    val totalSessions: Long = 0L,
    val overallClarityScore: Long = 0L,
    val preferredStartSpeed: Double = 0.80,
    val focusPhonemeIds: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class PhonemeMasteryDoc(
    @DocumentId val id: String = "",
    val phonemeId: String = "",
    val ipaSymbol: String = "",
    val category: String = "",
    val masteryScore: Long = 0L,
    val attemptCount: Long = 0L,
    val recommendedSpeed: Double = 0.80,
    val recentIssueNote: String? = null,
    @ServerTimestamp val lastPracticedAt: Timestamp? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class PracticeSessionDoc(
    @DocumentId val id: String = "",
    val phraseId: String = "",
    val phraseText: String = "",
    val targetPhonemes: List<String> = emptyList(),
    val speedMultiplier: Double = 0.80,
    val overallScore: Long = 0L,
    val phonemeAccuracy: Long = 0L,
    val rhythmScore: Long = 0L,
    val flowScore: Long = 0L,
    val coachingSummary: String = "",
    val nextStepRecommendation: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

enum class OperationType(val value: String) {
    CREATE("create"),
    UPDATE("update"),
    DELETE("delete"),
    LIST("list"),
    GET("get"),
    WRITE("write"),
}

fun handleFirestoreError(
    exception: Throwable,
    operationType: OperationType,
    path: String?
): String {
    val causes = generateSequence(exception) { it.cause }
    val rootError = causes.firstOrNull { it is FirebaseFirestoreException }
        ?: causes.firstOrNull { it !is CancellationException }
        ?: exception

    val currentUser = runCatching { FirebaseAuth.getInstance().currentUser }.getOrNull()

    val providerInfoList = currentUser?.providerData?.map { provider ->
        JSONObject().apply {
            put("providerId", provider.providerId)
            put("email", provider.email)
        }
    } ?: emptyList()

    val authInfoJson = JSONObject().apply {
        put("userId", currentUser?.uid)
        put("email", currentUser?.email)
        put("emailVerified", currentUser?.isEmailVerified)
        put("tenantId", currentUser?.tenantId)
        put("providerInfo", JSONArray(providerInfoList))
    }

    val errorInfoJson = JSONObject().apply {
        put("error", rootError.message ?: rootError.toString())
        put("operationType", operationType.value)
        put("path", path)
        put("authInfo", authInfoJson)
    }

    val jsonString = errorInfoJson.toString()
    Log.e("FirestoreError", "Firestore Error: $jsonString")
    return jsonString
}
