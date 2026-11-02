package com.universalrp.cleansweep.data

import org.junit.Assert.*
import org.junit.Test

class SecurityIdentityPolicyTest {
    private val a = "a".repeat(64)
    private val b = "b".repeat(64)
    @Test fun sameSignerIsNotAChange() { assertEquals(SecurityIdentityPolicy.Change.SAME,SecurityIdentityPolicy.compare(PackageIdentity(setOf(a)),PackageIdentity(setOf(a)))) }
    @Test fun platformLineageDistinguishesLegitimateRotation() { assertEquals(SecurityIdentityPolicy.Change.PLATFORM_ROTATION,SecurityIdentityPolicy.compare(PackageIdentity(setOf(a)),PackageIdentity(setOf(b),setOf(a,b)))) }
    @Test fun unrelatedSignerIsReviewNotMalwareVerdict() { assertEquals(SecurityIdentityPolicy.Change.DIFFERENT,SecurityIdentityPolicy.compare(PackageIdentity(setOf(a)),PackageIdentity(setOf(b)))) }
    @Test fun missingEvidenceIsUnknownNotSame() { assertEquals(SecurityIdentityPolicy.Change.UNAVAILABLE,SecurityIdentityPolicy.compare(null,PackageIdentity(setOf(a)))) }
    @Test fun malformedHashIsNotComparable() { assertEquals(SecurityIdentityPolicy.Change.UNAVAILABLE,SecurityIdentityPolicy.compare(PackageIdentity(setOf("invalid")),PackageIdentity(setOf(a)))) }
    @Test fun multiSignerRequiresExactSetNotPartialOverlap() { assertEquals(SecurityIdentityPolicy.Change.DIFFERENT,SecurityIdentityPolicy.compare(PackageIdentity(setOf(a)),PackageIdentity(setOf(a,b),setOf(a,b),true))) }
    @Test fun malformedMultipleFlagRejected() { assertFalse(PackageIdentity(setOf(a,b)).valid());assertFalse(PackageIdentity(setOf(a),multiple=true).valid()) }
    @Test fun historyMustContainCurrentSigner() { assertFalse(PackageIdentity(setOf(b),setOf(a)).valid()) }
    @Test fun overlayAccessibilityCombinationNeedsBothObserved() { assertEquals(setOf("access_combo_overlay"),SecurityIdentityPolicy.combinations(setOf("overlay","accessibility")));assertTrue(SecurityIdentityPolicy.combinations(setOf("overlay")).isEmpty()) }
    @Test fun notificationInstallerCombinationNeedsBothObserved() { assertEquals(setOf("access_combo_installer"),SecurityIdentityPolicy.combinations(setOf("notification_listeners","install_other_apps"))) }
    @Test fun unavailableDependencyCannotResolveCombination() { assertEquals(setOf("access_combo_overlay"),SecurityIdentityPolicy.unavailable(setOf("overlay"))) }
    @Test fun lookalikeInstallerNameIsNotRecognised() { assertTrue(BloatRules.isTrustedInstaller("com.android.vending"));assertFalse(BloatRules.isTrustedInstaller("evil.com.android.vending.copy"));assertFalse(BloatRules.isTrustedInstaller(null)) }
}
