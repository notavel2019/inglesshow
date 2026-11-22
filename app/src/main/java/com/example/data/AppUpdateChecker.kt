package com.example.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

private const val TAG = "AppUpdateChecker"
private const val PREFS_NAME = "voxform_update_prefs"
private const val KEY_GITHUB_REPO = "github_repo_slug"
private val REPO_SLUG_REGEX = Regex("^[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+$")

data class AppUpdateInfo(
    val tagName: String,
    val releaseTitle: String,
    val releaseNotes: String,
    val apkDownloadUrl: String,
    val releasePageUrl: String,
    val apkSizeBytes: Long,
    val updateAvailable: Boolean
)

/**
 * Checks GitHub Releases for the latest VoxForm APK build and opens the validated
 * HTTPS download URL via Intent.ACTION_VIEW (zero restricted permissions).
 */
class AppUpdateChecker(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getConfiguredRepo(): String {
        val saved = prefs.getString(KEY_GITHUB_REPO, null)?.trim().orEmpty()
        if (saved.isNotBlank()) return saved
        return BuildConfig.GITHUB_REPO.trim()
    }

    fun saveConfiguredRepo(rawInput: String): Result<String> {
        val cleaned = normalizeRepoSlug(rawInput)
        if (!REPO_SLUG_REGEX.matches(cleaned)) {
            return Result.failure(
                IllegalArgumentException("Use o formato usuario/repositorio (ex: meuusuario/voxform).")
            )
        }
        prefs.edit().putString(KEY_GITHUB_REPO, cleaned).apply()
        return Result.success(cleaned)
    }

    suspend fun checkLatestRelease(repoOverride: String? = null): Result<AppUpdateInfo> = withContext(Dispatchers.IO) {
        val repo = normalizeRepoSlug(repoOverride ?: getConfiguredRepo())
        if (!REPO_SLUG_REGEX.matches(repo)) {
            return@withContext Result.failure(
                IllegalStateException("Configure seu repositório GitHub (usuario/repositorio) para verificar atualizações.")
            )
        }

        val apiUrl = "https://api.github.com/repos/$repo/releases/latest"
        var connection: HttpURLConnection? = null
        try {
            connection = (URL(apiUrl).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 15_000
                readTimeout = 15_000
                setRequestProperty("Accept", "application/vnd.github+json")
                setRequestProperty("User-Agent", "VoxForm-Android/${BuildConfig.VERSION_NAME}")
            }

            val status = connection.responseCode
            if (status == HttpURLConnection.HTTP_NOT_FOUND) {
                return@withContext Result.failure(
                    IllegalStateException("Nenhuma release encontrada em $repo. Faça push no GitHub para o Actions gerar o primeiro APK.")
                )
            }
            if (status !in 200..299) {
                return@withContext Result.failure(
                    IllegalStateException("GitHub retornou erro HTTP $status ao buscar atualizações.")
                )
            }

            val body = connection.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(body)
            val tagName = json.optString("tag_name", "").trim()
            val releaseTitle = json.optString("name", tagName).ifBlank { tagName }
            val releaseNotes = json.optString("body", "").trim().take(500)
            val releasePageUrl = json.optString("html_url", "https://github.com/$repo/releases/latest")

            var apkUrl = ""
            var apkSize = 0L
            val assets = json.optJSONArray("assets")
            if (assets != null) {
                for (i in 0 until assets.length()) {
                    val asset = assets.optJSONObject(i) ?: continue
                    val name = asset.optString("name", "")
                    val downloadUrl = asset.optString("browser_download_url", "")
                    if (name.endsWith(".apk", ignoreCase = true) && isTrustedGithubHttpsUrl(downloadUrl)) {
                        apkUrl = downloadUrl
                        apkSize = asset.optLong("size", 0L)
                        break
                    }
                }
            }

            val targetUrl = when {
                apkUrl.isNotBlank() -> apkUrl
                isTrustedGithubHttpsUrl(releasePageUrl) -> releasePageUrl
                else -> "https://github.com/$repo/releases/latest"
            }

            val newer = isRemoteVersionNewer(
                remoteTag = tagName,
                currentVersionName = BuildConfig.VERSION_NAME,
                currentVersionCode = BuildConfig.VERSION_CODE
            )

            Result.success(
                AppUpdateInfo(
                    tagName = tagName.ifBlank { "latest" },
                    releaseTitle = releaseTitle.ifBlank { "VoxForm Latest" },
                    releaseNotes = releaseNotes,
                    apkDownloadUrl = targetUrl,
                    releasePageUrl = releasePageUrl,
                    apkSizeBytes = apkSize,
                    updateAvailable = newer
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Failed to check GitHub releases", e)
            Result.failure(Exception("Não foi possível verificar atualizações: ${e.localizedMessage ?: "sem conexão"}"))
        } finally {
            connection?.disconnect()
        }
    }

    fun openApkDownload(context: Context, url: String): Boolean {
        if (!isTrustedGithubHttpsUrl(url)) {
            Log.w(TAG, "Blocked untrusted update URL: $url")
            return false
        }
        return try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open APK download URL", e)
            false
        }
    }

    companion object {
        fun normalizeRepoSlug(raw: String): String {
            return raw.trim()
                .removePrefix("https://github.com/")
                .removePrefix("http://github.com/")
                .removePrefix("github.com/")
                .removeSuffix(".git")
                .trim('/')
        }

        fun isTrustedGithubHttpsUrl(url: String): Boolean {
            val parsed = runCatching { Uri.parse(url) }.getOrNull() ?: return false
            if (!parsed.scheme.equals("https", ignoreCase = true)) return false
            val host = parsed.host?.lowercase() ?: return false
            return host == "github.com" ||
                host.endsWith(".github.com") ||
                host == "githubusercontent.com" ||
                host.endsWith(".githubusercontent.com")
        }

        fun isRemoteVersionNewer(
            remoteTag: String,
            currentVersionName: String,
            currentVersionCode: Int
        ): Boolean {
            val remoteParts = extractNumericParts(remoteTag)
            val currentParts = extractNumericParts(currentVersionName)
            if (remoteParts.isEmpty()) return false

            val maxLen = maxOf(remoteParts.size, currentParts.size)
            for (i in 0 until maxLen) {
                val r = remoteParts.getOrElse(i) { 0 }
                val c = currentParts.getOrElse(i) { 0 }
                if (r > c) return true
                if (r < c) return false
            }
            // Fallback check if single numeric build tag exceeds versionCode
            return remoteParts.size == 1 && remoteParts[0] > currentVersionCode
        }

        private fun extractNumericParts(version: String): List<Int> {
            return Regex("\\d+")
                .findAll(version)
                .mapNotNull { it.value.toIntOrNull() }
                .toList()
        }
    }
}
