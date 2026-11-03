package com.universalrp.cleansweep.ui
import com.universalrp.updates.SisterApps
import org.junit.Assert.*
import org.junit.Test
class LiveGuardAboutPolicyTest {
    @Test fun noLegacyInLiveGuardProjects() { assertFalse(LiveGuardAboutPolicy.projectLinks(SisterApps.projectLinks()).any { it.second == SisterApps.LEGACY_MIRROR_URL }) }
    @Test fun dailyVaultOnlyInOtherApps() { val url=SisterApps.releaseUrl(SisterApps.byId("dailyvault")!!);assertFalse(LiveGuardAboutPolicy.projectLinks(SisterApps.projectLinks()).any { it.second==url });assertEquals(1,LiveGuardAboutPolicy.releaseLinks(SisterApps.releaseLinks()).count { it.second==url }) }
    @Test fun otherAppsDoNotIncludeLiveGuardItself() { assertEquals(13,LiveGuardAboutPolicy.releaseLinks(SisterApps.releaseLinks()).size) }
    @Test fun siblingSharedListsUnchanged() { val original=SisterApps.projectLinks();LiveGuardAboutPolicy.projectLinks(original);assertTrue(original.any {it.second==SisterApps.LEGACY_MIRROR_URL});assertTrue(original.any {it.second==SisterApps.releaseUrl(SisterApps.byId("dailyvault")!!)}) }
}
