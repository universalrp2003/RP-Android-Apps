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
    @Test fun photoAndCameraRecommendationsRequireCautionWithoutChangingOtherRatings() {
        val protectedExamples = listOf(
            "com.google.android.apps.photos", "com.miui.gallery", "com.sec.android.gallery3d",
            "com.coloros.gallery3d", "com.huawei.photos", "com.hihonor.photos",
            "com.android.camera2", "com.sec.android.app.camera", "org.codeaurora.snapcam"
        )
        for (pkg in protectedExamples) {
            assertEquals(DisableCategory.CAUTION, reviewCategory(pkg, DisableCategory.SAFE))
            assertEquals(DisableCategory.CAUTION, reviewCategory(pkg, DisableCategory.CAUTION))
            assertEquals(DisableCategory.CRITICAL, reviewCategory(pkg, DisableCategory.CRITICAL))
            assertEquals(DisableCategory.UNLISTED, reviewCategory(pkg, DisableCategory.UNLISTED))
        }
        // Xiaomi's wallpaper feed contains gallery in its name but is not the gallery app.
        assertEquals(DisableCategory.SAFE, reviewCategory("com.miui.android.fashiongallery", DisableCategory.SAFE))
    }
}
