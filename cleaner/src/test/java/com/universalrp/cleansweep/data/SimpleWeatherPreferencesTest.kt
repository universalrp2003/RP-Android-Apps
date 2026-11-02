package com.universalrp.cleansweep.data

import android.content.Context
import org.robolectric.RuntimeEnvironment
import com.universalrp.weather.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE)
class SimpleWeatherPreferencesTest {
    private val context: Context get() = RuntimeEnvironment.getApplication()
    private val home = WeatherPlaceCandidate("Home", "Test place", "", 10.0, 78.0)
    private val gps = home.copy(name = "Moving", latitude = 11.0)
    private fun snapshot() = WeatherNowcastSnapshot(100_000, WeatherSourceKind.TOMORROW_IO_1M, 28.0, 80, 0.0, 0.0, 1001,
        listOf(MinuteRainSample(0, 0.0), MinuteRainSample(1, 0.0)), minuteHorizonMinutes = 1, tomorrowStatus = "MINUTE_READY")
    @Before fun clean() { listOf("simple_weather_283", "charge_announcement_v283", "saved_rain_watches_283", WeatherAlertStore.PREFS).forEach { context.getSharedPreferences(it, 0).edit().clear().commit() } }
    @Test fun advancedOffPercentageOnByDefault() { assertFalse(ChargeAnnouncementSettings.advanced(context)); assertTrue(ChargeAnnouncementSettings.percentNotice(context)) }
    @Test fun thresholdsSurviveAdvancedToggle() { val settings = ChargeEstimateSettings(.5f,.05f,180_000); ChargeAnnouncementSettings.save(context, settings); ChargeAnnouncementSettings.setAdvanced(context,true); ChargeAnnouncementSettings.setAdvanced(context,false); assertEquals(settings, ChargeAnnouncementSettings.thresholds(context)) }
    @Test fun gpsIsOptInAndDoesNotReplaceHome() { WeatherAlertStore.savePlace(context,home); assertFalse(SavedRainWatches.currentEnabled(context)); SavedRainWatches.setCurrentEnabled(context,true); SimpleWeatherStore.save(context,SimpleWeatherStore.GPS,gps,snapshot(),WeatherEpisodeMemory()); assertEquals(home,WeatherAlertStore.savedPlace(context)) }
    @Test fun independentHomeAndGpsResults() { SimpleWeatherStore.save(context,SimpleWeatherStore.HOME,home,snapshot(),WeatherEpisodeMemory()); SimpleWeatherStore.save(context,SimpleWeatherStore.GPS,gps,snapshot(),WeatherEpisodeMemory()); SimpleWeatherStore.clear(context,SimpleWeatherStore.GPS); assertEquals(home.name,SimpleWeatherStore.read(context,SimpleWeatherStore.HOME)?.place?.name); assertNull(SimpleWeatherStore.read(context,SimpleWeatherStore.GPS)) }
    @Test fun movementResetsOnlyGpsMemory() { val memory=WeatherEpisodeMemory(episodeActive=true,highestAnnouncedRank=1); SimpleWeatherStore.save(context,SimpleWeatherStore.GPS,gps,snapshot(),memory); SimpleWeatherStore.save(context,SimpleWeatherStore.HOME,home,snapshot(),memory); assertEquals(WeatherEpisodeMemory(),SimpleWeatherStore.memory(context,SimpleWeatherStore.GPS,gps.copy(latitude=12.0))); assertEquals(memory,SimpleWeatherStore.memory(context,SimpleWeatherStore.HOME,home)) }
    @Test fun failedCheckDoesNotLookLikeDryWeather() { SimpleWeatherStore.save(context,SimpleWeatherStore.HOME,home,null,WeatherEpisodeMemory()); val result=SimpleWeatherStore.read(context,SimpleWeatherStore.HOME)!!; assertTrue(result.error); assertNull(result.snapshot) }
}
