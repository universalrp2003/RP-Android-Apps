package com.universalrp.tamilanstore

import java.net.URI
import org.json.JSONObject

data class StoreApp(val id: String, val name: String, val tamilName: String, val packageName: String,
    val version: String, val url: String, val size: Long, val sha256: String, val releaseUrl: String)

object Catalog {
    const val SIGNER = "7366d76dc5a9032424f032f1530f661366d880af37657374df85ca7a6299747d"
    // Matches tools/prepare_release_signing.py and the immutable replacement releases.
    // Never accept the retired certificate as an alternative for new archives.
    // Secret Message has its own long-standing release key, not the legacy batch key.
    fun signerFor(packageName: String): String = if (packageName == "com.rpideas.secretmessage")
        "58e370e417cf6f33234689acd5bf379474f5a8c55f79181d9e892a321bc04f08" else SIGNER
    const val WEBSITE = "https://universalrp2003.github.io/RP-Android-Apps/"
    /**
     * Fifteen apps is the bundled contract: a Store build ships exactly the suite it was signed for.
     * A live feed is validated by [LiveCatalog] instead, where the id set must match this snapshot.
     */
    const val EXPECTED_APPS = 15
    fun parse(text: String): List<StoreApp> = parse(text, EXPECTED_APPS)
    /**
     * A live feed may be shorter than this build's snapshot for a while after an app is added: the
     * website is regenerated from public releases on its own deployment, and a phone that refreshes
     * in that window must keep working on what it already has instead of calling the feed invalid.
     */
    fun parseLive(text: String): List<StoreApp> {
        val root = JSONObject(text)
        require(root.getInt("schema") == 1)
        val count = root.getJSONArray("apps").length()
        require(count in 1..EXPECTED_APPS) { "live catalog must not exceed the bundled contract" }
        return parse(text, count)
    }
    /** Used for the frozen legacy endpoints, which keep serving their original twelve entries. */
    fun parse(text: String, expected: Int): List<StoreApp> {
        val root = JSONObject(text)
        require(root.getInt("schema") == 1)
        val entries = root.getJSONArray("apps")
        require(entries.length() == expected)
        val apps = (0 until entries.length()).map { i ->
            val a = entries.getJSONObject(i)
            StoreApp(a.getString("id"), a.getString("name"), a.getString("tamilName"),
                a.getString("packageName"), a.getString("version"), a.getString("url"),
                a.getLong("size"), a.getString("sha256"), a.getString("releaseUrl")).also {
                val uri = URI(it.url)
                require(uri.scheme == "https" && uri.host == "github.com" && uri.port == -1 && uri.rawUserInfo == null && uri.rawQuery == null && uri.fragment == null)
                require(uri.path.startsWith("/universalrp2003/RP-Android-Apps/releases/download/") && uri.path.endsWith(".apk"))
                require(it.sha256.matches(Regex("[a-f0-9]{64}")))
                require(it.size in 1..200_000_000L)
                require(it.packageName.matches(Regex("[a-zA-Z][a-zA-Z0-9_]*(\\.[a-zA-Z][a-zA-Z0-9_]*)+")))
                require(it.id.matches(Regex("[a-z][a-z0-9]*")))
                require(it.version.matches(Regex("[0-9]+(?:\\.[0-9]+){1,3}")))
                val release = URI(it.releaseUrl)
                require(release.scheme == "https" && release.host == "github.com" && release.port == -1 && release.rawUserInfo == null && release.rawQuery == null && release.rawFragment == null)
                require(release.path.matches(Regex("/universalrp2003/RP-Android-Apps/releases/tag/[a-z0-9-]+-v[0-9]+(?:\\.[0-9]+){1,3}")))
                val releaseTag = release.path.substringAfterLast('/')
                require(releaseTag.endsWith("-v${it.version}"))
                require(uri.path.substringAfter("/releases/download/").substringBefore('/') == releaseTag)
            }
        }
        require(apps.map { it.id }.distinct().size == expected && apps.map { it.packageName }.distinct().size == expected)
        return apps
    }
    fun allowedDownloadHost(host: String) = host == "github.com" || host == "release-assets.githubusercontent.com" || host == "objects.githubusercontent.com"
}
