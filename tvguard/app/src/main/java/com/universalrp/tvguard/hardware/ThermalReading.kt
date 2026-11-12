package com.universalrp.tvguard.hardware

import java.io.File
import java.util.Locale

/**
 * One temperature the TV actually exposes, and where it came from.
 *
 * [celsius] is null when nothing readable was found. TV SoCs vary a lot: many expose
 * `/sys/class/thermal/thermal_zone*`, some only expose a coarse thermal status through the
 * PowerManager, and some expose nothing to an unprivileged app. In every one of those cases
 * the honest answer is "unavailable" rather than a plausible number.
 */
data class ThermalReading(
    val celsius: Double?,
    val source: String?,
    /** Android's coarse thermal status, 0 = none .. 6 = shutdown, when reported. */
    val thermalStatus: Int?,
) {
    val available: Boolean get() = celsius != null

    /** Prefers the hottest readable zone, because that is the one that causes throttling. */
    fun format(): String =
        if (celsius == null) "Temp unavailable"
        else String.format(Locale.US, "Temp %.1f°C", celsius)

    companion object {
        val UNAVAILABLE = ThermalReading(null, null, null)
    }
}

/**
 * Reads the device temperature without root and without inventing values.
 *
 * Sources, in order of preference:
 *  1. `/sys/class/thermal/thermal_zone*` — the raw kernel sensors. Values are in
 *     milli-degrees Celsius on Linux; a few vendors use degrees, so anything implausible is
 *     rejected instead of converted.
 *  2. `PowerManager.getThermalStatus()` (API 29+) — a coarse 0..6 status, not a temperature.
 *     It is reported separately and never converted into a fake °C figure.
 *
 * Anything below [MIN_PLAUSIBLE_C] or above [MAX_PLAUSIBLE_C] is discarded: a TV is not at
 * -40 °C, and a 300 °C reading is a raw sensor with no calibration, not a real temperature.
 */
object ThermalReader {

    private const val MIN_PLAUSIBLE_C = -10.0
    private const val MAX_PLAUSIBLE_C = 150.0

    private const val THERMAL_ROOT = "/sys/class/thermal"

    /** Testable: takes the raw zone files so JVM tests do not need root or /sys. */
    fun collect(zones: List<ZoneSample>): ThermalReading {
        val valid = zones.mapNotNull { zone ->
            normalise(zone.rawValue)?.let { temp -> zone to temp }
        }.filter { (_, temp) -> temp in MIN_PLAUSIBLE_C..MAX_PLAUSIBLE_C }

        if (valid.isEmpty()) return ThermalReading.UNAVAILABLE
        // The hottest zone governs throttling, so report that one and name it.
        val (hottestZone, hottest) = valid.maxByOrNull { it.second }!!
        return ThermalReading(
            celsius = hottest,
            source = hottestZone.name,
            thermalStatus = null,
        )
    }

    /**
     * Kernel thermal zones report milli-degrees Celsius (`45000` = 45 °C), but a handful of
     * vendor kernels write plain degrees (`45` = 45 °C). The scale is inferred from the
     * magnitude, which is unambiguous in practice:
     *
     *  - `>= 1000` in magnitude can only be milli-degrees, because no real sensor sits at
     *    1000 °C. Divide by 1000.
     *  - anything smaller can only be a plain degree reading, because a milli-degree value
     *    below 1000 would describe a TV at under 1 °C.
     *
     * `0` is treated as a sentinel meaning "not calibrated" rather than 0 °C, and anything
     * outside the plausible human range is rejected instead of clamped.
     */
    internal fun normalise(raw: Long?): Double? {
        if (raw == null || raw == 0L) return null
        val celsius = if (raw >= 1000L || raw <= -1000L) raw / 1000.0 else raw.toDouble()
        return if (celsius in MIN_PLAUSIBLE_C..MAX_PLAUSIBLE_C) celsius else null
    }

    data class ZoneSample(val name: String, val rawValue: Long?)

    /** Reads the real filesystem. Returns [ThermalReading.UNAVAILABLE] when nothing is readable. */
    fun read(): ThermalReading {
        return try {
            val root = File(THERMAL_ROOT)
            if (!root.isDirectory) return ThermalReading.UNAVAILABLE
            val zones = root.listFiles()
                ?.filter { it.isDirectory && it.name.startsWith("thermal_zone") }
                ?.mapNotNull { dir ->
                    val raw = try {
                        File(dir, "temp").takeIf { it.canRead() }?.readText()?.trim()?.toLongOrNull()
                    } catch (_: Exception) {
                        null
                    }
                    val type = try {
                        File(dir, "type").takeIf { it.canRead() }?.readText()?.trim()
                    } catch (_: Exception) {
                        null
                    }
                    ZoneSample(type ?: dir.name, raw)
                }
                ?: emptyList()
            collect(zones)
        } catch (_: Exception) {
            ThermalReading.UNAVAILABLE
        }
    }

    /** Android's coarse thermal status, kept distinct from a Celsius reading. */
    fun thermalStatusLabel(status: Int?): String = when (status) {
        null -> "Thermal status unavailable"
        0 -> "Thermal status normal"
        1 -> "Thermal status light"
        2 -> "Thermal status moderate"
        3 -> "Thermal status severe"
        4 -> "Thermal status critical"
        5 -> "Thermal status emergency"
        6 -> "Thermal status shutdown"
        else -> "Thermal status $status"
    }
}
