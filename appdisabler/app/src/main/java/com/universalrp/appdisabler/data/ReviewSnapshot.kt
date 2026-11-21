package com.universalrp.appdisabler.data

import android.app.ActivityManager
import android.content.Context
import android.os.Environment
import android.os.StatFs
import org.json.JSONArray
import org.json.JSONObject

/** Observations only: not an APK/data backup, nor a promise of restoration. */
internal data class PackageObservation(val name: String, val enabled: Boolean, val suspended: Boolean?)
internal data class DeviceSnapshot(val capturedAt: Long, val totalRam: Long, val availableRam: Long,
    val totalStorage: Long, val availableStorage: Long, val packages: List<PackageObservation>)
internal data class SnapshotChanges(val added: List<String>, val missing: List<String>,
    val enabledChanged: List<String>, val suspensionChanged: List<String>, val suspensionUnknown: Int)

internal fun compareSnapshots(before: DeviceSnapshot, after: DeviceSnapshot): SnapshotChanges {
    val old = before.packages.associateBy { it.name }; val now = after.packages.associateBy { it.name }
    val common = old.keys intersect now.keys
    return SnapshotChanges((now.keys - old.keys).sorted(), (old.keys - now.keys).sorted(),
        common.filter { old.getValue(it).enabled != now.getValue(it).enabled }.sorted(),
        common.filter { old.getValue(it).suspended != null && now.getValue(it).suspended != null &&
            old.getValue(it).suspended != now.getValue(it).suspended }.sorted(),
        common.count { old.getValue(it).suspended == null || now.getValue(it).suspended == null })
}

internal object ReviewSnapshots {
    private const val PREFS = "review_baseline"
    private const val KEY = "snapshot_v1"
    fun capture(context: Context, apps: List<AppInfo>): DeviceSnapshot {
        require(apps.isNotEmpty()) { "App list is not ready" }
        val memory = ActivityManager.MemoryInfo()
        (context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager).getMemoryInfo(memory)
        val disk = StatFs(Environment.getDataDirectory().path)
        return DeviceSnapshot(System.currentTimeMillis(), memory.totalMem, memory.availMem,
            disk.totalBytes, disk.availableBytes, apps.map {
                PackageObservation(it.packageName, it.isEnabled, if (it.isSuspensionKnown) it.isSuspended else null)
            }.sortedBy { it.name })
    }
    fun encode(s: DeviceSnapshot): String {
        val items = JSONArray()
        s.packages.forEach { p -> items.put(JSONObject().put("package", p.name).put("enabled", p.enabled)
            .put("suspended", p.suspended ?: JSONObject.NULL)) }
        return JSONObject().put("format", 1).put("capturedAtMillis", s.capturedAt)
            .put("ramTotalBytes", s.totalRam).put("ramAvailableBytes", s.availableRam)
            .put("storageTotalBytes", s.totalStorage).put("storageAvailableBytes", s.availableStorage)
            .put("note", "Package state inventory only; not APKs, app data, a recovery script or proof of restore.")
            .put("packages", items).toString(2)
    }
    fun decode(json: String): DeviceSnapshot {
        val o = JSONObject(json); require(o.getInt("format") == 1)
        val arr = o.getJSONArray("packages"); require(arr.length() in 1..10000)
        val packages = (0 until arr.length()).map { i ->
            val p = arr.getJSONObject(i)
            PackageObservation(p.getString("package"), p.getBoolean("enabled"),
                if (p.isNull("suspended")) null else p.getBoolean("suspended"))
        }
        require(packages.map { it.name }.toSet().size == packages.size)
        return DeviceSnapshot(o.getLong("capturedAtMillis"), o.getLong("ramTotalBytes"), o.getLong("ramAvailableBytes"),
            o.getLong("storageTotalBytes"), o.getLong("storageAvailableBytes"), packages)
    }
    fun load(context: Context): DeviceSnapshot? = runCatching {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, null) ?: return null
        decode(raw)
    }.getOrNull()
    fun save(context: Context, snapshot: DeviceSnapshot): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY, encode(snapshot)).commit()
}
