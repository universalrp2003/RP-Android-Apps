package com.universalrp.weather

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.os.Build
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.util.Locale

/**
 * Smart location resolver matching Oor Arivippu's `இலக்கு நெருங்கும் அறிவிப்பு` (`DestinationResolver`):
 *
 *  - **GPS ON + permission granted:** Automatically reads Android's passive/cached last-known fix
 *    (zero GPS battery drain!) and resolves its village/town label locally.
 *  - **GPS OFF (or no fix):** Automatically falls back to the user's saved PIN / place coordinates.
 *    If none has been saved yet, reports `LocationResolutionMode.NEEDS_SAVED_PLACE` so the UI asks
 *    once for PIN code (`622301`) or place + PIN (`alangudi 622301` / `ஆலங்குடி`) with a full
 *    district + state + PIN disambiguation list so two villages with the same name never collide.
 */
object WeatherPlaceResolver {

    private const val INDIA_SOUTH = 6.0
    private const val INDIA_WEST = 68.0
    private const val INDIA_NORTH = 37.5
    private const val INDIA_EAST = 97.5
    private const val MAX_RESULTS = 10
    private const val SAME_PLACE_KM = 1.2

    private val pinRegex = Regex("""(?<!\d)([1-9]\d{5})(?!\d)""")
    private val separators = Regex("""\s*[/\\|]+\s*""")
    private val whitespace = Regex("""\s+""")
    private val districtSuffix = Regex("""(?i)\s*(?:\bdistrict\b|\bdist\.?)\s*$|\s*மாவட்டம்\s*$""")

    data class ParsedPlaceQuery(
        val rawTrimmed: String,
        val placeText: String,
        val postalCode: String?,
    )

    /** Splits inputs like `"622301"`, `"alangudi 622301"`, `"ஆலங்குடி - 622301"`, `"pallapatti/karur"`. */
    fun parseQuery(raw: String): ParsedPlaceQuery {
        val cleaned = whitespace.replace(separators.replace(raw.trim(), ", "), " ")
            .trim { it.isWhitespace() || it == ',' || it == '-' }
        val pin = pinRegex.find(cleaned)?.groupValues?.get(1)
        val withoutPin = if (pin != null) {
            whitespace.replace(cleaned.replace(pin, " "), " ")
                .trim { it.isWhitespace() || it == ',' || it == '-' }
        } else {
            cleaned
        }
        return ParsedPlaceQuery(
            rawTrimmed = cleaned,
            placeText = withoutPin,
            postalCode = pin,
        )
    }

    fun isDeviceLocationTurnedOn(context: Context): Boolean {
        val manager = context.getSystemService(LocationManager::class.java) ?: return false
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            runCatching { manager.isLocationEnabled }.getOrDefault(false)
        } else {
            runCatching {
                manager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                    manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
            }.getOrDefault(false)
        }
    }

    fun hasFineLocationPermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    fun hasLocationPermission(context: Context): Boolean {
        val fine = hasFineLocationPermission(context)
        val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    private const val MAX_UI_CACHED_LOCATION_AGE_MS = 30 * 60_000L
    private const val FRESH_FIX_MAX_AGE_MS = 10 * 60_000L

    @SuppressLint("MissingPermission")
    fun resolveActiveLocation(
        context: Context,
        savedName: String,
        savedDetails: String,
        savedLat: Double,
        savedLon: Double,
        tamil: Boolean,
    ): ResolvedWeatherLocation {
        val gpsOn = isDeviceLocationTurnedOn(context)
        val permitted = hasLocationPermission(context)

        if (gpsOn && permitted) {
            val now = System.currentTimeMillis()
            val lastFix = bestCachedLocation(context)?.takeIf { loc ->
                loc.time <= 0L || (now - loc.time) <= MAX_UI_CACHED_LOCATION_AGE_MS
            }
            if (lastFix != null && lastFix.latitude.isFinite() && lastFix.longitude.isFinite()) {
                val label = reverseGeocodeLabel(context, lastFix.latitude, lastFix.longitude, tamil)
                return ResolvedWeatherLocation(
                    mode = LocationResolutionMode.GPS_AUTOMATIC,
                    gpsTurnedOn = true,
                    permissionGranted = true,
                    name = label.first,
                    details = label.second,
                    latitude = lastFix.latitude,
                    longitude = lastFix.longitude,
                )
            }
        }

        if (savedLat.isFinite() && savedLon.isFinite() && savedName.isNotBlank()) {
            return ResolvedWeatherLocation(
                mode = LocationResolutionMode.SAVED_PLACE_FALLBACK,
                gpsTurnedOn = gpsOn,
                permissionGranted = permitted,
                name = savedName,
                details = savedDetails,
                latitude = savedLat,
                longitude = savedLon,
            )
        }

        return ResolvedWeatherLocation(
            mode = LocationResolutionMode.NEEDS_SAVED_PLACE,
            gpsTurnedOn = gpsOn,
            permissionGranted = permitted,
        )
    }

    /**
     * Async wrapper used by workers and "Check now":
     * - Whenever OS Location is ON & permitted (including Approximate Mobile-Tower `ACCESS_COARSE_LOCATION`
     *   in Ramesh Radio!), refreshes stale cached fixes (> 10 min old) via fast cell-tower `NETWORK_PROVIDER`
     *   first so an old village (`Venavalkudi`) never sticks after moving to a new town (`ஆலங்குடி`).
     * - When OS Location is OFF (or no fix), uses the saved fallback place/PIN (`622301 ஆலங்குடி`).
     */
    @SuppressLint("MissingPermission")
    suspend fun resolveActiveLocationSuspend(
        context: Context,
        savedName: String,
        savedDetails: String,
        savedLat: Double,
        savedLon: Double,
        tamil: Boolean,
    ): ResolvedWeatherLocation = withContext(Dispatchers.IO) {
        val gpsOn = isDeviceLocationTurnedOn(context)
        val permitted = hasLocationPermission(context)
        if (gpsOn && permitted) {
            val now = System.currentTimeMillis()
            val cached = bestCachedLocation(context)
            val freshEnough = cached != null &&
                cached.latitude.isFinite() &&
                cached.longitude.isFinite() &&
                cached.time > 0L &&
                (now - cached.time) <= FRESH_FIX_MAX_AGE_MS
            val activeLoc = if (freshEnough) cached else (requestQuickFix(context) ?: cached)
            if (activeLoc != null && activeLoc.latitude.isFinite() && activeLoc.longitude.isFinite()) {
                val label = reverseGeocodeLabel(context, activeLoc.latitude, activeLoc.longitude, tamil)
                return@withContext ResolvedWeatherLocation(
                    mode = LocationResolutionMode.GPS_AUTOMATIC,
                    gpsTurnedOn = true,
                    permissionGranted = true,
                    name = label.first,
                    details = label.second,
                    latitude = activeLoc.latitude,
                    longitude = activeLoc.longitude,
                )
            }
        }
        resolveActiveLocation(context, savedName, savedDetails, savedLat, savedLon, tamil)
    }

    @SuppressLint("MissingPermission")
    private suspend fun requestQuickFix(context: Context): Location? {
        val manager = context.getSystemService(LocationManager::class.java) ?: return null
        val fineAllowed = hasFineLocationPermission(context)
        val provider = when {
            runCatching { manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) }.getOrDefault(false) ->
                LocationManager.NETWORK_PROVIDER
            fineAllowed && runCatching { manager.isProviderEnabled(LocationManager.GPS_PROVIDER) }.getOrDefault(false) ->
                LocationManager.GPS_PROVIDER
            else -> return null
        }
        return kotlinx.coroutines.withTimeoutOrNull(4_000L) {
            kotlinx.coroutines.suspendCancellableCoroutine { cont ->
                val listener = object : android.location.LocationListener {
                    override fun onLocationChanged(location: Location) {
                        runCatching { manager.removeUpdates(this) }
                        if (cont.isActive) cont.resumeWith(Result.success(location))
                    }
                    @Deprecated("Deprecated in Java")
                    override fun onStatusChanged(p: String?, s: Int, e: android.os.Bundle?) = Unit
                    override fun onProviderEnabled(p: String) = Unit
                    override fun onProviderDisabled(p: String) = Unit
                }
                val mainLooper = android.os.Looper.getMainLooper()
                runCatching {
                    manager.requestLocationUpdates(provider, 0L, 0f, listener, mainLooper)
                }.onFailure {
                    if (cont.isActive) cont.resumeWith(Result.success(null))
                }
                cont.invokeOnCancellation { runCatching { manager.removeUpdates(listener) } }
            }
        }
    }

    /**
     * Resolves matching villages/towns/PIN codes off the main thread using:
     *  1. India Post PIN directory (`api.postalpincode.in`) when a 6-digit PIN like `622301` is given,
     *  2. Android `Geocoder` in Tamil/English (same as Oor Arivippu `DestinationResolver`),
     *  3. Open-Meteo Geocoding API fallback so every ROM resolves coordinates reliably.
     */
    @Suppress("DEPRECATION")
    suspend fun searchCandidates(
        context: Context,
        query: String,
        tamil: Boolean,
    ): List<WeatherPlaceCandidate> = withContext(Dispatchers.IO) {
        val parsed = parseQuery(query)
        if (parsed.rawTrimmed.isBlank()) return@withContext emptyList()
        val districtWord = if (tamil) "மாவட்டம்" else "district"

        // Step 1: If user included a 6-digit Indian PIN (e.g. "622301" or "alangudi 622301"),
        // look up official post office + district + state so even pure PIN queries get real town names.
        val pinOffices: List<PinOfficeInfo> = parsed.postalCode?.let { pin ->
            runCatching { fetchIndiaPinOffices(pin) }.getOrDefault(emptyList())
        }.orEmpty()

        val rawCandidates = mutableListOf<WeatherPlaceCandidate>()

        // Step 2: Android system Geocoder (supports Tamil & English scripts directly on device).
        if (Geocoder.isPresent()) {
            val locale = if (tamil) Locale("ta", "IN") else Locale("en", "IN")
            val geocoder = Geocoder(context.applicationContext, locale)
            val queriesToTry = buildList {
                add(parsed.rawTrimmed)
                if (parsed.postalCode != null && pinOffices.isNotEmpty()) {
                    val primary = pinOffices.firstOrNull { office ->
                        parsed.placeText.isBlank() ||
                            office.name.contains(parsed.placeText, ignoreCase = true) ||
                            parsed.placeText.contains(office.name, ignoreCase = true)
                    } ?: pinOffices.first()
                    add("${primary.name}, ${primary.district}, ${primary.state} ${parsed.postalCode}")
                }
            }.distinct()

            for (q in queriesToTry) {
                val addresses = runCatching {
                    geocoder.getFromLocationName(q, MAX_RESULTS, INDIA_SOUTH, INDIA_WEST, INDIA_NORTH, INDIA_EAST)
                        ?.takeIf { it.isNotEmpty() }
                        ?: geocoder.getFromLocationName(q, MAX_RESULTS)
                }.getOrNull().orEmpty()

                for (addr in addresses) {
                    if (!addr.hasLatitude() || !addr.hasLongitude()) continue
                    if (!addr.latitude.isFinite() || !addr.longitude.isFinite()) continue
                    val fallbackPinOffice = pinOffices.firstOrNull()
                    val cleanName = firstCleanPlace(
                        addr.locality,
                        addr.subLocality,
                        addr.featureName,
                        fallbackPinOffice?.name,
                        addr.subAdminArea,
                    ).ifBlank { parsed.placeText.ifBlank { parsed.rawTrimmed } }
                    val effectivePin = addr.postalCode?.trim()?.takeIf { pinRegex.matches(it) }
                        ?: parsed.postalCode.orEmpty()
                    val district = addr.subAdminArea?.takeIf { it.isNotBlank() } ?: fallbackPinOffice?.district
                    val state = addr.adminArea?.takeIf { it.isNotBlank() } ?: fallbackPinOffice?.state
                    rawCandidates += WeatherPlaceCandidate(
                        name = cleanName,
                        details = formatDetails(cleanName, district, state, effectivePin, districtWord),
                        postalCode = effectivePin,
                        latitude = addr.latitude,
                        longitude = addr.longitude,
                        matchedPin = parsed.postalCode != null && effectivePin == parsed.postalCode,
                    )
                }
            }
        }

        // Step 3: Open-Meteo Geocoding API — ensures complete results even without Google Geocoder,
        // and lists all matching towns across districts (e.g., Alangudi in Pudukkottai vs Thiruvarur).
        val searchTerms = buildList {
            if (parsed.placeText.isNotBlank()) add(parsed.placeText)
            pinOffices.take(3).forEach { office -> add(office.name) }
            if (isEmpty() && parsed.postalCode != null) add(parsed.postalCode)
        }.distinct()

        for (term in searchTerms) {
            val openMeteoHits = runCatching { fetchOpenMeteoPlaces(term, tamil) }.getOrDefault(emptyList())
            for (hit in openMeteoHits) {
                val matchingOffice = pinOffices.firstOrNull { office ->
                    hit.district.contains(office.district, ignoreCase = true) ||
                        office.district.contains(hit.district, ignoreCase = true)
                }
                val pin = when {
                    parsed.postalCode != null && hit.postcodes.contains(parsed.postalCode) -> parsed.postalCode
                    parsed.postalCode != null && matchingOffice != null -> parsed.postalCode
                    else -> hit.postcodes.firstOrNull().orEmpty()
                }
                val pinMatch = parsed.postalCode != null && (hit.postcodes.contains(parsed.postalCode) || matchingOffice != null)
                rawCandidates += WeatherPlaceCandidate(
                    name = hit.name,
                    details = formatDetails(hit.name, hit.district, hit.state, pin, districtWord),
                    postalCode = pin,
                    latitude = hit.latitude,
                    longitude = hit.longitude,
                    matchedPin = pinMatch,
                )
            }
        }

        rankAndDeduplicate(rawCandidates, parsed)
    }

    internal data class PinOfficeInfo(val name: String, val district: String, val state: String)

    internal fun parseIndiaPinResponse(jsonText: String): List<PinOfficeInfo> {
        val arr = JSONArray(jsonText)
        if (arr.length() == 0) return emptyList()
        val first = arr.optJSONObject(0) ?: return emptyList()
        if (!first.optString("Status").equals("Success", ignoreCase = true)) return emptyList()
        val postOffices = first.optJSONArray("PostOffice") ?: return emptyList()
        val result = mutableListOf<PinOfficeInfo>()
        for (i in 0 until postOffices.length()) {
            val po = postOffices.optJSONObject(i) ?: continue
            val name = po.optString("Name").trim()
            val district = po.optString("District").trim()
            val state = po.optString("State").trim()
            if (name.isNotBlank()) result += PinOfficeInfo(name, district, state)
        }
        return result
    }

    internal data class OpenMeteoGeoHit(
        val name: String,
        val district: String,
        val state: String,
        val latitude: Double,
        val longitude: Double,
        val postcodes: List<String>,
    )

    internal fun parseOpenMeteoGeocoding(jsonText: String): List<OpenMeteoGeoHit> {
        val root = JSONObject(jsonText)
        val results = root.optJSONArray("results") ?: return emptyList()
        val hits = mutableListOf<OpenMeteoGeoHit>()
        for (i in 0 until results.length()) {
            val item = results.optJSONObject(i) ?: continue
            val lat = item.optDouble("latitude", Double.NaN)
            val lon = item.optDouble("longitude", Double.NaN)
            if (!lat.isFinite() || !lon.isFinite()) continue
            val postcodesArr = item.optJSONArray("postcodes")
            val postcodes = buildList {
                if (postcodesArr != null) {
                    for (j in 0 until postcodesArr.length()) {
                        postcodesArr.optString(j).trim().takeIf { it.isNotBlank() }?.let(::add)
                    }
                }
            }
            hits += OpenMeteoGeoHit(
                name = item.optString("name").trim(),
                district = item.optString("admin2").trim(),
                state = item.optString("admin1").trim(),
                latitude = lat,
                longitude = lon,
                postcodes = postcodes,
            )
        }
        return hits
    }

    fun rankAndDeduplicate(
        candidates: List<WeatherPlaceCandidate>,
        parsed: ParsedPlaceQuery,
    ): List<WeatherPlaceCandidate> {
        val sorted = candidates.sortedWith(
            compareByDescending<WeatherPlaceCandidate> { it.matchedPin }
                .thenByDescending { c ->
                    parsed.placeText.isNotBlank() && c.name.equals(parsed.placeText, ignoreCase = true)
                }
                .thenByDescending { it.postalCode.isNotBlank() }
        )
        val kept = mutableListOf<WeatherPlaceCandidate>()
        for (candidate in sorted) {
            val duplicate = kept.any { existing ->
                WeatherForecastClient.haversineKm(
                    existing.latitude,
                    existing.longitude,
                    candidate.latitude,
                    candidate.longitude,
                ) < SAME_PLACE_KM
            }
            if (!duplicate) kept += candidate
            if (kept.size >= MAX_RESULTS) break
        }
        return kept
    }

    private fun fetchIndiaPinOffices(pin: String): List<PinOfficeInfo> {
        val raw = WeatherForecastClient.httpGet("https://api.postalpincode.in/pincode/$pin")
        return parseIndiaPinResponse(raw)
    }

    private fun fetchOpenMeteoPlaces(name: String, tamil: Boolean): List<OpenMeteoGeoHit> {
        val encoded = URLEncoder.encode(name, "UTF-8")
        val lang = if (tamil) "ta" else "en"
        val raw = WeatherForecastClient.httpGet(
            "https://geocoding-api.open-meteo.com/v1/search?name=$encoded&count=10&language=$lang&format=json"
        )
        return parseOpenMeteoGeocoding(raw)
    }

    @SuppressLint("MissingPermission")
    private fun bestCachedLocation(context: Context): Location? {
        val manager = context.getSystemService(LocationManager::class.java) ?: return null
        val providers = if (hasFineLocationPermission(context)) {
            listOf(
                LocationManager.GPS_PROVIDER,
                LocationManager.NETWORK_PROVIDER,
                LocationManager.PASSIVE_PROVIDER,
            )
        } else {
            listOf(
                LocationManager.NETWORK_PROVIDER,
                LocationManager.PASSIVE_PROVIDER,
            )
        }
        return providers
            .mapNotNull { provider -> runCatching { manager.getLastKnownLocation(provider) }.getOrNull() }
            .maxByOrNull { it.time }
    }

    private data class CachedReverseLabel(
        val lat: Double,
        val lon: Double,
        val tamil: Boolean,
        val name: String,
        val details: String,
        val atMs: Long,
    )

    @Volatile
    private var lastReverseCache: CachedReverseLabel? = null

    @Suppress("DEPRECATION")
    private fun reverseGeocodeLabel(
        context: Context,
        latitude: Double,
        longitude: Double,
        tamil: Boolean,
    ): Pair<String, String> {
        val now = System.currentTimeMillis()
        lastReverseCache?.let { cached ->
            if (cached.tamil == tamil &&
                now - cached.atMs < 20L * 60_000L &&
                kotlin.math.abs(cached.lat - latitude) < 0.008 &&
                kotlin.math.abs(cached.lon - longitude) < 0.008 &&
                cached.name.isNotBlank()
            ) {
                return cached.name to cached.details
            }
        }

        val districtWord = if (tamil) "மாவட்டம்" else "district"

        // Layer 1: Android Geocoder (if available and non-empty)
        val platformPair = runCatching {
            if (!Geocoder.isPresent()) return@runCatching null
            val locale = if (tamil) Locale("ta", "IN") else Locale("en", "IN")
            val list = Geocoder(context.applicationContext, locale).getFromLocation(latitude, longitude, 3)
            val addr: Address = list?.firstOrNull {
                firstCleanPlace(it.subLocality, it.locality, it.subAdminArea).isNotBlank()
            } ?: list?.firstOrNull() ?: return@runCatching null
            val name = firstCleanPlace(addr.subLocality, addr.locality, addr.thoroughfare, addr.subAdminArea, addr.adminArea)
            if (name.isBlank()) null
            else name to formatDetails(name, addr.subAdminArea, addr.adminArea, addr.postalCode.orEmpty(), districtWord)
        }.getOrNull()

        if (platformPair != null && platformPair.first.isNotBlank()) {
            lastReverseCache = CachedReverseLabel(latitude, longitude, tamil, platformPair.first, platformPair.second, now)
            return platformPair
        }

        // Layer 2: OpenStreetMap Nominatim HTTPS JSON reverse lookup (works on all Xiaomi/MIUI/HyperOS & de-Googled devices)
        val nominatimPair = runCatching {
            val lang = if (tamil) "ta,en" else "en"
            val raw = WeatherForecastClient.httpGet(
                "https://nominatim.openstreetmap.org/reverse?format=jsonv2&lat=$latitude&lon=$longitude&zoom=14&addressdetails=1&accept-language=$lang"
            )
            if (raw.isBlank()) return@runCatching null
            val root = org.json.JSONObject(raw)
            val addr = root.optJSONObject("address") ?: return@runCatching null
            val name = firstCleanPlace(
                addr.optString("village"),
                addr.optString("suburb"),
                addr.optString("town"),
                addr.optString("neighbourhood"),
                addr.optString("hamlet"),
                addr.optString("city"),
                addr.optString("county"),
                addr.optString("state_district"),
                root.optString("name"),
            )
            if (name.isBlank()) null
            else {
                val district = addr.optString("state_district").ifBlank { addr.optString("county") }.ifBlank { addr.optString("city") }
                val state = addr.optString("state")
                val pin = addr.optString("postcode")
                name to formatDetails(name, district, state, pin, districtWord)
            }
        }.getOrNull()

        if (nominatimPair != null && nominatimPair.first.isNotBlank()) {
            lastReverseCache = CachedReverseLabel(latitude, longitude, tamil, nominatimPair.first, nominatimPair.second, now)
            return nominatimPair
        }

        // Layer 3: Free BigDataCloud reverse geocoder fallback
        val bdcPair = runCatching {
            val lang = if (tamil) "ta" else "en"
            val raw = WeatherForecastClient.httpGet(
                "https://api.bigdatacloud.net/data/reverse-geocode-client?latitude=$latitude&longitude=$longitude&localityLanguage=$lang"
            )
            if (raw.isBlank()) return@runCatching null
            val root = org.json.JSONObject(raw)
            val name = firstCleanPlace(
                root.optString("locality"),
                root.optString("city"),
                root.optString("principalSubdivision"),
            )
            if (name.isBlank()) null
            else {
                val city = root.optString("city")
                val state = root.optString("principalSubdivision")
                val pin = root.optString("postcode")
                name to formatDetails(name, city, state, pin, districtWord)
            }
        }.getOrNull()

        if (bdcPair != null && bdcPair.first.isNotBlank()) {
            lastReverseCache = CachedReverseLabel(latitude, longitude, tamil, bdcPair.first, bdcPair.second, now)
            return bdcPair
        }

        // Layer 4: Guaranteed non-blank fallback when offline with cell tower fix
        val coord = String.format(Locale.US, "%.2f°N, %.2f°E", latitude, longitude)
        return if (tamil) {
            "மொபைல் டவர் இடம் ($coord)" to "செல் டவர்"
        } else {
            "Mobile Tower Area ($coord)" to "Cell tower"
        }
    }

    private fun firstCleanPlace(vararg candidates: String?): String =
        candidates.firstOrNull { raw ->
            val v = raw?.trim().orEmpty()
            v.isNotBlank() &&
                !pinRegex.matches(v) &&
                !v.contains('+') &&
                !v.startsWith("Unnamed", ignoreCase = true) &&
                !v.startsWith("பெயரில்லாத")
        }?.trim().orEmpty()

    private fun formatDetails(
        name: String,
        district: String?,
        state: String?,
        pin: String,
        districtWord: String,
    ): String {
        val parts = mutableListOf<String>()
        district?.trim()?.takeIf { it.isNotBlank() }?.let { rawDistrict ->
            val clean = districtSuffix.replace(rawDistrict, "").trim().ifBlank { rawDistrict }
            if (!clean.equals(name, ignoreCase = true)) {
                parts += "$clean $districtWord"
            }
        }
        state?.trim()?.takeIf { it.isNotBlank() }?.let { parts += it }
        val placePart = parts.joinToString(", ")
        return listOfNotNull(placePart.takeIf { it.isNotBlank() }, pin.takeIf { it.isNotBlank() })
            .joinToString(" ")
    }
}
