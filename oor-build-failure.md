# Oor Arivippu CI failure excerpt
```
	at org.gradle.internal.execution.steps.BroadcastChangingOutputsStep.execute(BroadcastChangingOutputsStep.java:26)
	at org.gradle.internal.execution.steps.CaptureOutputsAfterExecutionStep.execute(CaptureOutputsAfterExecutionStep.java:69)
	at org.gradle.internal.execution.steps.CaptureOutputsAfterExecutionStep.execute(CaptureOutputsAfterExecutionStep.java:46)
	at org.gradle.internal.execution.steps.ResolveInputChangesStep.execute(ResolveInputChangesStep.java:40)
	at org.gradle.internal.execution.steps.ResolveInputChangesStep.execute(ResolveInputChangesStep.java:29)
	at org.gradle.internal.execution.steps.BuildCacheStep.executeWithoutCache(BuildCacheStep.java:189)
	at org.gradle.internal.execution.steps.BuildCacheStep.executeAndStoreInCache(BuildCacheStep.java:145)
	at org.gradle.internal.execution.steps.BuildCacheStep.lambda$executeWithCache$4(BuildCacheStep.java:101)
	at org.gradle.internal.execution.steps.BuildCacheStep.lambda$executeWithCache$5(BuildCacheStep.java:101)
	at org.gradle.internal.Try$Success.map(Try.java:175)
	at org.gradle.internal.execution.steps.BuildCacheStep.executeWithCache(BuildCacheStep.java:85)
	at org.gradle.internal.execution.steps.BuildCacheStep.lambda$execute$0(BuildCacheStep.java:74)
	at org.gradle.internal.Either$Left.fold(Either.java:115)
	at org.gradle.internal.execution.caching.CachingState.fold(CachingState.java:62)
	at org.gradle.internal.execution.steps.BuildCacheStep.execute(BuildCacheStep.java:73)
	at org.gradle.internal.execution.steps.BuildCacheStep.execute(BuildCacheStep.java:48)
	at org.gradle.internal.execution.steps.StoreExecutionStateStep.execute(StoreExecutionStateStep.java:46)
	at org.gradle.internal.execution.steps.StoreExecutionStateStep.execute(StoreExecutionStateStep.java:35)
	at org.gradle.internal.execution.steps.SkipUpToDateStep.executeBecause(SkipUpToDateStep.java:75)
	at org.gradle.internal.execution.steps.SkipUpToDateStep.lambda$execute$2(SkipUpToDateStep.java:53)
	at org.gradle.internal.execution.steps.SkipUpToDateStep.execute(SkipUpToDateStep.java:53)
	at org.gradle.internal.execution.steps.SkipUpToDateStep.execute(SkipUpToDateStep.java:35)
	at org.gradle.internal.execution.steps.legacy.MarkSnapshottingInputsFinishedStep.execute(MarkSnapshottingInputsFinishedStep.java:37)
	at org.gradle.internal.execution.steps.legacy.MarkSnapshottingInputsFinishedStep.execute(MarkSnapshottingInputsFinishedStep.java:27)
	at org.gradle.internal.execution.steps.ResolveIncrementalCachingStateStep.executeDelegate(ResolveIncrementalCachingStateStep.java:49)
	at org.gradle.internal.execution.steps.ResolveIncrementalCachingStateStep.executeDelegate(ResolveIncrementalCachingStateStep.java:27)
	at org.gradle.internal.execution.steps.AbstractResolveCachingStateStep.execute(AbstractResolveCachingStateStep.java:71)
	at org.gradle.internal.execution.steps.AbstractResolveCachingStateStep.execute(AbstractResolveCachingStateStep.java:39)
	at org.gradle.internal.execution.steps.ResolveChangesStep.execute(ResolveChangesStep.java:65)
	at org.gradle.internal.execution.steps.ResolveChangesStep.execute(ResolveChangesStep.java:36)
	at org.gradle.internal.execution.steps.ValidateStep.execute(ValidateStep.java:107)
	at org.gradle.internal.execution.steps.ValidateStep.execute(ValidateStep.java:56)
	at org.gradle.internal.execution.steps.AbstractCaptureStateBeforeExecutionStep.execute(AbstractCaptureStateBeforeExecutionStep.java:64)
	at org.gradle.internal.execution.steps.AbstractCaptureStateBeforeExecutionStep.execute(AbstractCaptureStateBeforeExecutionStep.java:43)
	at org.gradle.internal.execution.steps.AbstractSkipEmptyWorkStep.executeWithNonEmptySources(AbstractSkipEmptyWorkStep.java:125)
	at org.gradle.internal.execution.steps.AbstractSkipEmptyWorkStep.execute(AbstractSkipEmptyWorkStep.java:61)
	at org.gradle.internal.execution.steps.AbstractSkipEmptyWorkStep.execute(AbstractSkipEmptyWorkStep.java:36)
	at org.gradle.internal.execution.steps.legacy.MarkSnapshottingInputsStartedStep.execute(MarkSnapshottingInputsStartedStep.java:38)
	at org.gradle.internal.execution.steps.LoadPreviousExecutionStateStep.execute(LoadPreviousExecutionStateStep.java:36)
	at org.gradle.internal.execution.steps.LoadPreviousExecutionStateStep.execute(LoadPreviousExecutionStateStep.java:23)
	at org.gradle.internal.execution.steps.HandleStaleOutputsStep.execute(HandleStaleOutputsStep.java:75)
	at org.gradle.internal.execution.steps.HandleStaleOutputsStep.execute(HandleStaleOutputsStep.java:41)
	at org.gradle.internal.execution.steps.AssignMutableWorkspaceStep.lambda$execute$0(AssignMutableWorkspaceStep.java:35)
	at org.gradle.api.internal.tasks.execution.TaskExecution$4.withWorkspace(TaskExecution.java:289)
	at org.gradle.internal.execution.steps.AssignMutableWorkspaceStep.execute(AssignMutableWorkspaceStep.java:31)
	at org.gradle.internal.execution.steps.AssignMutableWorkspaceStep.execute(AssignMutableWorkspaceStep.java:22)
	at org.gradle.internal.execution.steps.ChoosePipelineStep.execute(ChoosePipelineStep.java:40)
	at org.gradle.internal.execution.steps.ChoosePipelineStep.execute(ChoosePipelineStep.java:23)
	at org.gradle.internal.execution.steps.ExecuteWorkBuildOperationFiringStep.lambda$execute$2(ExecuteWorkBuildOperationFiringStep.java:67)
	at org.gradle.internal.execution.steps.ExecuteWorkBuildOperationFiringStep.execute(ExecuteWorkBuildOperationFiringStep.java:67)
	at org.gradle.internal.execution.steps.ExecuteWorkBuildOperationFiringStep.execute(ExecuteWorkBuildOperationFiringStep.java:39)
	at org.gradle.internal.execution.steps.IdentityCacheStep.execute(IdentityCacheStep.java:46)
	at org.gradle.internal.execution.steps.IdentityCacheStep.execute(IdentityCacheStep.java:34)
	at org.gradle.internal.execution.steps.IdentifyStep.execute(IdentifyStep.java:48)
	at org.gradle.internal.execution.steps.IdentifyStep.execute(IdentifyStep.java:35)
	at org.gradle.internal.execution.impl.DefaultExecutionEngine$1.execute(DefaultExecutionEngine.java:64)
	at org.gradle.api.internal.tasks.execution.ExecuteActionsTaskExecuter.executeIfValid(ExecuteActionsTaskExecuter.java:127)
	at org.gradle.api.internal.tasks.execution.ExecuteActionsTaskExecuter.execute(ExecuteActionsTaskExecuter.java:116)
	at org.gradle.api.internal.tasks.execution.ProblemsTaskPathTrackingTaskExecuter.execute(ProblemsTaskPathTrackingTaskExecuter.java:41)
	at org.gradle.api.internal.tasks.execution.FinalizePropertiesTaskExecuter.execute(FinalizePropertiesTaskExecuter.java:46)
	at org.gradle.api.internal.tasks.execution.ResolveTaskExecutionModeExecuter.execute(ResolveTaskExecutionModeExecuter.java:51)
	at org.gradle.api.internal.tasks.execution.SkipTaskWithNoActionsExecuter.execute(SkipTaskWithNoActionsExecuter.java:57)
	at org.gradle.api.internal.tasks.execution.SkipOnlyIfTaskExecuter.execute(SkipOnlyIfTaskExecuter.java:74)
	at org.gradle.api.internal.tasks.execution.CatchExceptionTaskExecuter.execute(CatchExceptionTaskExecuter.java:36)
	at org.gradle.api.internal.tasks.execution.EventFiringTaskExecuter$1.executeTask(EventFiringTaskExecuter.java:77)
	at org.gradle.api.internal.tasks.execution.EventFiringTaskExecuter$1.call(EventFiringTaskExecuter.java:55)
	at org.gradle.api.internal.tasks.execution.EventFiringTaskExecuter$1.call(EventFiringTaskExecuter.java:52)
	at org.gradle.internal.operations.DefaultBuildOperationRunner$CallableBuildOperationWorker.execute(DefaultBuildOperationRunner.java:210)
	at org.gradle.internal.operations.DefaultBuildOperationRunner$CallableBuildOperationWorker.execute(DefaultBuildOperationRunner.java:205)
	at org.gradle.internal.operations.DefaultBuildOperationRunner$2.execute(DefaultBuildOperationRunner.java:67)
	at org.gradle.internal.operations.DefaultBuildOperationRunner$2.execute(DefaultBuildOperationRunner.java:60)
	at org.gradle.internal.operations.DefaultBuildOperationRunner.execute(DefaultBuildOperationRunner.java:167)
	at org.gradle.internal.operations.DefaultBuildOperationRunner.execute(DefaultBuildOperationRunner.java:60)
	at org.gradle.internal.operations.DefaultBuildOperationRunner.call(DefaultBuildOperationRunner.java:54)
	at org.gradle.api.internal.tasks.execution.EventFiringTaskExecuter.execute(EventFiringTaskExecuter.java:52)
	at org.gradle.execution.plan.LocalTaskNodeExecutor.execute(LocalTaskNodeExecutor.java:42)
	at org.gradle.execution.taskgraph.DefaultTaskExecutionGraph$InvokeNodeExecutorsAction.execute(DefaultTaskExecutionGraph.java:331)
	at org.gradle.execution.taskgraph.DefaultTaskExecutionGraph$InvokeNodeExecutorsAction.execute(DefaultTaskExecutionGraph.java:318)
	at org.gradle.execution.taskgraph.DefaultTaskExecutionGraph$BuildOperationAwareExecutionAction.lambda$execute$0(DefaultTaskExecutionGraph.java:314)
	at org.gradle.internal.operations.CurrentBuildOperationRef.with(CurrentBuildOperationRef.java:85)
	at org.gradle.execution.taskgraph.DefaultTaskExecutionGraph$BuildOperationAwareExecutionAction.execute(DefaultTaskExecutionGraph.java:314)
	at org.gradle.execution.taskgraph.DefaultTaskExecutionGraph$BuildOperationAwareExecutionAction.execute(DefaultTaskExecutionGraph.java:303)
	at org.gradle.execution.plan.DefaultPlanExecutor$ExecutorWorker.execute(DefaultPlanExecutor.java:459)
	at org.gradle.execution.plan.DefaultPlanExecutor$ExecutorWorker.run(DefaultPlanExecutor.java:376)
	at org.gradle.internal.concurrent.ExecutorPolicy$CatchAndRecordFailures.onExecute(ExecutorPolicy.java:64)
	at org.gradle.internal.concurrent.AbstractManagedExecutor$1.run(AbstractManagedExecutor.java:48)


BUILD FAILED in 1m 52s
50 actionable tasks: 36 executed, 14 from cache
```
### oorarivippu/build/reports/tests/testReleaseUnitTest/classes/com.oorarivippu.voiceannouncer.AdaptiveLocationPollingPolicyTest.html
```
<!DOCTYPE html>
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=utf-8"/>
<meta http-equiv="x-ua-compatible" content="IE=edge"/>
<title>Test results - Class com.oorarivippu.voiceannouncer.AdaptiveLocationPollingPolicyTest</title>
<link href="../css/base-style.css" rel="stylesheet" type="text/css"/>
<link href="../css/style.css" rel="stylesheet" type="text/css"/>
<script src="../js/report.js" type="text/javascript"></script>
</head>
<body>
<div id="content">
<h1>Class com.oorarivippu.voiceannouncer.AdaptiveLocationPollingPolicyTest</h1>
<div class="breadcrumbs">
<a href="../index.html">all</a> &gt; 
<a href="../packages/com.oorarivippu.voiceannouncer.html">com.oorarivippu.voiceannouncer</a> &gt; AdaptiveLocationPollingPolicyTest</div>
<div id="summary">
<table>
<tr>
<td>
<div class="summaryGroup">
<table>
<tr>
<td>
<div class="infoBox" id="tests">
<div class="counter">3</div>
<p>tests</p>
</div>
</td>
<td>
<div class="infoBox" id="failures">
<div class="counter">0</div>
<p>failures</p>
</div>
</td>
<td>
<div class="infoBox" id="ignored">
<div class="counter">0</div>
<p>ignored</p>
</div>
</td>
<td>
<div class="infoBox" id="duration">
<div class="counter">0.003s</div>
<p>duration</p>
</div>
</td>
</tr>
</table>
</div>
</td>
<td>
<div class="infoBox success" id="successRate">
<div class="percent">100%</div>
<p>successful</p>
</div>
</td>
</tr>
</table>
</div>
<div class="tab-container">
<ul class="tabLinks">
<li>
<a href="#">Tests</a>
</li>
</ul>
<div class="tab">
<h2>Tests</h2>
<table>
<thead>
<tr>
<th>Test</th>
<th>Duration</th>
<th>Result</th>
</tr>
</thead>
<tr>
<td class="success">detectedMovementPreventsStationaryPolling</td>
<td class="success">0s</td>
<td class="success">passed</td>
</tr>
<tr>
<td class="success">selectsStationaryCityAndHighwayIntervals</td>
<td class="success">0.003s</td>
<td class="success">passed</td>
</tr>
<tr>
<td class="success">usesHysteresisAroundHighwayThreshold</td>
<td class="success">0s</td>
<td class="success">passed</td>
```
### oorarivippu/build/reports/tests/testReleaseUnitTest/classes/com.oorarivippu.voiceannouncer.BilingualStringsTest.html
```
<!DOCTYPE html>
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=utf-8"/>
<meta http-equiv="x-ua-compatible" content="IE=edge"/>
<title>Test results - Class com.oorarivippu.voiceannouncer.BilingualStringsTest</title>
<link href="../css/base-style.css" rel="stylesheet" type="text/css"/>
<link href="../css/style.css" rel="stylesheet" type="text/css"/>
<script src="../js/report.js" type="text/javascript"></script>
</head>
<body>
<div id="content">
<h1>Class com.oorarivippu.voiceannouncer.BilingualStringsTest</h1>
<div class="breadcrumbs">
<a href="../index.html">all</a> &gt; 
<a href="../packages/com.oorarivippu.voiceannouncer.html">com.oorarivippu.voiceannouncer</a> &gt; BilingualStringsTest</div>
<div id="summary">
<table>
<tr>
<td>
<div class="summaryGroup">
<table>
<tr>
<td>
<div class="infoBox" id="tests">
<div class="counter">9</div>
<p>tests</p>
</div>
</td>
<td>
<div class="infoBox" id="failures">
<div class="counter">0</div>
<p>failures</p>
</div>
</td>
<td>
<div class="infoBox" id="ignored">
<div class="counter">0</div>
<p>ignored</p>
</div>
</td>
<td>
<div class="infoBox" id="duration">
<div class="counter">0.133s</div>
<p>duration</p>
</div>
</td>
</tr>
</table>
</div>
</td>
<td>
<div class="infoBox success" id="successRate">
<div class="percent">100%</div>
<p>successful</p>
</div>
</td>
</tr>
</table>
</div>
<div class="tab-container">
<ul class="tabLinks">
<li>
<a href="#">Tests</a>
</li>
</ul>
<div class="tab">
<h2>Tests</h2>
<table>
<thead>
<tr>
<th>Test</th>
<th>Duration</th>
<th>Result</th>
</tr>
</thead>
<tr>
<td class="success">bothAppLanguagesExplainTtsAvailabilityAndFallbacks</td>
<td class="success">0.018s</td>
<td class="success">passed</td>
</tr>
<tr>
<td class="success">destinationAndDistrictAnnouncementsHaveLocalizedText</td>
<td class="success">0.033s</td>
<td class="success">passed</td>
</tr>
<tr>
<td class="success">districtWordIsSpokenOnceAndOneTownIsNotSpokenInTwoScripts</td>
<td class="success">0.009s</td>
<td class="success">passed</td>
```
### oorarivippu/build/reports/tests/testReleaseUnitTest/classes/com.oorarivippu.voiceannouncer.DestinationProximityPolicyTest.html
```
<!DOCTYPE html>
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=utf-8"/>
<meta http-equiv="x-ua-compatible" content="IE=edge"/>
<title>Test results - Class com.oorarivippu.voiceannouncer.DestinationProximityPolicyTest</title>
<link href="../css/base-style.css" rel="stylesheet" type="text/css"/>
<link href="../css/style.css" rel="stylesheet" type="text/css"/>
<script src="../js/report.js" type="text/javascript"></script>
</head>
<body>
<div id="content">
<h1>Class com.oorarivippu.voiceannouncer.DestinationProximityPolicyTest</h1>
<div class="breadcrumbs">
<a href="../index.html">all</a> &gt; 
<a href="../packages/com.oorarivippu.voiceannouncer.html">com.oorarivippu.voiceannouncer</a> &gt; DestinationProximityPolicyTest</div>
<div id="summary">
<table>
<tr>
<td>
<div class="summaryGroup">
<table>
<tr>
<td>
<div class="infoBox" id="tests">
<div class="counter">5</div>
<p>tests</p>
</div>
</td>
<td>
<div class="infoBox" id="failures">
<div class="counter">0</div>
<p>failures</p>
</div>
</td>
<td>
<div class="infoBox" id="ignored">
<div class="counter">0</div>
<p>ignored</p>
</div>
</td>
<td>
<div class="infoBox" id="duration">
<div class="counter">0.008s</div>
<p>duration</p>
</div>
</td>
</tr>
</table>
</div>
</td>
<td>
<div class="infoBox success" id="successRate">
<div class="percent">100%</div>
<p>successful</p>
</div>
</td>
</tr>
</table>
</div>
<div class="tab-container">
<ul class="tabLinks">
<li>
<a href="#">Tests</a>
</li>
</ul>
<div class="tab">
<h2>Tests</h2>
<table>
<thead>
<tr>
<th>Test</th>
<th>Duration</th>
<th>Result</th>
</tr>
</thead>
<tr>
<td class="success">arrivalIsOneTimeAndConsumesEarlierMilestones</td>
<td class="success">0.005s</td>
<td class="success">passed</td>
</tr>
<tr>
<td class="success">crossingFiveKilometersAfterTenKilometersEmitsNextMilestone</td>
<td class="success">0.001s</td>
<td class="success">passed</td>
</tr>
<tr>
<td class="success">crossingThresholdEmitsItOnlyOnce</td>
<td class="success">0.001s</td>
<td class="success">passed</td>
```
### oorarivippu/build/reports/tests/testReleaseUnitTest/classes/com.oorarivippu.voiceannouncer.DestinationSearchPolicyTest.html
```
<!DOCTYPE html>
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=utf-8"/>
<meta http-equiv="x-ua-compatible" content="IE=edge"/>
<title>Test results - Class com.oorarivippu.voiceannouncer.DestinationSearchPolicyTest</title>
<link href="../css/base-style.css" rel="stylesheet" type="text/css"/>
<link href="../css/style.css" rel="stylesheet" type="text/css"/>
<script src="../js/report.js" type="text/javascript"></script>
</head>
<body>
<div id="content">
<h1>Class com.oorarivippu.voiceannouncer.DestinationSearchPolicyTest</h1>
<div class="breadcrumbs">
<a href="../index.html">all</a> &gt; 
<a href="../packages/com.oorarivippu.voiceannouncer.html">com.oorarivippu.voiceannouncer</a> &gt; DestinationSearchPolicyTest</div>
<div id="summary">
<table>
<tr>
<td>
<div class="summaryGroup">
<table>
<tr>
<td>
<div class="infoBox" id="tests">
<div class="counter">3</div>
<p>tests</p>
</div>
</td>
<td>
<div class="infoBox" id="failures">
<div class="counter">0</div>
<p>failures</p>
</div>
</td>
<td>
<div class="infoBox" id="ignored">
<div class="counter">0</div>
<p>ignored</p>
</div>
</td>
<td>
<div class="infoBox" id="duration">
<div class="counter">0.011s</div>
<p>duration</p>
</div>
</td>
</tr>
</table>
</div>
</td>
<td>
<div class="infoBox success" id="successRate">
<div class="percent">100%</div>
<p>successful</p>
</div>
</td>
</tr>
</table>
</div>
<div class="tab-container">
<ul class="tabLinks">
<li>
<a href="#">Tests</a>
</li>
</ul>
<div class="tab">
<h2>Tests</h2>
<table>
<thead>
<tr>
<th>Test</th>
<th>Duration</th>
<th>Result</th>
</tr>
</thead>
<tr>
<td class="success">detailsShowTheDistrictStateAndPinCode</td>
<td class="success">0.004s</td>
<td class="success">passed</td>
</tr>
<tr>
<td class="success">repeatsAreDroppedButSameNamedPlacesInOtherDistrictsStay</td>
<td class="success">0.005s</td>
<td class="success">passed</td>
</tr>
<tr>
<td class="success">slashesBecomeCommasSoTheGeocoderReadsTheDistrict</td>
<td class="success">0.002s</td>
<td class="success">passed</td>
```
### oorarivippu/build/reports/tests/testReleaseUnitTest/classes/com.oorarivippu.voiceannouncer.OfflineReminderPolicyTest.html
```
<!DOCTYPE html>
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=utf-8"/>
<meta http-equiv="x-ua-compatible" content="IE=edge"/>
<title>Test results - Class com.oorarivippu.voiceannouncer.OfflineReminderPolicyTest</title>
<link href="../css/base-style.css" rel="stylesheet" type="text/css"/>
<link href="../css/style.css" rel="stylesheet" type="text/css"/>
<script src="../js/report.js" type="text/javascript"></script>
</head>
<body>
<div id="content">
<h1>Class com.oorarivippu.voiceannouncer.OfflineReminderPolicyTest</h1>
<div class="breadcrumbs">
<a href="../index.html">all</a> &gt; 
<a href="../packages/com.oorarivippu.voiceannouncer.html">com.oorarivippu.voiceannouncer</a> &gt; OfflineReminderPolicyTest</div>
<div id="summary">
<table>
<tr>
<td>
<div class="summaryGroup">
<table>
<tr>
<td>
<div class="infoBox" id="tests">
<div class="counter">4</div>
<p>tests</p>
</div>
</td>
<td>
<div class="infoBox" id="failures">
<div class="counter">0</div>
<p>failures</p>
</div>
</td>
<td>
<div class="infoBox" id="ignored">
<div class="counter">0</div>
<p>ignored</p>
</div>
</td>
<td>
<div class="infoBox" id="duration">
<div class="counter">0.003s</div>
<p>duration</p>
</div>
</td>
</tr>
</table>
</div>
</td>
<td>
<div class="infoBox success" id="successRate">
<div class="percent">100%</div>
<p>successful</p>
</div>
</td>
</tr>
</table>
</div>
<div class="tab-container">
<ul class="tabLinks">
<li>
<a href="#">Tests</a>
</li>
</ul>
<div class="tab">
<h2>Tests</h2>
<table>
<thead>
<tr>
<th>Test</th>
<th>Duration</th>
<th>Result</th>
</tr>
</thead>
<tr>
<td class="success">defaultsFollowTheOwnersRequestForOnePointOne</td>
<td class="success">0.002s</td>
<td class="success">passed</td>
</tr>
<tr>
<td class="success">onlySupportedIntervalsAreStored</td>
<td class="success">0s</td>
<td class="success">passed</td>
</tr>
<tr>
<td class="success">reminderRunsOnlyWhileTrackingWithKnownOfflineStateAndEnabledInterval</td>
<td class="success">0s</td>
<td class="success">passed</td>
```
### oorarivippu/build/reports/tests/testReleaseUnitTest/classes/com.oorarivippu.voiceannouncer.OorWeatherAlertTest.html
```
<!DOCTYPE html>
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=utf-8"/>
<meta http-equiv="x-ua-compatible" content="IE=edge"/>
<title>Test results - Class com.oorarivippu.voiceannouncer.OorWeatherAlertTest</title>
<link href="../css/base-style.css" rel="stylesheet" type="text/css"/>
<link href="../css/style.css" rel="stylesheet" type="text/css"/>
<script src="../js/report.js" type="text/javascript"></script>
</head>
<body>
<div id="content">
<h1>Class com.oorarivippu.voiceannouncer.OorWeatherAlertTest</h1>
<div class="breadcrumbs">
<a href="../index.html">all</a> &gt; 
<a href="../packages/com.oorarivippu.voiceannouncer.html">com.oorarivippu.voiceannouncer</a> &gt; OorWeatherAlertTest</div>
<div id="summary">
<table>
<tr>
<td>
<div class="summaryGroup">
<table>
<tr>
<td>
<div class="infoBox" id="tests">
<div class="counter">1</div>
<p>tests</p>
</div>
</td>
<td>
<div class="infoBox" id="failures">
<div class="counter">1</div>
<p>failures</p>
</div>
</td>
<td>
<div class="infoBox" id="ignored">
<div class="counter">0</div>
<p>ignored</p>
</div>
</td>
<td>
<div class="infoBox" id="duration">
<div class="counter">0.014s</div>
<p>duration</p>
</div>
</td>
</tr>
</table>
</div>
</td>
<td>
<div class="infoBox failures" id="successRate">
<div class="percent">0%</div>
<p>successful</p>
</div>
</td>
</tr>
</table>
</div>
<div class="tab-container">
<ul class="tabLinks">
<li>
<a href="#">Failed tests</a>
</li>
<li>
<a href="#">Tests</a>
</li>
</ul>
<div class="tab">
<h2>Failed tests</h2>
<div class="test">
<a name="smartWeatherRulesRespectQuietHoursForRainAndNeverSkipCyclone"></a>
<h3 class="failures">smartWeatherRulesRespectQuietHoursForRainAndNeverSkipCyclone</h3>
<span class="code">
<pre>java.lang.AssertionError
	at org.junit.Assert.fail(Assert.java:87)
	at org.junit.Assert.assertTrue(Assert.java:42)
	at org.junit.Assert.assertTrue(Assert.java:53)
	at com.oorarivippu.voiceannouncer.OorWeatherAlertTest.smartWeatherRulesRespectQuietHoursForRainAndNeverSkipCyclone(OorWeatherAlertTest.kt:22)
	at java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
	at java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
	at java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
	at java.base/java.lang.reflect.Method.invoke(Method.java:569)
	at org.junit.runners.model.FrameworkMethod$1.runReflectiveCall(FrameworkMethod.java:59)
	at org.junit.internal.runners.model.ReflectiveCallable.run(ReflectiveCallable.java:12)
	at org.junit.runners.model.FrameworkMethod.invokeExplosively(FrameworkMethod.java:56)
	at org.junit.internal.runners.statements.InvokeMethod.evaluate(InvokeMethod.java:17)
	at org.junit.runners.ParentRunner$3.evaluate(ParentRunner.java:306)
	at org.junit.runners.BlockJUnit4ClassRunner$1.evaluate(BlockJUnit4ClassRunner.java:100)
```
### oorarivippu/build/reports/tests/testReleaseUnitTest/classes/com.oorarivippu.voiceannouncer.PlaceDataPolicyTest.html
```
<!DOCTYPE html>
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=utf-8"/>
<meta http-equiv="x-ua-compatible" content="IE=edge"/>
<title>Test results - Class com.oorarivippu.voiceannouncer.PlaceDataPolicyTest</title>
<link href="../css/base-style.css" rel="stylesheet" type="text/css"/>
<link href="../css/style.css" rel="stylesheet" type="text/css"/>
<script src="../js/report.js" type="text/javascript"></script>
</head>
<body>
<div id="content">
<h1>Class com.oorarivippu.voiceannouncer.PlaceDataPolicyTest</h1>
<div class="breadcrumbs">
<a href="../index.html">all</a> &gt; 
<a href="../packages/com.oorarivippu.voiceannouncer.html">com.oorarivippu.voiceannouncer</a> &gt; PlaceDataPolicyTest</div>
<div id="summary">
<table>
<tr>
<td>
<div class="summaryGroup">
<table>
<tr>
<td>
<div class="infoBox" id="tests">
<div class="counter">14</div>
<p>tests</p>
</div>
</td>
<td>
<div class="infoBox" id="failures">
<div class="counter">0</div>
<p>failures</p>
</div>
</td>
<td>
<div class="infoBox" id="ignored">
<div class="counter">0</div>
<p>ignored</p>
</div>
</td>
<td>
<div class="infoBox" id="duration">
<div class="counter">0.055s</div>
<p>duration</p>
</div>
</td>
</tr>
</table>
</div>
</td>
<td>
<div class="infoBox success" id="successRate">
<div class="percent">100%</div>
<p>successful</p>
</div>
</td>
</tr>
</table>
</div>
<div class="tab-container">
<ul class="tabLinks">
<li>
<a href="#">Tests</a>
</li>
</ul>
<div class="tab">
<h2>Tests</h2>
<table>
<thead>
<tr>
<th>Test</th>
<th>Duration</th>
<th>Result</th>
</tr>
</thead>
<tr>
<td class="success">aTamilNameAndItsEnglishSpellingAreTheSamePlace</td>
<td class="success">0.018s</td>
<td class="success">passed</td>
</tr>
<tr>
<td class="success">aTownAnnouncedMinutesAgoIsNotRepeatedAtATownBorder</td>
<td class="success">0.015s</td>
<td class="success">passed</td>
</tr>
<tr>
<td class="success">detectsHighwayAndTollLandmarkNamesButIgnoresOrdinaryStreetNames</td>
<td class="success">0.003s</td>
<td class="success">passed</td>
```
### oorarivippu/build/reports/tests/testReleaseUnitTest/classes/com.oorarivippu.voiceannouncer.ReleaseUpdaterPolicyTest.html
```
<!DOCTYPE html>
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=utf-8"/>
<meta http-equiv="x-ua-compatible" content="IE=edge"/>
<title>Test results - Class com.oorarivippu.voiceannouncer.ReleaseUpdaterPolicyTest</title>
<link href="../css/base-style.css" rel="stylesheet" type="text/css"/>
<link href="../css/style.css" rel="stylesheet" type="text/css"/>
<script src="../js/report.js" type="text/javascript"></script>
</head>
<body>
<div id="content">
<h1>Class com.oorarivippu.voiceannouncer.ReleaseUpdaterPolicyTest</h1>
<div class="breadcrumbs">
<a href="../index.html">all</a> &gt; 
<a href="../packages/com.oorarivippu.voiceannouncer.html">com.oorarivippu.voiceannouncer</a> &gt; ReleaseUpdaterPolicyTest</div>
<div id="summary">
<table>
<tr>
<td>
<div class="summaryGroup">
<table>
<tr>
<td>
<div class="infoBox" id="tests">
<div class="counter">3</div>
<p>tests</p>
</div>
</td>
<td>
<div class="infoBox" id="failures">
<div class="counter">0</div>
<p>failures</p>
</div>
</td>
<td>
<div class="infoBox" id="ignored">
<div class="counter">0</div>
<p>ignored</p>
</div>
</td>
<td>
<div class="infoBox" id="duration">
<div class="counter">0.020s</div>
<p>duration</p>
</div>
</td>
</tr>
</table>
</div>
</td>
<td>
<div class="infoBox success" id="successRate">
<div class="percent">100%</div>
<p>successful</p>
</div>
</td>
</tr>
</table>
</div>
<div class="tab-container">
<ul class="tabLinks">
<li>
<a href="#">Tests</a>
</li>
</ul>
<div class="tab">
<h2>Tests</h2>
<table>
<thead>
<tr>
<th>Test</th>
<th>Duration</th>
<th>Result</th>
</tr>
</thead>
<tr>
<td class="success">bothOfficialRepositoriesAreUsedInDestinationFirstOrder</td>
<td class="success">0.002s</td>
<td class="success">passed</td>
</tr>
<tr>
<td class="success">canonicalOorReleaseAndAssetUrlsAreRequired</td>
<td class="success">0.012s</td>
<td class="success">passed</td>
</tr>
<tr>
<td class="success">versionComparisonUsesNumericStableVersionsAndRejectsTestNames</td>
<td class="success">0.006s</td>
<td class="success">passed</td>
```
### oorarivippu/build/reports/tests/testReleaseUnitTest/classes/com.oorarivippu.voiceannouncer.SpeechVolumePolicyTest.html
```
<!DOCTYPE html>
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=utf-8"/>
<meta http-equiv="x-ua-compatible" content="IE=edge"/>
<title>Test results - Class com.oorarivippu.voiceannouncer.SpeechVolumePolicyTest</title>
<link href="../css/base-style.css" rel="stylesheet" type="text/css"/>
<link href="../css/style.css" rel="stylesheet" type="text/css"/>
<script src="../js/report.js" type="text/javascript"></script>
</head>
<body>
<div id="content">
<h1>Class com.oorarivippu.voiceannouncer.SpeechVolumePolicyTest</h1>
<div class="breadcrumbs">
<a href="../index.html">all</a> &gt; 
<a href="../packages/com.oorarivippu.voiceannouncer.html">com.oorarivippu.voiceannouncer</a> &gt; SpeechVolumePolicyTest</div>
<div id="summary">
<table>
<tr>
<td>
<div class="summaryGroup">
<table>
<tr>
<td>
<div class="infoBox" id="tests">
<div class="counter">2</div>
<p>tests</p>
</div>
</td>
<td>
<div class="infoBox" id="failures">
<div class="counter">0</div>
<p>failures</p>
</div>
</td>
<td>
<div class="infoBox" id="ignored">
<div class="counter">0</div>
<p>ignored</p>
</div>
</td>
<td>
<div class="infoBox" id="duration">
<div class="counter">0.001s</div>
<p>duration</p>
</div>
</td>
</tr>
</table>
</div>
</td>
<td>
<div class="infoBox success" id="successRate">
<div class="percent">100%</div>
<p>successful</p>
</div>
</td>
</tr>
</table>
</div>
<div class="tab-container">
<ul class="tabLinks">
<li>
<a href="#">Tests</a>
</li>
</ul>
<div class="tab">
<h2>Tests</h2>
<table>
<thead>
<tr>
<th>Test</th>
<th>Duration</th>
<th>Result</th>
</tr>
</thead>
<tr>
<td class="success">appVolumeIsClampedToTheTtsParameterRange</td>
<td class="success">0s</td>
<td class="success">passed</td>
</tr>
<tr>
<td class="success">volumePercentMapsToAndroidTtsLinearVolume</td>
<td class="success">0.001s</td>
<td class="success">passed</td>
</tr>
</table>
</div>
</div>
<div id="footer">
```
### oorarivippu/build/reports/tests/testReleaseUnitTest/classes/com.oorarivippu.voiceannouncer.TtsVoicePolicyTest.html
```
<!DOCTYPE html>
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=utf-8"/>
<meta http-equiv="x-ua-compatible" content="IE=edge"/>
<title>Test results - Class com.oorarivippu.voiceannouncer.TtsVoicePolicyTest</title>
<link href="../css/base-style.css" rel="stylesheet" type="text/css"/>
<link href="../css/style.css" rel="stylesheet" type="text/css"/>
<script src="../js/report.js" type="text/javascript"></script>
</head>
<body>
<div id="content">
<h1>Class com.oorarivippu.voiceannouncer.TtsVoicePolicyTest</h1>
<div class="breadcrumbs">
<a href="../index.html">all</a> &gt; 
<a href="../packages/com.oorarivippu.voiceannouncer.html">com.oorarivippu.voiceannouncer</a> &gt; TtsVoicePolicyTest</div>
<div id="summary">
<table>
<tr>
<td>
<div class="summaryGroup">
<table>
<tr>
<td>
<div class="infoBox" id="tests">
<div class="counter">3</div>
<p>tests</p>
</div>
</td>
<td>
<div class="infoBox" id="failures">
<div class="counter">0</div>
<p>failures</p>
</div>
</td>
<td>
<div class="infoBox" id="ignored">
<div class="counter">0</div>
<p>ignored</p>
</div>
</td>
<td>
<div class="infoBox" id="duration">
<div class="counter">0.008s</div>
<p>duration</p>
</div>
</td>
</tr>
</table>
</div>
</td>
<td>
<div class="infoBox success" id="successRate">
<div class="percent">100%</div>
<p>successful</p>
</div>
</td>
</tr>
</table>
</div>
<div class="tab-container">
<ul class="tabLinks">
<li>
<a href="#">Tests</a>
</li>
</ul>
<div class="tab">
<h2>Tests</h2>
<table>
<thead>
<tr>
<th>Test</th>
<th>Duration</th>
<th>Result</th>
</tr>
</thead>
<tr>
<td class="success">keepsTheSelectedVoiceFirstAndDoesNotMixLanguages</td>
<td class="success">0.005s</td>
<td class="success">passed</td>
</tr>
<tr>
<td class="success">offersEveryVoiceOfTheLanguageBestQualityFirst</td>
<td class="success">0.001s</td>
<td class="success">passed</td>
</tr>
<tr>
<td class="success">qualityLabelsFollowAndroidRatings</td>
<td class="success">0.002s</td>
<td class="success">passed</td>
```
