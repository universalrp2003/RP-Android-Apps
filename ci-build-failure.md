# CI build failure excerpt

## Kotlin / resource errors
```
```

## Gradle summary
```
[Incubating] Problems report is available at: file:///home/runner/work/RP-Android-Apps/RP-Android-Apps/build/reports/problems/problems-report.html

FAILURE: Build failed with an exception.

* What went wrong:
Execution failed for task ':cleaner:testDebugUnitTest'.
> There were failing tests. See the report at: file:///home/runner/work/RP-Android-Apps/RP-Android-Apps/cleaner/build/reports/tests/testDebugUnitTest/index.html

* Try:
> Run with --scan to get full insights.

Deprecated Gradle features were used in this build, making it incompatible with Gradle 9.0.

You can use '--warning-mode all' to show the individual deprecation warnings and determine if they come from your own scripts or plugins.

For more on this, please refer to https://docs.gradle.org/8.14.3/userguide/command_line_interface.html#sec:command_line_warnings in the Gradle documentation.

BUILD FAILED in 3m 26s
833 actionable tasks: 384 executed, 449 from cache
```
com.universalrp.cleansweep.data.FeatureDiscoveryTest :: futureNotNowDoesNotActivate
    java.lang.AssertionError: Failed to fetch maven artifact org.robolectric:android-all-instrumented:14-robolectric-10818077-i7

com.universalrp.cleansweep.data.FeatureDiscoveryTest :: introductionPendingOnFirstOpening
    java.lang.AssertionError: Failed to fetch maven artifact org.robolectric:android-all-instrumented:14-robolectric-10818077-i7

com.universalrp.cleansweep.data.FeatureDiscoveryTest :: futureAskFirstRequiresExplicitEnable
    java.lang.AssertionError: Failed to fetch maven artifact org.robolectric:android-all-instrumented:14-robolectric-10818077-i7

com.universalrp.cleansweep.data.FeatureDiscoveryTest :: savedHumourSelectionIsRemovedWithoutChangingOtherChoices
    java.lang.AssertionError: Failed to fetch maven artifact org.robolectric:android-all-instrumented:14-robolectric-10818077-i7

com.universalrp.cleansweep.data.FeatureDiscoveryTest :: unknownOldCategoryNamesDoNotBreakDefault
    java.lang.AssertionError: Failed to fetch maven artifact org.robolectric:android-all-instrumented:14-robolectric-10818077-i7

com.universalrp.cleansweep.data.FeatureDiscoveryTest :: acknowledgementCannotBypassFutureConsent
    java.lang.AssertionError: Failed to fetch maven artifact org.robolectric:android-all-instrumented:14-robolectric-10818077-i7

com.universalrp.cleansweep.data.FeatureDiscoveryTest :: turnOffFromNoticePersists
    java.lang.AssertionError: Failed to fetch maven artifact org.robolectric:android-all-instrumented:14-robolectric-10818077-i7

com.universalrp.cleansweep.data.FeatureDiscoveryTest :: explicitSilentNotificationEnableDoesNotTurnNewsBackOn
    java.lang.AssertionError: Failed to fetch maven artifact org.robolectric:android-all-instrumented:14-robolectric-10818077-i7

com.universalrp.cleansweep.data.FeatureDiscoveryTest :: oldEmptyDefaultSelectionUsesNewAllTopicsDefault
    java.lang.AssertionError: Failed to fetch maven artifact org.robolectric:android-all-instrumented:14-robolectric-10818077-i7

com.universalrp.cleansweep.data.FeatureDiscoveryTest :: securityNotNowPreservesPreviouslyEnabledHistory
    java.lang.AssertionError: Failed to fetch maven artifact org.robolectric:android-all-instrumented:14-robolectric-10818077-i7

com.universalrp.cleansweep.data.FeatureDiscoveryTest :: oldDefaultOffWithoutStoredChoiceBecomesOnWithoutResettingCounts
    java.lang.AssertionError: Failed to fetch maven artifact org.robolectric:android-all-instrumented:14-robolectric-10818077-i7

com.universalrp.cleansweep.data.FeatureDiscoveryTest :: introductionNeverResetsThreeReadMemory
    java.lang.AssertionError: Failed to fetch maven artifact org.robolectric:android-all-instrumented:14-robolectric-10818077-i7

com.universalrp.cleansweep.data.FeatureDiscoveryTest :: acknowledgingDoesNotTurnPreviousOffBackOn
    java.lang.AssertionError: Failed to fetch maven artifact org.robolectric:android-all-instrumented:14-robolectric-10818077-i7

com.universalrp.cleansweep.data.FeatureDiscoveryTest :: securityIntroductionAppearsAfterDisasterAndStartsOff
    java.lang.AssertionError: Failed to fetch maven artifact org.robolectric:android-all-instrumented:14-robolectric-10818077-i7

com.universalrp.cleansweep.data.FeatureDiscoveryTest :: acknowledgeDurableAndNotRepeated
    java.lang.AssertionError: Failed to fetch maven artifact org.robolectric:android-all-instrumented:14-robolectric-10818077-i7

com.universalrp.cleansweep.data.FeatureDiscoveryTest :: explicitSecurityEnablePersistsWithoutChangingDisaster
    java.lang.AssertionError: Failed to fetch maven artifact org.robolectric:android-all-instrumented:14-robolectric-10818077-i7

com.universalrp.cleansweep.data.FeatureDiscoveryTest :: futureAskFirstFeaturesDefaultOff
    java.lang.AssertionError: Failed to fetch maven artifact org.robolectric:android-all-instrumented:14-robolectric-10818077-i7

com.universalrp.cleansweep.data.FeatureDiscoveryTest :: defaultOnNoticeAcknowledgementPreservesCurrentState
    java.lang.AssertionError: Failed to fetch maven artifact org.robolectric:android-all-instrumented:14-robolectric-10818077-i7

com.universalrp.cleansweep.data.FeatureDiscoveryTest :: notNowSecurityNeverEnablesHistory
    java.lang.AssertionError: Failed to fetch maven artifact org.robolectric:android-all-instrumented:14-robolectric-10818077-i7

com.universalrp.cleansweep.data.FeatureDiscoveryTest :: silentNotificationsAreAskFirstAndNotNowStaysOff
    java.lang.AssertionError: Failed to fetch maven artifact org.robolectric:android-all-instrumented:14-robolectric-10818077-i7

com.universalrp.cleansweep.data.FeatureDiscoveryTest :: explicitEnableFromNoticeAllowed
    java.lang.AssertionError: Failed to fetch maven artifact org.robolectric:android-all-instrumented:14-robolectric-10818077-i7

com.universalrp.cleansweep.data.FeatureDiscoveryTest :: customisedSubsetNotReplaced
    java.lang.AssertionError: Failed to fetch maven artifact org.robolectric:android-all-instrumented:14-robolectric-10818077-i7

com.universalrp.cleansweep.data.FeatureDiscoveryTest :: allTwelveFactualNewsTopicsSelectedByDefault
    java.lang.AssertionError: Failed to fetch maven artifact org.robolectric:android-all-instrumented:14-robolectric-10818077-i7

com.universalrp.cleansweep.data.FeatureDiscoveryTest :: noSignalIsAskFirstNotNowKeepsOff
    java.lang.AssertionError: Failed to fetch maven artifact org.robolectric:android-all-instrumented:14-robolectric-10818077-i7

com.universalrp.cleansweep.data.FeatureDiscoveryTest :: previousExplicitOffPreserved
    java.lang.AssertionError: Failed to fetch maven artifact org.robolectric:android-all-instrumented:14-robolectric-10818077-i7

com.universalrp.cleansweep.data.FeatureDiscoveryTest :: noSignalEnableLeavesNewsAndVoiceOff
    java.lang.AssertionError: Failed to fetch maven artifact org.robolectric:android-all-instrumented:14-robolectric-10818077-i7

com.universalrp.cleansweep.data.FeatureDiscoveryTest :: languagePreferenceNotResetByNewDefault
    java.lang.AssertionError: Failed to fetch maven artifact org.robolectric:android-all-instrumented:14-robolectric-10818077-i7

com.universalrp.cleansweep.data.FeatureDiscoveryTest :: disasterOnForUnconfiguredNewAndUpdatedUsers
    java.lang.AssertionError: Failed to fetch maven artifact org.robolectric:android-all-instrumented:14-robolectric-10818077-i7

com.universalrp.cleansweep.data.FeatureDiscoveryTest :: previousExplicitOnPreserved
    java.lang.AssertionError: Failed to fetch maven artifact org.robolectric:android-all-instrumented:14-robolectric-10818077-i7

com.universalrp.cleansweep.data.HomePlacePreferencesTest :: sameHomeSaveDoesNotResetForecast
    java.lang.AssertionError: Failed to fetch maven artifact org.robolectric:android-all-instrumented:14-robolectric-10818077-i7

com.universalrp.cleansweep.data.HomePlacePreferencesTest :: removalResetsEpisodeAndThrottle
    java.lang.AssertionError: Failed to fetch maven artifact org.robolectric:android-all-instrumented:14-robolectric-10818077-i7

com.universalrp.cleansweep.data.HomePlacePreferencesTest :: removalPreservesKeyAndExplicitVoiceChoice
    java.lang.AssertionError: Failed to fetch maven artifact org.robolectric:android-all-instrumented:14-robolectric-10818077-i7

com.universalrp.cleansweep.data.HomePlacePreferencesTest :: replacementClearsOnlyHomeDerivedState
    java.lang.AssertionError: Failed to fetch maven artifact org.robolectric:android-all-instrumented:14-robolectric-10818077-i7

com.universalrp.cleansweep.data.HomePlacePreferencesTest :: removalClearsHomeAndDerivedResults
    java.lang.AssertionError: Failed to fetch maven artifact org.robolectric:android-all-instrumented:14-robolectric-10818077-i7

com.universalrp.cleansweep.data.HomePlacePreferencesTest :: removalIsIdempotentAndAllowsResaving
    java.lang.AssertionError: Failed to fetch maven artifact org.robolectric:android-all-instrumented:14-robolectric-10818077-i7

com.universalrp.cleansweep.data.HomePlacePreferencesTest :: removalPreservesGpsOtherPlacesAndLead
    java.lang.AssertionError: Failed to fetch maven artifact org.robolectric:android-all-instrumented:14-robolectric-10818077-i7

com.universalrp.cleansweep.data.NetworkSplitDayTest :: incrementStartsNewDayRatherThanAddingToYesterday
    java.lang.AssertionError: Failed to fetch maven artifact org.robolectric:android-all-instrumented:14-robolectric-10818077-i7

com.universalrp.cleansweep.data.NetworkSplitDayTest :: staleBaselineIsNeverCarriedIntoToday
    java.lang.AssertionError: Failed to fetch maven artifact org.robolectric:android-all-instrumented:14-robolectric-10818077-i7

com.universalrp.cleansweep.data.NetworkSplitDayTest :: firstNewBaselineDoesNotBackfillHistory
    java.lang.AssertionError: Failed to fetch maven artifact org.robolectric:android-all-instrumented:14-robolectric-10818077-i7

com.universalrp.cleansweep.data.NetworkSplitDayTest :: repeatedReadsKeepTodaysSamplesAndBaseline
    java.lang.AssertionError: Failed to fetch maven artifact org.robolectric:android-all-instrumented:14-robolectric-10818077-i7

com.universalrp.cleansweep.data.NetworkSplitDayTest :: futureDayOrZoneChangeAlsoInvalidates
    java.lang.AssertionError: Failed to fetch maven artifact org.robolectric:android-all-instrumented:14-robolectric-10818077-i7

com.universalrp.cleansweep.data.NetworkSplitDayTest :: readFourGResetsWithoutServiceOrTraffic
    java.lang.AssertionError: Failed to fetch maven artifact org.robolectric:android-all-instrumented:14-robolectric-10818077-i7

com.universalrp.cleansweep.data.NetworkSplitDayTest :: statusCannotShowYesterday
    java.lang.AssertionError: Failed to fetch maven artifact org.robolectric:android-all-instrumented:14-robolectric-10818077-i7

com.universalrp.cleansweep.data.NetworkSplitDayTest :: readFiveGResetsWithoutService
    java.lang.AssertionError: Failed to fetch maven artifact org.robolectric:android-all-instrumented:14-robolectric-10818077-i7

com.universalrp.cleansweep.data.NewsDisasterPolicyTest :: homeExplicitNotCurrentPhone
    java.lang.AssertionError: Failed to fetch maven artifact org.robolectric:android-all-instrumented:14-robolectric-10818077-i7

com.universalrp.cleansweep.data.NewsDisasterPolicyTest :: polygonIsNotAReportedPosition
    java.lang.AssertionError: Failed to fetch maven artifact org.robolectric:android-all-instrumented:14-robolectric-10818077-i7

com.universalrp.cleansweep.data.NewsDisasterPolicyTest :: acceptsRecentCurrentReports
    java.lang.AssertionError: Failed to fetch maven artifact org.robolectric:android-all-instrumented:14-robolectric-10818077-i7

com.universalrp.cleansweep.data.NewsDisasterPolicyTest :: compactAddonNamesPlaceOnlyOnceForAllThreeEvents
    java.lang.AssertionError: Failed to fetch maven artifact org.robolectric:android-all-instrumented:14-robolectric-10818077-i7

com.universalrp.cleansweep.data.NewsDisasterPolicyTest :: latestActualNotForecastMaximum
    java.lang.AssertionError: Failed to fetch maven artifact org.robolectric:android-all-instrumented:14-robolectric-10818077-i7

com.universalrp.cleansweep.data.NewsDisasterPolicyTest :: forecastNeverSuppliesWindOrPosition
    java.lang.AssertionError: Failed to fetch maven artifact org.robolectric:android-all-instrumented:14-robolectric-10818077-i7

com.universalrp.cleansweep.data.NewsDisasterPolicyTest :: capsThreeSuccessfulReadsNotAttempts
    java.lang.AssertionError: Failed to fetch maven artifact org.robolectric:android-all-instrumented:14-robolectric-10818077-i7

com.universalrp.cleansweep.data.NewsDisasterPolicyTest :: badCoordinateExcluded
    java.lang.AssertionError: Failed to fetch maven artifact org.robolectric:android-all-instrumented:14-robolectric-10818077-i7

com.universalrp.cleansweep.data.NewsDisasterPolicyTest :: unitlessMovementZeroNeverStationary
    java.lang.AssertionError: Failed to fetch maven artifact org.robolectric:android-all-instrumented:14-robolectric-10818077-i7

com.universalrp.cleansweep.data.NewsDisasterPolicyTest :: providerInstructionsNotSpokenAsName
    java.lang.AssertionError: Failed to fetch maven artifact org.robolectric:android-all-instrumented:14-robolectric-10818077-i7

com.universalrp.cleansweep.data.NewsDisasterPolicyTest :: cycloneHasNameReferencePlaceAndFieldSources
    java.lang.AssertionError: Failed to fetch maven artifact org.robolectric:android-all-instrumented:14-robolectric-10818077-i7

com.universalrp.cleansweep.data.NewsDisasterPolicyTest :: foreignStormTimelineRejected
    java.lang.AssertionError: Failed to fetch maven artifact org.robolectric:android-all-instrumented:14-robolectric-10818077-i7

com.universalrp.cleansweep.data.NewsDisasterPolicyTest :: staleActualRejected
    java.lang.AssertionError: Failed to fetch maven artifact org.robolectric:android-all-instrumented:14-robolectric-10818077-i7

com.universalrp.cleansweep.data.NewsDisasterPolicyTest :: compactSpeechDoesNotMutateStoredReferenceLabel
    java.lang.AssertionError: Failed to fetch maven artifact org.robolectric:android-all-instrumented:14-robolectric-10818077-i7

com.universalrp.cleansweep.data.NewsDisasterPolicyTest :: unknownPlaceNameNotClaimedToBeHomeOrGpsTown
    java.lang.AssertionError: Failed to fetch maven artifact org.robolectric:android-all-instrumented:14-robolectric-10818077-i7

com.universalrp.cleansweep.data.NewsDisasterPolicyTest :: timelineHostAndPathMustRemainAllowlisted
    java.lang.AssertionError: Failed to fetch maven artifact org.robolectric:android-all-instrumented:14-robolectric-10818077-i7

com.universalrp.cleansweep.data.NewsDisasterPolicyTest :: emptyReportsNeverAllClear
    java.lang.AssertionError: Failed to fetch maven artifact org.robolectric:android-all-instrumented:14-robolectric-10818077-i7

com.universalrp.cleansweep.data.NewsDisasterPolicyTest :: futureReportExcluded
    java.lang.AssertionError: Failed to fetch maven artifact org.robolectric:android-all-instrumented:14-robolectric-10818077-i7

com.universalrp.cleansweep.data.NewsDisasterPolicyTest :: phoneSampleTimeExplicit
    java.lang.AssertionError: Failed to fetch maven artifact org.robolectric:android-all-instrumented:14-robolectric-10818077-i7

com.universalrp.cleansweep.data.NewsDisasterPolicyTest :: otherMeteorologicalSourcesCannotSupplyObservedSpeech
    java.lang.AssertionError: Failed to fetch maven artifact org.robolectric:android-all-instrumented:14-robolectric-10818077-i7

com.universalrp.cleansweep.data.NewsDisasterPolicyTest :: supportsFloodAndEarthquake
    java.lang.AssertionError: Failed to fetch maven artifact org.robolectric:android-all-instrumented:14-robolectric-10818077-i7

com.universalrp.cleansweep.data.NewsDisasterPolicyTest :: nonCurrentActualRowNotUsed
    java.lang.AssertionError: Failed to fetch maven artifact org.robolectric:android-all-instrumented:14-robolectric-10818077-i7

com.universalrp.cleansweep.data.NewsDisasterPolicyTest :: updatedEpisodeCannotChangeIdentity
    java.lang.AssertionError: Failed to fetch maven artifact org.robolectric:android-all-instrumented:14-robolectric-10818077-i7

com.universalrp.cleansweep.data.NewsDisasterPolicyTest :: oldReportExcluded
    java.lang.AssertionError: Failed to fetch maven artifact org.robolectric:android-all-instrumented:14-robolectric-10818077-i7

com.universalrp.cleansweep.data.NewsDisasterPolicyTest :: foreignEventDetailsRejected
    java.lang.AssertionError: Failed to fetch maven artifact org.robolectric:android-all-instrumented:14-robolectric-10818077-i7

com.universalrp.cleansweep.data.NewsDisasterPolicyTest :: withdrawnReportExcluded
    java.lang.AssertionError: Failed to fetch maven artifact org.robolectric:android-all-instrumented:14-robolectric-10818077-i7

com.universalrp.cleansweep.data.NewsDisasterPolicyTest :: movementAndLandfallNeverInventedFromWindOrDistance
    java.lang.AssertionError: Failed to fetch maven artifact org.robolectric:android-all-instrumented:14-robolectric-10818077-i7

com.universalrp.cleansweep.data.NewsDisasterPolicyTest :: eventKindsRemainDistinct
    java.lang.AssertionError: Failed to fetch maven artifact org.robolectric:android-all-instrumented:14-robolectric-10818077-i7

com.universalrp.cleansweep.data.NewsDisasterPolicyTest :: tamilLabelsHomeAndUnavailable
    java.lang.AssertionError: Failed to fetch maven artifact org.robolectric:android-all-instrumented:14-robolectric-10818077-i7

com.universalrp.cleansweep.data.NewsDisasterPolicyTest :: futureActualRejected
    java.lang.AssertionError: Failed to fetch maven artifact org.robolectric:android-all-instrumented:14-robolectric-10818077-i7

com.universalrp.cleansweep.data.NewsDisasterPolicyTest :: endedStormExcludedDespiteFreshModification
    java.lang.AssertionError: Failed to fetch maven artifact org.robolectric:android-all-instrumented:14-robolectric-10818077-i7

com.universalrp.cleansweep.data.NewsDisasterPolicyTest :: verifiedJtwcAdvisoryTimelineAccepted
    java.lang.AssertionError: Failed to fetch maven artifact org.robolectric:android-all-instrumented:14-robolectric-10818077-i7

com.universalrp.cleansweep.data.NewsDisasterPolicyTest :: scopeIsRegionalNotImpactRadius
    java.lang.AssertionError: Failed to fetch maven artifact org.robolectric:android-all-instrumented:14-robolectric-10818077-i7

com.universalrp.cleansweep.data.NewsDisasterPolicyTest :: unsupportedWindNotZero
    java.lang.AssertionError: Failed to fetch maven artifact org.robolectric:android-all-instrumented:14-robolectric-10818077-i7

com.universalrp.cleansweep.data.NewsDisasterUpdatesTest :: lastMemorySlotCannotOverflow
    java.lang.AssertionError: Failed to fetch maven artifact org.robolectric:android-all-instrumented:14-robolectric-10818077-i7

com.universalrp.cleansweep.data.NewsDisasterUpdatesTest :: modifiedFeedTimeAndDistanceDoNotCreateAdvisory
    java.lang.AssertionError: Failed to fetch maven artifact org.robolectric:android-all-instrumented:14-robolectric-10818077-i7

com.universalrp.cleansweep.data.NewsDisasterUpdatesTest :: newerCycloneAdvisoryBecomesEligible
    java.lang.AssertionError: Failed to fetch maven artifact org.robolectric:android-all-instrumented:14-robolectric-10818077-i7

com.universalrp.cleansweep.data.NewsDisasterUpdatesTest :: explicitOffStaysSilent
    java.lang.AssertionError: Failed to fetch maven artifact org.robolectric:android-all-instrumented:14-robolectric-10818077-i7

com.universalrp.cleansweep.data.NewsDisasterUpdatesTest :: oldCancellationCannotReleaseNewAdvisory
    java.lang.AssertionError: Failed to fetch maven artifact org.robolectric:android-all-instrumented:14-robolectric-10818077-i7

com.universalrp.cleansweep.data.NewsDisasterUpdatesTest :: missingFreshGpsUsesLabelledHome
    java.lang.AssertionError: Failed to fetch maven artifact org.robolectric:android-all-instrumented:14-robolectric-10818077-i7

com.universalrp.cleansweep.data.NewsDisasterUpdatesTest :: boundedCycloneMemoryDoesNotEvictOldStorms
    java.lang.AssertionError: Failed to fetch maven artifact org.robolectric:android-all-instrumented:14-robolectric-10818077-i7

com.universalrp.cleansweep.data.NewsDisasterUpdatesTest :: corruptMemoryFailsClosedNotReset
    java.lang.AssertionError: Failed to fetch maven artifact org.robolectric:android-all-instrumented:14-robolectric-10818077-i7

com.universalrp.cleansweep.data.NewsDisasterUpdatesTest :: oldCycloneAdvisoryNeverReturns
    java.lang.AssertionError: Failed to fetch maven artifact org.robolectric:android-all-instrumented:14-robolectric-10818077-i7

com.universalrp.cleansweep.data.NewsDisasterUpdatesTest :: concurrentManualAutoCannotReserveSameEvent
    java.lang.AssertionError: Failed to fetch maven artifact org.robolectric:android-all-instrumented:14-robolectric-10818077-i7

com.universalrp.cleansweep.data.NewsDisasterUpdatesTest :: homeEditWithdrawsPreparedDistance
    java.lang.AssertionError: Failed to fetch maven artifact org.robolectric:android-all-instrumented:14-robolectric-10818077-i7

com.universalrp.cleansweep.data.NewsDisasterUpdatesTest :: withdrawalCancelsPreparedAndDoesNotConsume
    java.lang.AssertionError: Failed to fetch maven artifact org.robolectric:android-all-instrumented:14-robolectric-10818077-i7

com.universalrp.cleansweep.data.NewsDisasterUpdatesTest :: maxThreeDistinctEventsEachBulletin
    java.lang.AssertionError: Failed to fetch maven artifact org.robolectric:android-all-instrumented:14-robolectric-10818077-i7

com.universalrp.cleansweep.data.NewsDisasterUpdatesTest :: savedReferenceTownIsReturnedWithoutMovingCoordinates
    java.lang.AssertionError: Failed to fetch maven artifact org.robolectric:android-all-instrumented:14-robolectric-10818077-i7

com.universalrp.cleansweep.data.NewsDisasterUpdatesTest :: languageAndHomeChangeDoNotResetCount
    java.lang.AssertionError: Failed to fetch maven artifact org.robolectric:android-all-instrumented:14-robolectric-10818077-i7

com.universalrp.cleansweep.data.NewsDisasterUpdatesTest :: duplicateFinalCallbackCannotIncrementTwice
    java.lang.AssertionError: Failed to fetch maven artifact org.robolectric:android-all-instrumented:14-robolectric-10818077-i7

com.universalrp.cleansweep.data.NewsDisasterUpdatesTest :: legacyThreeCountsDoNotSuppressNewAdvisory
    java.lang.AssertionError: Failed to fetch maven artifact org.robolectric:android-all-instrumented:14-robolectric-10818077-i7

com.universalrp.cleansweep.data.NewsDisasterUpdatesTest :: cycloneMemorySurvivesReloadToggleLanguageAndHome
    java.lang.AssertionError: Failed to fetch maven artifact org.robolectric:android-all-instrumented:14-robolectric-10818077-i7

com.universalrp.cleansweep.data.NewsDisasterUpdatesTest :: cycloneRequiresObservedAdvisoryTimestamp
    java.lang.AssertionError: Failed to fetch maven artifact org.robolectric:android-all-instrumented:14-robolectric-10818077-i7

com.universalrp.cleansweep.data.NewsDisasterUpdatesTest :: preparingCycloneDoesNotMarkDelivered
    java.lang.AssertionError: Failed to fetch maven artifact org.robolectric:android-all-instrumented:14-robolectric-10818077-i7

com.universalrp.cleansweep.data.NewsDisasterUpdatesTest :: corruptCycloneLedgerFailsClosedButFloodStillWorks
    java.lang.AssertionError: Failed to fetch maven artifact org.robolectric:android-all-instrumented:14-robolectric-10818077-i7

com.universalrp.cleansweep.data.NewsDisasterUpdatesTest :: malformedCycloneLedgerDoesNotResetToUnheard
    java.lang.AssertionError: Failed to fetch maven artifact org.robolectric:android-all-instrumented:14-robolectric-10818077-i7

com.universalrp.cleansweep.data.NewsDisasterUpdatesTest :: cancelledCycloneCanRetry
    java.lang.AssertionError: Failed to fetch maven artifact org.robolectric:android-all-instrumented:14-robolectric-10818077-i7

com.universalrp.cleansweep.data.NewsDisasterUpdatesTest :: preparingDoesNotConsume
    java.lang.AssertionError: Failed to fetch maven artifact org.robolectric:android-all-instrumented:14-robolectric-10818077-i7

com.universalrp.cleansweep.data.NewsDisasterUpdatesTest :: oneMemoryRecordPerStormNotPerAdvisory
    java.lang.AssertionError: Failed to fetch maven artifact org.robolectric:android-all-instrumented:14-robolectric-10818077-i7

com.universalrp.cleansweep.data.NewsDisasterUpdatesTest :: newerCycloneWithdrawsOlderPending
    java.lang.AssertionError: Failed to fetch maven artifact org.robolectric:android-all-instrumented:14-robolectric-10818077-i7

com.universalrp.cleansweep.data.NewsDisasterUpdatesTest :: offThenOnCannotResurrectPreparedDraft
    java.lang.AssertionError: Failed to fetch maven artifact org.robolectric:android-all-instrumented:14-robolectric-10818077-i7

com.universalrp.cleansweep.data.NewsDisasterUpdatesTest :: unnamedSavedHomeMustBeConfirmedBeforeDisasterSpeech
    java.lang.AssertionError: Failed to fetch maven artifact org.robolectric:android-all-instrumented:14-robolectric-10818077-i7

com.universalrp.cleansweep.data.NewsDisasterUpdatesTest :: concurrentSameCycloneAdvisoryExcluded
    java.lang.AssertionError: Failed to fetch maven artifact org.robolectric:android-all-instrumented:14-robolectric-10818077-i7

com.universalrp.cleansweep.data.NewsDisasterUpdatesTest :: interruptedReadKeepsItsSlot
    java.lang.AssertionError: Failed to fetch maven artifact org.robolectric:android-all-instrumented:14-robolectric-10818077-i7

com.universalrp.cleansweep.data.NewsDisasterUpdatesTest :: newestOfDuplicateStormEntriesWins
    java.lang.AssertionError: Failed to fetch maven artifact org.robolectric:android-all-instrumented:14-robolectric-10818077-i7

com.universalrp.cleansweep.data.NewsDisasterUpdatesTest :: oldCompletionCannotConsumeNewAdvisory
    java.lang.AssertionError: Failed to fetch maven artifact org.robolectric:android-all-instrumented:14-robolectric-10818077-i7

com.universalrp.cleansweep.data.NewsDisasterUpdatesTest :: noLocationNoAnnouncement
    java.lang.AssertionError: Failed to fetch maven artifact org.robolectric:android-all-instrumented:14-robolectric-10818077-i7

com.universalrp.cleansweep.data.NewsDisasterUpdatesTest :: fullMemoryNeverEvictsCompletedEventTombstones
    java.lang.AssertionError: Failed to fetch maven artifact org.robolectric:android-all-instrumented:14-robolectric-10818077-i7

com.universalrp.cleansweep.data.NewsDisasterUpdatesTest :: repeatedCycloneCompletionIdempotent
    java.lang.AssertionError: Failed to fetch maven artifact org.robolectric:android-all-instrumented:14-robolectric-10818077-i7

com.universalrp.cleansweep.data.NewsDisasterUpdatesTest :: floodUpdateDoesNotResetThree
    java.lang.AssertionError: Failed to fetch maven artifact org.robolectric:android-all-instrumented:14-robolectric-10818077-i7

com.universalrp.cleansweep.data.NewsDisasterUpdatesTest :: futureCycloneAdvisoryExcluded
    java.lang.AssertionError: Failed to fetch maven artifact org.robolectric:android-all-instrumented:14-robolectric-10818077-i7

com.universalrp.cleansweep.data.NewsDisasterUpdatesTest :: completedMemorySurvivesPreferenceReloadAndToggle
    java.lang.AssertionError: Failed to fetch maven artifact org.robolectric:android-all-instrumented:14-robolectric-10818077-i7

com.universalrp.cleansweep.data.NewsDisasterUpdatesTest :: cycloneOncePerSuccessfulAdvisory
    java.lang.AssertionError: Failed to fetch maven artifact org.robolectric:android-all-instrumented:14-robolectric-10818077-i7

com.universalrp.cleansweep.data.NewsDisasterUpdatesTest :: staleDraftWithdrawn
    java.lang.AssertionError: Failed to fetch maven artifact org.robolectric:android-all-instrumented:14-robolectric-10818077-i7

com.universalrp.cleansweep.data.NewsDisasterUpdatesTest :: exactlyThreeCompletedBulletins
    java.lang.AssertionError: Failed to fetch maven artifact org.robolectric:android-all-instrumented:14-robolectric-10818077-i7

com.universalrp.cleansweep.data.NewsLanguageDefaultTest :: existingCustomTopicsDoNotBlockDefaultOrGetReset
    java.lang.AssertionError: Failed to fetch maven artifact org.robolectric:android-all-instrumented:14-robolectric-10818077-i7

com.universalrp.cleansweep.data.NewsLanguageDefaultTest :: unsetDefaultsToTamilAndEnglishTranslatedIntoTamil
    java.lang.AssertionError: Failed to fetch maven artifact org.robolectric:android-all-instrumented:14-robolectric-10818077-i7

com.universalrp.cleansweep.data.NewsLanguageDefaultTest :: allExplicitEarlierLanguageChoicesRemain
    java.lang.AssertionError: Failed to fetch maven artifact org.robolectric:android-all-instrumented:14-robolectric-10818077-i7

com.universalrp.cleansweep.data.NewsLanguageDefaultTest :: invalidLanguageFallsBackSafelyWithoutOverwritingStoredData
    java.lang.AssertionError: Failed to fetch maven artifact org.robolectric:android-all-instrumented:14-robolectric-10818077-i7

com.universalrp.cleansweep.data.SavedRainWatchPreferencesTest :: threeIndependentPlacesCanBeDisabledEditedAndDeletedWithoutGps
    java.lang.AssertionError: Failed to fetch maven artifact org.robolectric:android-all-instrumented:14-robolectric-10818077-i7

com.universalrp.cleansweep.data.SavedRainWatchPreferencesTest :: movingCoordinatesResetsEpisodeButRenameKeepsIt
    java.lang.AssertionError: Failed to fetch maven artifact org.robolectric:android-all-instrumented:14-robolectric-10818077-i7

com.universalrp.cleansweep.data.SavedRainWatchPreferencesTest :: languageAndEmptyTopicDefaultsRemainExplicit
    java.lang.AssertionError: Failed to fetch maven artifact org.robolectric:android-all-instrumented:14-robolectric-10818077-i7

com.universalrp.cleansweep.data.SavedRainWatchPreferencesTest :: historyIsIndependentBoundedAndDeletedWithPlace
    java.lang.AssertionError: Failed to fetch maven artifact org.robolectric:android-all-instrumented:14-robolectric-10818077-i7

com.universalrp.cleansweep.data.SavedRainWatchPreferencesTest :: invalidLeadAndDuplicateLocationAreRejected
    java.lang.AssertionError: Failed to fetch maven artifact org.robolectric:android-all-instrumented:14-robolectric-10818077-i7

com.universalrp.cleansweep.data.SimpleWeatherPreferencesTest :: advancedOffPercentageOnByDefault
    java.lang.AssertionError: Failed to fetch maven artifact org.robolectric:android-all-instrumented:14-robolectric-10818077-i7

com.universalrp.cleansweep.data.SimpleWeatherPreferencesTest :: failedCheckDoesNotLookLikeDryWeather
    java.lang.AssertionError: Failed to fetch maven artifact org.robolectric:android-all-instrumented:14-robolectric-10818077-i7

com.universalrp.cleansweep.data.SimpleWeatherPreferencesTest :: gpsIsOptInAndDoesNotReplaceHome
    java.lang.AssertionError: Failed to fetch maven artifact org.robolectric:android-all-instrumented:14-robolectric-10818077-i7

com.universalrp.cleansweep.data.SimpleWeatherPreferencesTest :: independentHomeAndGpsResults
    java.lang.AssertionError: Failed to fetch maven artifact org.robolectric:android-all-instrumented:14-robolectric-10818077-i7

com.universalrp.cleansweep.data.SimpleWeatherPreferencesTest :: thresholdsSurviveAdvancedToggle
    java.lang.AssertionError: Failed to fetch maven artifact org.robolectric:android-all-instrumented:14-robolectric-10818077-i7

com.universalrp.cleansweep.data.SimpleWeatherPreferencesTest :: movementResetsOnlyGpsMemory
    java.lang.AssertionError: Failed to fetch maven artifact org.robolectric:android-all-instrumented:14-robolectric-10818077-i7

com.universalrp.cleansweep.notify.DisasterUpdateNotifierTest :: corruptAndFullMemoryFailClosed
    java.lang.AssertionError: Failed to fetch maven artifact org.robolectric:android-all-instrumented:14-robolectric-10818077-i7

com.universalrp.cleansweep.notify.DisasterUpdateNotifierTest :: floodRefreshDoesNotBecomeNewEvent
    java.lang.AssertionError: Failed to fetch maven artifact org.robolectric:android-all-instrumented:14-robolectric-10818077-i7

com.universalrp.cleansweep.notify.DisasterUpdateNotifierTest :: untrustedOfficialSourceCannotBecomeTapTarget
    java.lang.AssertionError: Failed to fetch maven artifact org.robolectric:android-all-instrumented:14-robolectric-10818077-i7

com.universalrp.cleansweep.notify.DisasterUpdateNotifierTest :: explicitOffBlocksDelivery
    java.lang.AssertionError: Failed to fetch maven artifact org.robolectric:android-all-instrumented:14-robolectric-10818077-i7

com.universalrp.cleansweep.notify.DisasterUpdateNotifierTest :: optInDefaultsOff
    java.lang.AssertionError: Failed to fetch maven artifact org.robolectric:android-all-instrumented:14-robolectric-10818077-i7

com.universalrp.cleansweep.notify.DisasterUpdateNotifierTest :: blockedChannelDoesNotConsumeReceipt
    java.lang.AssertionError: Failed to fetch maven artifact org.robolectric:android-all-instrumented:14-robolectric-10818077-i7

com.universalrp.cleansweep.notify.DisasterUpdateNotifierTest :: deniedPermissionDoesNotConsumeAndOffCancelsOnlyOwnCards
    java.lang.AssertionError: Failed to fetch maven artifact org.robolectric:android-all-instrumented:14-robolectric-10818077-i7

com.universalrp.cleansweep.notify.DisasterUpdateNotifierTest :: newsDisasterOffIsRespected
    java.lang.AssertionError: Failed to fetch maven artifact org.robolectric:android-all-instrumented:14-robolectric-10818077-i7

com.universalrp.cleansweep.notify.DisasterUpdateNotifierTest :: earthquakeRefreshDoesNotRepeat
    java.lang.AssertionError: Failed to fetch maven artifact org.robolectric:android-all-instrumented:14-robolectric-10818077-i7

com.universalrp.cleansweep.notify.DisasterUpdateNotifierTest :: staleFutureAndUnpositionedCyclonesCannotNotify
    java.lang.AssertionError: Failed to fetch maven artifact org.robolectric:android-all-instrumented:14-robolectric-10818077-i7

com.universalrp.cleansweep.notify.DisasterUpdateNotifierTest :: unchangedReportNeverReplays
    java.lang.AssertionError: Failed to fetch maven artifact org.robolectric:android-all-instrumented:14-robolectric-10818077-i7

com.universalrp.cleansweep.notify.DisasterUpdateNotifierTest :: silentChannelNoVibrationAndTapOpensReport
    java.lang.AssertionError: Failed to fetch maven artifact org.robolectric:android-all-instrumented:14-robolectric-10818077-i7

com.universalrp.cleansweep.notify.DisasterUpdateNotifierTest :: newerCycloneOnceAndOlderSuppressed
    java.lang.AssertionError: Failed to fetch maven artifact org.robolectric:android-all-instrumented:14-robolectric-10818077-i7

com.universalrp.cleansweep.notify.DisasterUpdateNotifierTest :: noSpokenLedgerOrReservationIsConsumed
    java.lang.AssertionError: Failed to fetch maven artifact org.robolectric:android-all-instrumented:14-robolectric-10818077-i7

com.universalrp.cleansweep.notify.NoSignalBeepTest :: mutedMediaBlocks
    java.lang.AssertionError: Failed to fetch maven artifact org.robolectric:android-all-instrumented:14-robolectric-10818077-i7

com.universalrp.cleansweep.notify.NoSignalBeepTest :: callAndDndBlock
    java.lang.AssertionError: Failed to fetch maven artifact org.robolectric:android-all-instrumented:14-robolectric-10818077-i7

com.universalrp.cleansweep.notify.NoSignalBeepTest :: defaultsOff
    java.lang.AssertionError: Failed to fetch maven artifact org.robolectric:android-all-instrumented:14-robolectric-10818077-i7

com.universalrp.cleansweep.notify.NoSignalBeepTest :: quietHoursAndAirplaneBlock
    java.lang.AssertionError: Failed to fetch maven artifact org.robolectric:android-all-instrumented:14-robolectric-10818077-i7

com.universalrp.cleansweep.notify.NoSignalBeepTest :: safeGateDoesNotChangeMediaVolumeOrVoiceMaster
    java.lang.AssertionError: Failed to fetch maven artifact org.robolectric:android-all-instrumented:14-robolectric-10818077-i7

com.universalrp.cleansweep.notify.NoSignalBeepTest :: silentAndVibrateBlock
    java.lang.AssertionError: Failed to fetch maven artifact org.robolectric:android-all-instrumented:14-robolectric-10818077-i7

com.universalrp.cleansweep.notify.RoutineStatusTest :: reconciliationCannotUndoLaterStop
    java.lang.AssertionError: Failed to fetch maven artifact org.robolectric:android-all-instrumented:14-robolectric-10818077-i7

com.universalrp.cleansweep.notify.RoutineStatusTest :: fallbackAdoptedByRealService
    java.lang.AssertionError: Failed to fetch maven artifact org.robolectric:android-all-instrumented:14-robolectric-10818077-i7

com.universalrp.cleansweep.notify.RoutineStatusTest :: oldRefreshCannotResurrectOwner
    java.lang.AssertionError: Failed to fetch maven artifact org.robolectric:android-all-instrumented:14-robolectric-10818077-i7

com.universalrp.cleansweep.notify.RoutineStatusTest :: lastOwnerRemovesCard
    java.lang.AssertionError: Failed to fetch maven artifact org.robolectric:android-all-instrumented:14-robolectric-10818077-i7

com.universalrp.cleansweep.notify.RoutineStatusTest :: boundedOwnersAndRepeatedClaimIdempotent
    java.lang.AssertionError: Failed to fetch maven artifact org.robolectric:android-all-instrumented:14-robolectric-10818077-i7

com.universalrp.cleansweep.notify.RoutineStatusTest :: compactContentCombinesNewsWithoutParagraphs
    java.lang.AssertionError: Failed to fetch maven artifact org.robolectric:android-all-instrumented:14-robolectric-10818077-i7

com.universalrp.cleansweep.notify.RoutineStatusTest :: workerTeardownReconcilesRemainingOwners
    java.lang.AssertionError: Failed to fetch maven artifact org.robolectric:android-all-instrumented:14-robolectric-10818077-i7

com.universalrp.cleansweep.notify.RoutineStatusTest :: monitorAndRadioShareOneCard
    java.lang.AssertionError: Failed to fetch maven artifact org.robolectric:android-all-instrumented:14-robolectric-10818077-i7

com.universalrp.cleansweep.notify.RoutineStatusTest :: alarmAndWorkerShareOneCard
    java.lang.AssertionError: Failed to fetch maven artifact org.robolectric:android-all-instrumented:14-robolectric-10818077-i7

com.universalrp.cleansweep.notify.RoutineStatusTest :: routineReleaseDoesNotClearUrgentAlert
    java.lang.AssertionError: Failed to fetch maven artifact org.robolectric:android-all-instrumented:14-robolectric-10818077-i7

com.universalrp.cleansweep.notify.RoutineStatusTest :: radioStopPreservesMonitor
    java.lang.AssertionError: Failed to fetch maven artifact org.robolectric:android-all-instrumented:14-robolectric-10818077-i7

com.universalrp.cleansweep.notify.RoutineStatusTest :: monitorStopPreservesRadio
    java.lang.AssertionError: Failed to fetch maven artifact org.robolectric:android-all-instrumented:14-robolectric-10818077-i7

com.universalrp.cleansweep.widget.DisasterWidgetCacheTest :: manualViewDoesNotEnableNewsOrConsumeReadCounts
    java.lang.AssertionError: Failed to fetch maven artifact org.robolectric:android-all-instrumented:14-robolectric-10818077-i7

com.universalrp.cleansweep.widget.DisasterWidgetCacheTest :: boundedCorruptionFailsClosed
    java.lang.AssertionError: Failed to fetch maven artifact org.robolectric:android-all-instrumented:14-robolectric-10818077-i7

com.universalrp.cleansweep.widget.DisasterWidgetCacheTest :: failedCheckNotEmptySuccess
    java.lang.AssertionError: Failed to fetch maven artifact org.robolectric:android-all-instrumented:14-robolectric-10818077-i7

com.universalrp.cleansweep.widget.DisasterWidgetCacheTest :: partialCheckNotEmptySuccess
    java.lang.AssertionError: Failed to fetch maven artifact org.robolectric:android-all-instrumented:14-robolectric-10818077-i7

com.universalrp.cleansweep.widget.DisasterWidgetCacheTest :: distantGpsAndUnpositionedCycloneNotShown
    java.lang.AssertionError: Failed to fetch maven artifact org.robolectric:android-all-instrumented:14-robolectric-10818077-i7

com.universalrp.cleansweep.widget.DisasterWidgetCacheTest :: emptyCheckExplicitlyNotAllClear
    java.lang.AssertionError: Failed to fetch maven artifact org.robolectric:android-all-instrumented:14-robolectric-10818077-i7

com.universalrp.cleansweep.widget.DisasterWidgetCacheTest :: uncheckedHasVisibleReason
    java.lang.AssertionError: Failed to fetch maven artifact org.robolectric:android-all-instrumented:14-robolectric-10818077-i7

com.universalrp.cleansweep.widget.DisasterWidgetCacheTest :: cacheDoesNotConsumeSpokenOrNotificationLedgers
    java.lang.AssertionError: Failed to fetch maven artifact org.robolectric:android-all-instrumented:14-robolectric-10818077-i7

com.universalrp.cleansweep.widget.DisasterWidgetCacheTest :: failedRefreshQualifiesCachedReport
    java.lang.AssertionError: Failed to fetch maven artifact org.robolectric:android-all-instrumented:14-robolectric-10818077-i7

com.universalrp.cleansweep.widget.DisasterWidgetCacheTest :: emptyIsUnknownNotAllClear
    java.lang.AssertionError: Failed to fetch maven artifact org.robolectric:android-all-instrumented:14-robolectric-10818077-i7

com.universalrp.cleansweep.widget.DisasterWidgetCacheTest :: homeMoveOrMasterOffHidesDetails
    java.lang.AssertionError: Failed to fetch maven artifact org.robolectric:android-all-instrumented:14-robolectric-10818077-i7

com.universalrp.cleansweep.widget.DisasterWidgetCacheTest :: manualViewStillRejectsChangedHome
    java.lang.AssertionError: Failed to fetch maven artifact org.robolectric:android-all-instrumented:14-robolectric-10818077-i7

com.universalrp.cleansweep.widget.DisasterWidgetCacheTest :: verifiedSummaryHasSourceTimeAndNonLocalQualification
    java.lang.AssertionError: Failed to fetch maven artifact org.robolectric:android-all-instrumented:14-robolectric-10818077-i7

com.universalrp.cleansweep.widget.DisasterWidgetCacheTest :: expiresAndRejectsFuture
    java.lang.AssertionError: Failed to fetch maven artifact org.robolectric:android-all-instrumented:14-robolectric-10818077-i7

com.universalrp.cleansweep.widget.DisasterWidgetCacheTest :: staleStatusRequestsRefresh
    java.lang.AssertionError: Failed to fetch maven artifact org.robolectric:android-all-instrumented:14-robolectric-10818077-i7

(181 failing test(s))

## Store artwork step
```
Setting up fonts-noto-mono (20201225-2build1) ...
Setting up libwmflite-0.2-7:amd64 (0.2.14-1) ...
Setting up libijs-0.35:amd64 (0.35-16) ...
Setting up libgs-common (10.06.0~dfsg-3ubuntu1.2) ...
Setting up libnetpbm11t64:amd64 (2:11.10.02-1build1) ...
Setting up poppler-data (0.4.12-1build1) ...
Setting up libjxr0t64:amd64 (1.2~git20170615.f752187-5.3build1) ...
Setting up libjbig2dec0:amd64 (0.20-1ubuntu0.26.04.1) ...
Setting up xfonts-encodings (1:1.0.5-0ubuntu3) ...
Setting up libopenexr-3-1-30:amd64 (3.1.13-2build1) ...
Setting up libidn12:amd64 (1.43-2ubuntu0.26.04.1) ...
Setting up netpbm (2:11.10.02-1build1) ...
Setting up imagemagick (8:7.1.2.18+dfsg1-1) ...
Setting up fonts-droid-fallback (1:8.1.0r7-1~1.gbp36536bbuild1) ...
Setting up libpaper2:amd64 (2.2.5-0.3maysync1) ...
Setting up libdjvulibre-text (3.5.29-1) ...
Setting up libjxr-tools (1.2~git20170615.f752187-5.3build1) ...
Setting up libdjvulibre21:amd64 (3.5.29-1) ...
Setting up libpaper-utils (2.2.5-0.3maysync1) ...
Setting up xfonts-utils (1:7.7+7build1) ...
Setting up libmagickcore-7.q16-10-extra:amd64 (8:7.1.2.18+dfsg1-1) ...
Setting up fonts-urw-base35 (20200910-8build1) ...
Setting up libgs10-common (10.06.0~dfsg-3ubuntu1.2) ...
Setting up libgs10:amd64 (10.06.0~dfsg-3ubuntu1.2) ...
Setting up ghostscript (10.06.0~dfsg-3ubuntu1.2) ...
Processing triggers for fontconfig (2.17.1-3ubuntu1) ...
Processing triggers for hicolor-icon-theme (0.18-2build1) ...
Processing triggers for libc-bin (2.43-2ubuntu2.7) ...
Processing triggers for man-db (2.13.1-1build1) ...
Not building database; man-db/auto-update is not 'true'.

Running kernel seems to be up-to-date.

Restarting services...

Service restarts being deferred:
 systemctl restart hosted-compute-agent.service

No containers need to be restarted.

No user sessions are running outdated binaries.

No VM guests are running outdated hypervisor (qemu) binaries on this host.
ImageMagick installed
fonts: title='DejaVu-Sans-Bold' body='DejaVu-Sans'
wrote cleaner/fastlane/metadata/android/en-US/images/featureGraphic.png
wrote cleaner/fastlane/metadata/android/ta-IN/images/featureGraphic.png
wrote radio/fastlane/metadata/android/en-US/images/featureGraphic.png
wrote radio/fastlane/metadata/android/ta-IN/images/featureGraphic.png
wrote equalizer/fastlane/metadata/android/en-US/images/featureGraphic.png
wrote tvguard/fastlane/metadata/android/en-US/images/featureGraphic.png
wrote tvguard/fastlane/metadata/android/ta-IN/images/featureGraphic.png

cleaner/fastlane/metadata/android/en-US/images/featureGraphic.png        1024x500
cleaner/fastlane/metadata/android/ta-IN/images/featureGraphic.png        1024x500
equalizer/fastlane/metadata/android/en-US/images/featureGraphic.png      1024x500
radio/fastlane/metadata/android/en-US/images/featureGraphic.png          1024x500
radio/fastlane/metadata/android/ta-IN/images/featureGraphic.png          1024x500
tvguard/fastlane/metadata/android/en-US/images/featureGraphic.png        1024x500
tvguard/fastlane/metadata/android/ta-IN/images/featureGraphic.png        1024x500
```
