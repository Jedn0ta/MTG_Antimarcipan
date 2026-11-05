package com.example.antimarcipan.data.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.core.content.FileProvider
import com.example.antimarcipan.BuildConfig
import com.example.antimarcipan.data.Network
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import java.io.File

sealed interface UpdateStatus {
    /** Běží poslední verze. */
    data class UpToDate(val sha: String) : UpdateStatus
    /** Je k dispozici novější build. */
    data class Available(
        val latestSha: String,
        val versionName: String?,
        val notes: String?,
        val downloadUrl: String,
        val sizeBytes: Long,
    ) : UpdateStatus
    /** Kontrolu se nepodařilo provést. */
    data class Error(val message: String) : UpdateStatus
}

/**
 * In-app aktualizace z GitHub Release. Porovná commit aktuálního buildu
 * (BuildConfig.GIT_SHA) s commitem posledního release a případně stáhne
 * a spustí instalaci nového APK.
 */
class UpdateRepository(
    private val context: Context,
    private val api: UpdateApi = Network.github,
) {
    private val repo = BuildConfig.UPDATE_REPO       // "owner/repo"
    private val tag = BuildConfig.UPDATE_TAG         // "latest-debug"
    private val currentSha = BuildConfig.GIT_SHA

    suspend fun check(): UpdateStatus = withContext(Dispatchers.IO) {
        runCatching {
            val (owner, name) = repo.split("/").let { it[0] to it[1] }
            val release = api.releaseByTag(owner, name, tag)
            val apk = release.apkAsset()
                ?: return@runCatching UpdateStatus.Error("Release nemá přiložené APK.")

            val latestSha = resolveLatestSha(release)

            if (isSameBuild(latestSha)) {
                UpdateStatus.UpToDate(shortSha(currentSha))
            } else {
                UpdateStatus.Available(
                    latestSha = shortSha(latestSha),
                    versionName = release.name ?: release.tagName,
                    notes = release.body?.takeIf { it.isNotBlank() },
                    downloadUrl = apk.browserDownloadUrl.orEmpty(),
                    sizeBytes = apk.size,
                )
            }
        }.getOrElse { e ->
            UpdateStatus.Error(e.message ?: "Neznámá chyba při kontrole aktualizace.")
        }
    }

    /** Stáhne APK do cache a reportuje průběh 0f..1f (−1f = neznámá velikost). */
    suspend fun download(url: String, onProgress: (Float) -> Unit): File =
        withContext(Dispatchers.IO) {
            val dir = File(context.cacheDir, "updates").apply { mkdirs() }
            val outFile = File(dir, "antimarcipan-update.apk")
            if (outFile.exists()) outFile.delete()

            val request = Request.Builder().url(url).build()
            Network.httpClient.newCall(request).execute().use { resp ->
                check(resp.isSuccessful) { "Stažení selhalo (HTTP ${resp.code})" }
                val body = resp.body ?: error("Prázdná odpověď při stahování.")
                val total = body.contentLength()
                body.byteStream().use { input ->
                    outFile.outputStream().use { output ->
                        val buffer = ByteArray(64 * 1024)
                        var read: Int
                        var downloaded = 0L
                        while (input.read(buffer).also { read = it } != -1) {
                            output.write(buffer, 0, read)
                            downloaded += read
                            onProgress(if (total > 0) downloaded.toFloat() / total else -1f)
                        }
                    }
                }
            }
            outFile
        }

    /** Spustí systémový instalátor nad staženým APK. */
    fun install(apk: File) {
        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            apk,
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    /** Smí aplikace instalovat APK z neznámých zdrojů? (Android O+) */
    fun canInstallUnknownApps(): Boolean =
        context.packageManager.canRequestPackageInstalls()

    /** Intent na systémové nastavení „instalace neznámých aplikací" pro tuto appku. */
    fun unknownSourcesSettingsIntent(): Intent =
        Intent(
            Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
            Uri.parse("package:${context.packageName}"),
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    val currentShortSha: String get() = shortSha(currentSha)

    // --- helpers ---

    private suspend fun resolveLatestSha(release: GithubRelease): String {
        // Přesnější je version.json asset; jinak padneme na target_commitish.
        release.versionAsset()?.browserDownloadUrl?.let { url ->
            runCatching {
                val request = Request.Builder().url(url).build()
                Network.httpClient.newCall(request).execute().use { resp ->
                    val txt = resp.body?.string().orEmpty()
                    val info = Network.jsonParser.decodeFromString(VersionInfo.serializer(), txt)
                    info.sha
                }
            }.getOrNull()?.let { return it }
        }
        return release.targetCommitish.orEmpty()
    }

    private fun isSameBuild(latestSha: String): Boolean {
        if (currentSha == "dev" || latestSha.isBlank()) return false
        // Porovnání tolerantní ke zkráceným SHA.
        val a = currentSha.lowercase()
        val b = latestSha.lowercase()
        return a.startsWith(b) || b.startsWith(a)
    }

    private fun shortSha(sha: String): String =
        if (sha.length >= 7 && sha != "dev") sha.take(7) else sha
}
