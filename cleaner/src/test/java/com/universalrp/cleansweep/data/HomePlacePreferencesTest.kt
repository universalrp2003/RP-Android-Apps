package com.universalrp.cleansweep.data

import android.content.Context
import com.universalrp.weather.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE)
class HomePlacePreferencesTest {
    private val c: Context get() = RuntimeEnvironment.getApplication()
    private val home = WeatherPlaceCandidate("Alangudi", "Tamil Nadu", "622301", 10.3, 78.9)
    private val other = home.copy(name = "Pallapatti", latitude = 10.5)
    private fun seed() {
        WeatherAlertStore.savePlace(c, home)
        SimpleWeatherStore.save(c, SimpleWeatherStore.HOME, home, null, WeatherEpisodeMemory(true, 2))
        WeatherAlertStore.prefs(c).edit().putString("last_place_name", home.name).putLong("last_checked_ms", 500L).putBoolean("episode_active", true).commit()
        SavedRainWatches.add(c, other); SavedRainWatches.setCurrentEnabled(c, true); SavedRainWatches.setLead(c, 60)
        SimpleWeatherStore.save(c, SimpleWeatherStore.GPS, other, null, WeatherEpisodeMemory())
    }
    @Before fun clean() { listOf(WeatherAlertStore.PREFS, "saved_rain_watches_283", "simple_weather_283").forEach { c.getSharedPreferences(it, 0).edit().clear().commit() } }
    @Test fun removalClearsHomeAndDerivedResults() { seed(); WeatherAlertStore.clearSavedPlace(c); assertNull(WeatherAlertStore.savedPlace(c)); assertNull(SimpleWeatherStore.read(c, SimpleWeatherStore.HOME)); assertEquals("", WeatherAlertStore.readCachedForWidget(c).placeName) }
    @Test fun removalResetsEpisodeAndThrottle() { seed(); WeatherAlertStore.clearSavedPlace(c); assertEquals(WeatherEpisodeMemory(), WeatherAlertStore.loadMemory(c)); assertEquals(0L, WeatherAlertStore.nextCheckAfterMs(c)) }
    @Test fun removalPreservesGpsOtherPlacesAndLead() { seed(); WeatherAlertStore.clearSavedPlace(c); assertTrue(SavedRainWatches.currentEnabled(c)); assertEquals(60, SavedRainWatches.lead(c)); assertEquals(other.name, SavedRainWatches.read(c).single().place.name); assertNotNull(SimpleWeatherStore.read(c, SimpleWeatherStore.GPS)) }
    @Test fun replacementClearsOnlyHomeDerivedState() { seed(); WeatherAlertStore.savePlace(c, other); assertEquals(other, WeatherAlertStore.savedPlace(c)); assertNull(SimpleWeatherStore.read(c, SimpleWeatherStore.HOME)); assertNotNull(SimpleWeatherStore.read(c, SimpleWeatherStore.GPS)) }
    @Test fun sameHomeSaveDoesNotResetForecast() { seed(); WeatherAlertStore.savePlace(c, home); assertNotNull(SimpleWeatherStore.read(c, SimpleWeatherStore.HOME)) }
    @Test fun removalIsIdempotentAndAllowsResaving() { seed(); WeatherAlertStore.clearSavedPlace(c); WeatherAlertStore.clearSavedPlace(c); WeatherAlertStore.savePlace(c, other); assertEquals(other, WeatherAlertStore.savedPlace(c)) }
    @Test fun removalPreservesKeyAndExplicitVoiceChoice() { seed(); WeatherAlertStore.prefs(c).edit().putString("tomorrow_io_api_key_enc", "synthetic-encrypted-fixture").putBoolean("user_explicit_weather_toggle", true).commit(); WeatherAlertStore.clearSavedPlace(c); assertEquals("synthetic-encrypted-fixture", WeatherAlertStore.prefs(c).getString("tomorrow_io_api_key_enc", "")); assertTrue(WeatherAlertStore.prefs(c).getBoolean("user_explicit_weather_toggle", false)) }
}
