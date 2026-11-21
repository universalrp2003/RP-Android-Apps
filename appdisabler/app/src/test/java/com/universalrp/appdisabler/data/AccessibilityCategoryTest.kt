package com.universalrp.appdisabler.data
import org.junit.Assert.assertEquals
import org.junit.Test
class AccessibilityCategoryTest {
    @Test fun talkbackRecommendationIsDowngradedButNeverOverridesCritical() {
        val pkg = "com.google.android.marvin.talkback"
        assertEquals(DisableCategory.CAUTION, reviewCategory(pkg, DisableCategory.SAFE))
        assertEquals(DisableCategory.CRITICAL, reviewCategory(pkg, DisableCategory.CRITICAL))
        assertEquals(DisableCategory.UNLISTED, reviewCategory(pkg, DisableCategory.UNLISTED))
        assertEquals(DisableCategory.SAFE, reviewCategory("com.example.other", DisableCategory.SAFE))
    }
}
