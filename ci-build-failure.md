# CI build failure excerpt

## Kotlin / resource errors
```
```

## Gradle summary
```
[Incubating] Problems report is available at: file:///home/runner/work/RP-Android-Apps/RP-Android-Apps/build/reports/problems/problems-report.html

FAILURE: Build failed with an exception.

* What went wrong:
Execution failed for task ':expensetracker:testDebugUnitTest'.
> There were failing tests. See the report at: file:///home/runner/work/RP-Android-Apps/RP-Android-Apps/expensetracker/build/reports/tests/testDebugUnitTest/index.html

* Try:
> Run with --scan to get full insights.

Deprecated Gradle features were used in this build, making it incompatible with Gradle 9.0.

You can use '--warning-mode all' to show the individual deprecation warnings and determine if they come from your own scripts or plugins.

For more on this, please refer to https://docs.gradle.org/8.14.3/userguide/command_line_interface.html#sec:command_line_warnings in the Gradle documentation.

BUILD FAILED in 2m 8s
275 actionable tasks: 140 executed, 135 from cache
```
com.universalrp.expensetracker.MonthlyLedgerTest :: aSelfTransferIsExcludedFromBothSidesAndReported
    java.lang.AssertionError: expected:<500.0> but was:<20500.0>

com.universalrp.expensetracker.MonthlyLedgerTest :: spendAndIncomeAreCountedSeparately
    java.lang.AssertionError: expected:<-4300.0> but was:<4300.0>

com.universalrp.expensetracker.TransactionStoreTest :: aSelfTransferIsNeverStored
    java.lang.AssertionError: expected null, but was:<Transaction(messageId=1, amountInr=20000.0, direction=DEBIT, category=FOOD, counterparty=SWIGGY, accountKey=, institutionEn=, timestampMs=1700000000000)>

com.universalrp.expensetracker.TransactionStoreTest :: rebuildingFromAParseBatchKeepsOnlyWhatCounts
    java.lang.AssertionError: expected:<[60.0, 250.0]> but was:<[60.0, 250.0, 20000.0]>

(4 failing test(s))

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
