# CI build failure excerpt

## Kotlin / resource errors
```
```

## Gradle summary
```
```
(no failing tests recorded in the JUnit XML)

## APK verification output
```
OK: canSearchWeb is in the CleanSweep APK
OK: setOnlineVoice is in the CleanSweep APK
OK: openScanner is in the CleanSweep APK
OK: CrashLog is in the CleanSweep APK
OK: StatusPillContent is in the CleanSweep APK
OK: showNetworkMeter is in the CleanSweep APK
OK: SecuritySettingsRoutes is in the CleanSweep APK
OK: FindingApp is in the CleanSweep APK
OK: SecurityAiPrompt is in the CleanSweep APK
OK: runSecurityAiAnalysis is in the CleanSweep APK
OK: powerConnected is in the CleanSweep APK
OK: AiRequestPolicy is in the CleanSweep APK
OK: RequestBudget is in the CleanSweep APK
OK: BoundedHttp is in the CleanSweep APK
OK: ProductEvidencePolicy is in the CleanSweep APK
OK: PermissionReviewStore is in the CleanSweep APK
OK: DisplayTime is in the CleanSweep APK
OK: AnswerVoicePreferences is in the CleanSweep APK
OK: FeatureSetupScreen is in the CleanSweep APK
OK: CorePrivacyMigrationPolicy is in the CleanSweep APK
OK: inSilentPlace is in the CleanSweep APK
OK: silentPlaceSlots is in the CleanSweep APK
OK: NO_INTERNET is in the CleanSweep APK
OK: SILENT_PLACE is in the CleanSweep APK
OK: SilentPlaceSlot is in the CleanSweep APK
OK: toggleSilentPlaceDay is in the CleanSweep APK
OK: VoiceNotifier is in the CleanSweep APK
OK: isoDayOfWeek is in the CleanSweep APK
OK: ALL_DAYS is in the CleanSweep APK
OK: SILENT_PLACE_DAY_LABELS is in the CleanSweep APK
OK: CorePrivacyMigration is in the CleanSweep APK
OK: LiveGuardUpdates is in the CleanSweep APK
OK: VerifiedApkDownload is in the CleanSweep APK
OK: UpdatePolicy is in the CleanSweep APK
OK: LastUnplugRepo is in the CleanSweep APK
OK: CompactWidgetPolicy is in the CleanSweep APK
OK: CleanSweepDataWidget is in the CleanSweep APK
OK: CleanSweepPhoneWidget is in the CleanSweep APK
OK: LocalReadGate is in the CleanSweep APK
OK: cancelAiReport is in the CleanSweep APK
OK: cancelDeviceIdentification is in the CleanSweep APK
OK: ChargingHonestyPolicy is in the CleanSweep APK
OK: ThermalWarningPolicy is in the CleanSweep APK
OK: AiOutputBudgetPolicy is in the CleanSweep APK
=== removed LM Arena rating feature gate (debug DEX) ===
OK: ArenaRatingPolicy is absent from the CleanSweep APK
OK: ArenaRatings is absent from the CleanSweep APK
OK: ArenaRatingCard is absent from the CleanSweep APK
=== v2.40 retired notification-reader gate (debug AND release) ===
OK: MessageReaderService is absent from both shipped APK variants
OK: LocalMessageTranslator is absent from both shipped APK variants
OK: NotificationReaderPrefs is absent from both shipped APK variants
OK: NotificationReaderScreen is absent from both shipped APK variants
OK: NotificationReadingPolicy is absent from both shipped APK variants
OK: NotificationReaderConfig is absent from both shipped APK variants
PASS: cleaner/build/intermediates/merged_manifests/debug/processDebugManifest/AndroidManifest.xml declares no notification-listener, SMS, Contacts, call-log, Accessibility or device-admin capability.
PASS: no retired reader class inside 2 compiled DEX file(s): cleaner-release.dex, cleaner.dex.
Source/static checks only: not Play Protect approval, not a device installation test and not Google review.
OK: merged CleanSweep manifest and both DEX blobs declare no retired capability
=== v2.41 honest-charging / heat-warning / AI-budget gate (debug AND release) ===
OK: batterySideWatts is in the CleanSweep DEBUG APK
OK: batterySideWatts is in the CleanSweep RELEASE APK
OK: screenNote is in the CleanSweep DEBUG APK
OK: screenNote is in the CleanSweep RELEASE APK
OK: shouldAnnounce is in the CleanSweep DEBUG APK
OK: shouldAnnounce is in the CleanSweep RELEASE APK
OK: effectiveOutputTokens is in the CleanSweep DEBUG APK
OK: effectiveOutputTokens is in the CleanSweep RELEASE APK
OK: checkModel is in the CleanSweep DEBUG APK
OK: checkModel is in the CleanSweep RELEASE APK
=== PulseEQ checks ===
FAIL: PulseEQ update checker requires INTERNET
17:    <uses-permission android:name="android.permission.MODIFY_AUDIO_SETTINGS" />
18:    <uses-permission android:name="android.permission.WAKE_LOCK" />
19:    <uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
20:    <uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
21:    <uses-permission android:name="android.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK" />
22:    <uses-permission android:name="android.permission.FOREGROUND_SERVICE_SPECIAL_USE" />
23:    <uses-permission android:name="android.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS" />
29:    <uses-permission android:name="com.universalrp.pulseeq.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION" />
```

## Store artwork step
```
wrote cleaner/fastlane/metadata/android/en-US/images/icon.png 7005 bytes
wrote cleaner/fastlane/metadata/android/ta-IN/images/icon.png 7005 bytes
wrote radio/fastlane/metadata/android/en-US/images/icon.png 4595 bytes
wrote radio/fastlane/metadata/android/ta-IN/images/icon.png 4595 bytes
wrote equalizer/fastlane/metadata/android/en-US/images/icon.png 4334 bytes
fonts: title='DejaVu-Sans-Bold' body='DejaVu-Sans'
wrote cleaner/fastlane/metadata/android/en-US/images/featureGraphic.png
wrote cleaner/fastlane/metadata/android/ta-IN/images/featureGraphic.png
wrote radio/fastlane/metadata/android/en-US/images/featureGraphic.png
wrote radio/fastlane/metadata/android/ta-IN/images/featureGraphic.png
wrote equalizer/fastlane/metadata/android/en-US/images/featureGraphic.png

cleaner/fastlane/metadata/android/en-US/images/featureGraphic.png        1024x500
cleaner/fastlane/metadata/android/ta-IN/images/featureGraphic.png        1024x500
equalizer/fastlane/metadata/android/en-US/images/featureGraphic.png      1024x500
radio/fastlane/metadata/android/en-US/images/featureGraphic.png          1024x500
radio/fastlane/metadata/android/ta-IN/images/featureGraphic.png          1024x500
```
