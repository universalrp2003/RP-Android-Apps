package com.universalrp.cleansweep.data

import org.junit.Assert.*
import org.junit.Test
import java.time.Instant
import java.time.ZoneId

class BalancedNewsPolicyTest {
    private val now = Instant.parse("2026-10-30T09:00:00Z").toEpochMilli()
    private val zone = ZoneId.of("Asia/Kolkata")
    private fun item(title: String, category: String = "Top News", age: Long = 1_000L, source: String = "The Hindu") =
        NewsHeadline(category, title, source, now - age, "https://example.org/" + NewsPolicy.titleKey(title))
    private fun select(items: List<NewsHeadline>, rotation: Int = 0, recent: List<String> = emptyList(), announced: Set<String> = emptySet()) =
        BalancedNewsPolicy.select(items, announced, recent, false, now, zone, rotation)
    private fun mixed() = listOf(
        item("Chennai metro opens a new corridor for commuters"),
        item("Madurai government hospital opens a new treatment centre"),
        item("Coimbatore schools introduce a new breakfast programme"),
        item("India parliament approves new national education legislation"),
        item("Japan government announces new international trade agreement"),
        item("NASA launches a new satellite to study distant stars", "Science"),
        item("Android malware steals banking credentials through fake applications", "Cyber"),
        item("New wildlife conservation project protects endangered species", "Environment"),
    )

    @Test fun fullBulletinHasExactlyTheApprovedMix() {
        val result = select(mixed())
        assertEquals(8, result.size)
        BalancedNewsPolicy.Category.values().forEach { category -> assertEquals(category.quota, result.count { BalancedNewsPolicy.category(it) == category }) }
    }
    @Test fun missingCategoriesNeverBecomeGeneralNewsFiller() {
        assertEquals(3, select((1..12).map { item("Chennai opens public service centre number $it for residents") }).size)
    }
    @Test fun screenshotRoundupsAreRejectedInBothHourlyVariants() {
        for (hour in listOf(1, 2)) {
            val title = "Today Headlines $hour PM-30.10.26 | தலைப்புச் செய்திகள் | CM Vijay | Devar Jayanthi | Guru Poojai"
            assertTrue(BalancedNewsPolicy.isJunk(title))
            assertEquals("", NewsSpeechSanitizer.cleanHeadline(title))
        }
    }
    @Test fun topicListsAndTeasersCannotBecomeIndividualStories() {
        assertTrue(BalancedNewsPolicy.isJunk("India | Japan | Climate conference"))
        assertTrue(BalancedNewsPolicy.isJunk("What happened to the new satellite?"))
        assertTrue(BalancedNewsPolicy.isJunk("India announces new legislation..."))
    }
    @Test fun truncationIsRejectedBeforeCleaningCanHideIt() {
        assertEquals("", NewsSpeechSanitizer.cleanHeadline("India government announces a new..."))
    }
    @Test fun adsLiveBlogsAndGalleriesAreRejected() {
        for (title in listOf("Amazon sale brings new phone offers", "Watch live India parliament proceedings", "Photo gallery shows latest science pictures", "Sponsored research project announces huge savings")) {
            assertTrue(BalancedNewsPolicy.isJunk(title))
        }
    }
    @Test fun aRealSingleStoryIsNotRejectedAsAProgramme() {
        assertFalse(BalancedNewsPolicy.isJunk("Chennai metro opens a new corridor for passengers"))
    }
    @Test fun yesterdayFutureAndMissingDatesCannotEnterBulletin() {
        val valid = mixed().first()
        assertTrue(select(listOf(valid.copy(publishedAtMs = now - 86_400_000), valid.copy(publishedAtMs = now + 1), valid.copy(publishedAtMs = 0))).isEmpty())
    }
    @Test fun freshnessUsesLocalDayNotUtcDay() {
        val localMorning = Instant.parse("2026-10-29T20:00:00Z").toEpochMilli()
        assertEquals(1, select(listOf(mixed().first().copy(publishedAtMs = localMorning))).size)
    }
    @Test fun alreadyAnnouncedAndRecentEventsAreExcluded() {
        val items = mixed()
        assertEquals(7, select(items, announced = setOf(items.first().key)).size)
        assertEquals(7, select(items, recent = listOf(items.first().title)).size)
    }
    @Test fun duplicatePublisherVariantsAreOneEvent() {
        val a = "Chennai metro opens new corridor for public transport passengers"
        val b = "Chennai metro opens new corridor for public transport passengers today"
        assertTrue(BalancedNewsPolicy.sameEvent(a, b))
        assertEquals(1, select(listOf(item(a), item(b, source = "NDTV"))).size)
    }
    @Test fun changedNumericFactsCanRepresentMaterialUpdates() {
        assertFalse(BalancedNewsPolicy.sameEvent("Chennai opens 12 new metro stations for public transport", "Chennai opens 15 new metro stations for public transport"))
    }
    @Test fun mixedTamilFeedDoesNotAutomaticallyMeanInternational() {
        assertNull(BalancedNewsPolicy.category(item("புதிய திட்டம் இன்று அதிகாரப்பூர்வமாக அறிவிக்கப்பட்டது", "செய்திகள்")))
        assertEquals(BalancedNewsPolicy.Category.STATE, BalancedNewsPolicy.category(item("சென்னையில் புதிய பேருந்து நிலையம் இன்று திறக்கப்பட்டது", "செய்திகள்")))
    }
    @Test fun aSpecialistFeedLabelDoesNotMakeAnAdvertisementScience() {
        assertNull(BalancedNewsPolicy.category(item("New smartphone available at an attractive price", "Science")))
    }
    @Test fun cyberCategoryRequiresDigitalSecurityNotAnyCrime() {
        assertNull(BalancedNewsPolicy.category(item("Police investigate land fraud involving local property dealers")))
        assertEquals(BalancedNewsPolicy.Category.CYBER, BalancedNewsPolicy.category(mixed()[6]))
    }
    @Test fun sciencePreferencesRotateWithoutChangingQuotas() {
        val a = item("NASA launches a new satellite to study distant stars", "Science")
        val b = item("New invention helps doctors detect disease earlier", "Science")
        assertEquals(a, select(listOf(a, b), rotation = 0).single())
        assertEquals(b, select(listOf(a, b), rotation = 1).single())
    }
    @Test fun cyberAndNaturePreferencesRotate() {
        val cyber = item("UPI phishing scam steals mobile banking credentials", "Cyber")
        val nature = item("New wildlife conservation project protects endangered species", "Environment")
        assertEquals(1, BalancedNewsPolicy.subtopicScore(cyber, 2))
        assertEquals(1, BalancedNewsPolicy.subtopicScore(nature, 1))
    }
    @Test fun newestStoryWinsWhenSubtopicsAreEqual() {
        val a = mixed().last()
        val b = item("Climate study reveals changes in seasonal rainfall patterns", "Environment", age = 100)
        assertEquals(b, select(listOf(a, b), rotation = 2).single())
    }
    @Test fun categoryReserveProtectsSpecialistsFromGeneralFeedFlood() {
        val state = (1..100).map { item("Chennai opens public service centre number $it for residents") }
        val reserved = BalancedNewsPolicy.reserveCategories(state + mixed().takeLast(3), 80)
        assertTrue(reserved.containsAll(mixed().takeLast(3)))
        assertEquals(80, reserved.size)
    }
    @Test fun englishCannotLeakIntoTamilBulletins() {
        assertTrue(BalancedNewsPolicy.select(mixed(), emptySet(), emptyList(), true, now, zone, 0).isEmpty())
    }
    @Test fun unknownAggregatedPublishersAreNotTrustedByDefault() {
        assertTrue(BalancedNewsPolicy.trustedAggregatedPublisher("BBC News தமிழ்"))
        assertFalse(BalancedNewsPolicy.trustedAggregatedPublisher("random scam blog"))
        assertFalse(BalancedNewsPolicy.trustedAggregatedPublisher("The Hindu fake giveaway"))
    }
    @Test fun parserKeepsBoundedPublisherDescriptionButNotUpdatedAsPublished() {
        val xml = """<rss><channel><item><title>NASA launches new satellite to study stars</title><link>https://example.org/a</link><pubDate>Fri, 30 Oct 2026 08:00:00 GMT</pubDate><description>NASA launched a satellite to study distant stars.</description></item></channel></rss>"""
        assertEquals("NASA launched a satellite to study distant stars.", NewsPolicy.parseFeed(xml, "Science", "NASA").single().description)
        assertTrue(NewsPolicy.parseFeed(xml.replace("NASA launches new satellite to study stars", "Fresh story from an unknown publisher"), "Science", "Google News Tamil").isEmpty())
    }
    @Test fun candidatePoolHasReplacementsBeforeEditing() {
        val candidates = (1..12).map { item("Chennai opens public service centre number $it for residents") }
        val pool = BalancedNewsPolicy.select(candidates, emptySet(), emptyList(), false, now, zone, 0, perCategoryMultiplier = 3)
        assertEquals(9, pool.size)
        assertEquals(3, select(pool.drop(3)).size)
    }
    @Test fun validatedTamilTranslationKeepsOriginalSourceIdentity() {
        val original = mixed()[5]
        val translated = original.copy(spokenText = "புதிய செயற்கைக்கோள் விண்ணில் இன்று செலுத்தப்பட்டது", translated = true)
        val selected = BalancedNewsPolicy.select(listOf(translated), emptySet(), emptyList(), true, now, zone, 0)
        assertEquals(original.key, selected.single().key)
        assertEquals(original.link, selected.single().link)
    }
    @Test fun unvalidatedEnglishCannotEnterTamilThroughFallback() {
        assertTrue(BalancedNewsPolicy.select(listOf(mixed()[5].copy(translated = true)), emptySet(), emptyList(), true, now, zone, 0).isEmpty())
    }
    @Test fun descriptionCanSupplyCategoryEvidenceMissingFromTitle() {
        val story = item("New material improves efficiency in laboratory trials").copy(description = "Researchers report a scientific discovery in a preliminary study.")
        assertEquals(BalancedNewsPolicy.Category.SCIENCE, BalancedNewsPolicy.category(story))
    }
    @Test fun invalidCandidateExpansionIsRejected() {
        var rejected = false
        try { BalancedNewsPolicy.select(mixed(), emptySet(), emptyList(), false, now, zone, 0, perCategoryMultiplier = 20) } catch (_: IllegalArgumentException) { rejected = true }
        assertTrue(rejected)
    }

}
