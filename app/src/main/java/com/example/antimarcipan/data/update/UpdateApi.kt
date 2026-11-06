package com.example.antimarcipan.data.update

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.GET
import retrofit2.http.Path

/**
 * GitHub Releases REST API — používá se jen pro kontrolu aktualizací.
 * Veřejné repo, nevyžaduje token.
 * Docs: https://docs.github.com/en/rest/releases/releases
 */
interface UpdateApi {

    @GET("repos/{owner}/{repo}/releases/tags/{tag}")
    suspend fun releaseByTag(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("tag") tag: String,
    ): GithubRelease
}

@Serializable
data class GithubRelease(
    @SerialName("tag_name") val tagName: String? = null,
    val name: String? = null,
    val body: String? = null,
    /** Commit, ze kterého je release sestaven — slouží k porovnání verzí. */
    @SerialName("target_commitish") val targetCommitish: String? = null,
    @SerialName("published_at") val publishedAt: String? = null,
    val assets: List<GithubAsset> = emptyList(),
) {
    fun apkAsset(): GithubAsset? =
        assets.firstOrNull { it.name?.endsWith(".apk", ignoreCase = true) == true }

    /** Obsah version.json, pokud ho release nese jako asset (přesnější než commitish). */
    fun versionAsset(): GithubAsset? =
        assets.firstOrNull { it.name.equals("version.json", ignoreCase = true) }
}

@Serializable
data class GithubAsset(
    val name: String? = null,
    val size: Long = 0,
    @SerialName("browser_download_url") val browserDownloadUrl: String? = null,
)

/** Obsah version.json přiloženého k release. */
@Serializable
data class VersionInfo(
    val sha: String? = null,
    val versionCode: Int? = null,
    val versionName: String? = null,
    val builtAt: String? = null,
)
