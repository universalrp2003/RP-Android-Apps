package com.universalrp.cleansweep.data

import android.content.Context
import com.universalrp.weather.WeatherEpisodeMemory
import com.universalrp.weather.WeatherPlaceCandidate
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[34],manifest=Config.NONE)
class SavedRainWatchPreferencesTest {
    private val context:Context get()=RuntimeEnvironment.getApplication()
    private fun place(n:Int)=WeatherPlaceCandidate("Synthetic $n","","",10.0+n*.1,78.0+n*.1)
    @Before fun clearSyntheticPreferences() {
        context.getSharedPreferences("saved_rain_watches_283",0).edit().clear().commit()
        context.getSharedPreferences("news_topics_283",0).edit().clear().commit()
    }
    @Test fun threeIndependentPlacesCanBeDisabledEditedAndDeletedWithoutGps() {
        (1..3).forEach {assertTrue(SavedRainWatches.add(context,place(it)))}
        assertFalse(SavedRainWatches.add(context,place(4)))
        val ids=SavedRainWatches.read(context).map {it.id}
        SavedRainWatches.setCurrentEnabled(context,false)
        SavedRainWatches.toggle(context,ids[0],false)
        assertEquals(listOf(false,true,true),SavedRainWatches.read(context).map {it.enabled})
        assertTrue(SavedRainWatches.update(context,ids[0],place(4).copy(name="Renamed synthetic")))
        assertEquals(ids,SavedRainWatches.read(context).map {it.id})
        assertEquals("Renamed synthetic",SavedRainWatches.read(context).first().place.name)
        assertFalse(SavedRainWatches.currentEnabled(context))
        SavedRainWatches.remove(context,ids[1]);assertEquals(2,SavedRainWatches.read(context).size)
    }
    @Test fun historyIsIndependentBoundedAndDeletedWithPlace() {
        SavedRainWatches.add(context,place(1));SavedRainWatches.add(context,place(2))
        val ids=SavedRainWatches.read(context).map {it.id}
        for(n in 1..50) SavedRainWatches.save(context,ids[0],WeatherEpisodeMemory(),"Synthetic record $n")
        assertEquals(40,SavedRainWatches.history(context,ids[0]).size)
        assertEquals("Synthetic record 50",SavedRainWatches.history(context,ids[0]).first().message)
        assertTrue(SavedRainWatches.history(context,ids[1]).isEmpty())
        SavedRainWatches.remove(context,ids[0]);assertTrue(SavedRainWatches.history(context,ids[0]).isEmpty())
        // A deleted watch cannot be resurrected by a late network response.
        SavedRainWatches.save(context,ids[0],WeatherEpisodeMemory(),"Late response")
        assertTrue(SavedRainWatches.history(context,ids[0]).isEmpty())
    }
    @Test fun movingCoordinatesResetsEpisodeButRenameKeepsIt() {
        SavedRainWatches.add(context,place(1));val id=SavedRainWatches.read(context).single().id
        val memory=WeatherEpisodeMemory(episodeActive=true,highestAnnouncedRank=2)
        SavedRainWatches.save(context,id,memory,"Synthetic alert")
        SavedRainWatches.update(context,id,place(1).copy(name="Renamed"))
        assertEquals(memory,SavedRainWatches.memory(context,id))
        SavedRainWatches.update(context,id,place(2))
        assertEquals(WeatherEpisodeMemory(),SavedRainWatches.memory(context,id))
    }
    @Test fun invalidLeadAndDuplicateLocationAreRejected() {
        assertEquals(30,SavedRainWatches.lead(context))
        for(n in listOf(15,30,60)) {SavedRainWatches.setLead(context,n);assertEquals(n,SavedRainWatches.lead(context))}
        assertTrue(SavedRainWatches.add(context,place(1)))
        assertFalse(SavedRainWatches.add(context,place(1)))
        try {SavedRainWatches.setLead(context,17);fail("Invalid lead was accepted")} catch(_:IllegalArgumentException) {}
    }
    @Test fun languageAndEmptyTopicDefaultsRemainExplicit() {
        assertEquals(BalancedNewsPolicy.DEFAULT_CATEGORIES,NewsPreferences.categories(context))
        // Read state directly; don't initialise audio to test persistence.
        context.getSharedPreferences("news_topics_283",0).edit().putString("language","TAMIL_ONLY")
            .putStringSet("topics",setOf("SPORTS")).commit()
        assertTrue(NewsPreferences.tamil(context));assertFalse(NewsPreferences.translate(context))
        assertEquals(setOf(BalancedNewsPolicy.Category.SPORTS),NewsPreferences.categories(context))
    }
}
