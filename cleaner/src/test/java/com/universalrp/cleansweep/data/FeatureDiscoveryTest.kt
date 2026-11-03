package com.universalrp.cleansweep.data

import android.content.Context
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE)
class FeatureDiscoveryTest {
    private val c: Context get() = RuntimeEnvironment.getApplication()
    private val feature = FeatureDiscovery.Feature.DISASTER_NEWS
    private val notice = FeatureDiscovery.Mode.DEFAULT_ON_NOTICE
    private val ask = FeatureDiscovery.Mode.ASK_FIRST
    @Before fun clean() { listOf(FeatureDiscovery.PREFS, NewsDisasterUpdates.PREFS, "news_topics_283", "liveguard_insights", com.universalrp.cleansweep.notify.DisasterUpdateNotifier.PREFS).forEach { c.getSharedPreferences(it,0).edit().clear().commit() } }
    private fun rawDisaster() = c.getSharedPreferences(NewsDisasterUpdates.PREFS,0)
    @Test fun disasterOnForUnconfiguredNewAndUpdatedUsers() { assertTrue(NewsDisasterUpdates.enabled(c));assertFalse(rawDisaster().contains("enabled")) }
    @Test fun previousExplicitOffPreserved() { NewsDisasterUpdates.setEnabled(c,false);assertFalse(NewsDisasterUpdates.enabled(c));assertEquals(feature,FeatureDiscovery.next(c)) }
    @Test fun previousExplicitOnPreserved() { NewsDisasterUpdates.setEnabled(c,true);assertTrue(NewsDisasterUpdates.enabled(c)) }
    @Test fun oldDefaultOffWithoutStoredChoiceBecomesOnWithoutResettingCounts() { rawDisaster().edit().putString("counts","{\"TC:123\":3}").commit();assertTrue(NewsDisasterUpdates.enabled(c));assertEquals(3,NewsDisasterUpdates.count(c,"TC:123")) }
    @Test fun introductionPendingOnFirstOpening() { assertEquals(feature,FeatureDiscovery.next(c));assertFalse(FeatureDiscovery.seen(c,feature)) }
    @Test fun acknowledgeDurableAndNotRepeated() { assertTrue(FeatureDiscovery.respond(c,feature,FeatureDiscovery.Choice.ACKNOWLEDGE));assertEquals(FeatureDiscovery.Feature.SECURITY_HISTORY,FeatureDiscovery.next(c));assertTrue(c.getSharedPreferences(FeatureDiscovery.PREFS,0).getBoolean(feature.key,false)) }
    @Test fun turnOffFromNoticePersists() { assertTrue(FeatureDiscovery.respond(c,feature,FeatureDiscovery.Choice.DISABLE));assertFalse(NewsDisasterUpdates.enabled(c));assertEquals(FeatureDiscovery.Feature.SECURITY_HISTORY,FeatureDiscovery.next(c)) }
    @Test fun acknowledgingDoesNotTurnPreviousOffBackOn() { NewsDisasterUpdates.setEnabled(c,false);FeatureDiscovery.respond(c,feature,FeatureDiscovery.Choice.ACKNOWLEDGE);assertFalse(NewsDisasterUpdates.enabled(c)) }
    @Test fun explicitEnableFromNoticeAllowed() { NewsDisasterUpdates.setEnabled(c,false);FeatureDiscovery.respond(c,feature,FeatureDiscovery.Choice.ENABLE);assertTrue(NewsDisasterUpdates.enabled(c)) }
    @Test fun introductionNeverResetsThreeReadMemory() { rawDisaster().edit().putString("counts","{\"TC:123\":2}").commit();FeatureDiscovery.respond(c,feature,FeatureDiscovery.Choice.DISABLE);assertEquals(2,NewsDisasterUpdates.count(c,"TC:123")) }
    @Test fun allThirteenNewsTopicsSelectedByDefault() { assertEquals(13,NewsPreferences.selected(c).size);assertEquals(NewsPreferences.allCategories(),NewsPreferences.categories(c)) }
    @Test fun customisedSubsetNotReplaced() { c.getSharedPreferences("news_topics_283",0).edit().putStringSet("topics",setOf("SPORTS","SCIENCE")).commit();assertEquals(setOf(BalancedNewsPolicy.Category.SPORTS,BalancedNewsPolicy.Category.SCIENCE),NewsPreferences.selected(c)) }
    @Test fun languagePreferenceNotResetByNewDefault() { c.getSharedPreferences("news_topics_283",0).edit().putString("language","TAMIL_ONLY").commit();assertEquals(NewsPreferences.Language.TAMIL_ONLY,NewsPreferences.language(c));assertEquals(13,NewsPreferences.selected(c).size) }
    @Test fun oldEmptyDefaultSelectionUsesNewAllTopicsDefault() { c.getSharedPreferences("news_topics_283",0).edit().putStringSet("topics",emptySet()).commit();assertEquals(13,NewsPreferences.selected(c).size) }
    @Test fun unknownOldCategoryNamesDoNotBreakDefault() { c.getSharedPreferences("news_topics_283",0).edit().putStringSet("topics",setOf("UNKNOWN")).commit();assertEquals(13,NewsPreferences.selected(c).size) }
    @Test fun defaultOnNoticeAcknowledgementPreservesCurrentState() { assertTrue(FeatureDiscovery.defaultEnabled(notice));assertFalse(FeatureDiscovery.resolve(notice,false,FeatureDiscovery.Choice.ACKNOWLEDGE));assertTrue(FeatureDiscovery.resolve(notice,true,FeatureDiscovery.Choice.ACKNOWLEDGE)) }
    @Test fun futureAskFirstFeaturesDefaultOff() { assertFalse(FeatureDiscovery.defaultEnabled(ask)) }
    @Test fun futureAskFirstRequiresExplicitEnable() { assertTrue(FeatureDiscovery.resolve(ask,false,FeatureDiscovery.Choice.ENABLE)) }
    @Test fun futureNotNowDoesNotActivate() { assertFalse(FeatureDiscovery.resolve(ask,false,FeatureDiscovery.Choice.NOT_NOW)) }
    @Test fun acknowledgementCannotBypassFutureConsent() { try {FeatureDiscovery.resolve(ask,false,FeatureDiscovery.Choice.ACKNOWLEDGE);fail("Ask-first feature accepted acknowledgement")} catch(_:IllegalArgumentException) {} }

    @Test fun securityIntroductionAppearsAfterDisasterAndStartsOff() { FeatureDiscovery.respond(c,feature,FeatureDiscovery.Choice.ACKNOWLEDGE);assertEquals(FeatureDiscovery.Feature.SECURITY_HISTORY,FeatureDiscovery.next(c));assertFalse(SecurityHistoryRepo.enabled(c)) }
    @Test fun notNowSecurityNeverEnablesHistory() { assertTrue(FeatureDiscovery.respond(c,FeatureDiscovery.Feature.SECURITY_HISTORY,FeatureDiscovery.Choice.NOT_NOW));assertFalse(SecurityHistoryRepo.enabled(c));assertTrue(FeatureDiscovery.seen(c,FeatureDiscovery.Feature.SECURITY_HISTORY)) }
    @Test fun explicitSecurityEnablePersistsWithoutChangingDisaster() { NewsDisasterUpdates.setEnabled(c,false);assertTrue(FeatureDiscovery.respond(c,FeatureDiscovery.Feature.SECURITY_HISTORY,FeatureDiscovery.Choice.ENABLE));assertTrue(SecurityHistoryRepo.enabled(c));assertFalse(NewsDisasterUpdates.enabled(c)) }
    @Test fun securityNotNowPreservesPreviouslyEnabledHistory() { SecurityHistoryRepo.setEnabledDurably(c,true);FeatureDiscovery.respond(c,FeatureDiscovery.Feature.SECURITY_HISTORY,FeatureDiscovery.Choice.NOT_NOW);assertTrue(SecurityHistoryRepo.enabled(c)) }

    @Test fun silentNotificationsAreAskFirstAndNotNowStaysOff() { val f=FeatureDiscovery.Feature.DISASTER_NOTIFICATIONS;assertEquals(ask,f.mode);assertFalse(FeatureDiscovery.enabled(c,f));assertTrue(FeatureDiscovery.respond(c,f,FeatureDiscovery.Choice.NOT_NOW));assertTrue(FeatureDiscovery.seen(c,f));assertFalse(FeatureDiscovery.enabled(c,f)) }
    @Test fun explicitSilentNotificationEnableDoesNotTurnNewsBackOn() { val f=FeatureDiscovery.Feature.DISASTER_NOTIFICATIONS;NewsDisasterUpdates.setEnabled(c,false);assertTrue(FeatureDiscovery.respond(c,f,FeatureDiscovery.Choice.ENABLE));assertTrue(FeatureDiscovery.enabled(c,f));assertFalse(NewsDisasterUpdates.enabled(c)) }
}
