# equalizer candidate compile failure
Source: adfb6a62cd96f83f724cd293c9e79809fe6a449d
```
> Configure project :kurippu
w: file:///home/runner/work/RP-Android-Apps/RP-Android-Apps/kurippu/app/build.gradle.kts:19:21: 'jvmTarget: String' is deprecated. Please migrate to the compilerOptions DSL. More details are here: https://kotl.in/u1r8ln

> Configure project :tamilanstore
w: file:///home/runner/work/RP-Android-Apps/RP-Android-Apps/tamilanstore/build.gradle.kts:51:21: 'jvmTarget: String' is deprecated. Please migrate to the compilerOptions DSL. More details are here: https://kotl.in/u1r8ln

> Task :equalizer:checkKotlinGradlePluginConfigurationErrors SKIPPED
> Task :equalizer:preBuild UP-TO-DATE
> Task :equalizer:preDebugBuild UP-TO-DATE
> Task :equalizer:checkDebugAarMetadata
> Task :equalizer:processDebugNavigationResources
> Task :equalizer:compileDebugNavigationResources FROM-CACHE
> Task :equalizer:generateDebugResValues FROM-CACHE
> Task :equalizer:mapDebugSourceSetPaths
> Task :equalizer:generateDebugResources
> Task :equalizer:packageDebugResources
> Task :equalizer:mergeDebugResources
> Task :equalizer:createDebugCompatibleScreenManifests
> Task :equalizer:extractDeepLinksDebug
> Task :equalizer:parseDebugLocalResources
> Task :equalizer:processDebugMainManifest
> Task :equalizer:processDebugManifest
> Task :equalizer:javaPreCompileDebug FROM-CACHE
> Task :equalizer:preDebugUnitTestBuild UP-TO-DATE
> Task :equalizer:javaPreCompileDebugUnitTest FROM-CACHE
> Task :equalizer:preReleaseBuild UP-TO-DATE
> Task :equalizer:mergeReleaseJniLibFolders
> Task :equalizer:mergeReleaseNativeLibs FAILED
> Task :equalizer:processDebugManifestForPackage
gradle/actions: Writing build results to /home/runner/work/_temp/.gradle-actions/build-results/compile-1795706641393.json

FAILURE: Build failed with an exception.

* What went wrong:
Execution failed for task ':equalizer:mergeReleaseNativeLibs'.
> Could not resolve all files for configuration ':equalizer:releaseRuntimeClasspath'.
   > Failed to transform listenablefuture-1.0.jar (com.google.guava:listenablefuture:1.0) to match attributes {artifactType=android-jni, org.gradle.category=library, org.gradle.libraryelements=jar, org.gradle.status=release, org.gradle.usage=java-runtime}.
      > Could not download listenablefuture-1.0.jar (com.google.guava:listenablefuture:1.0)
         > Could not get resource 'https://repo.maven.apache.org/maven2/com/google/guava/listenablefuture/1.0/listenablefuture-1.0.jar'.
            > Could not GET 'https://repo.maven.apache.org/maven2/com/google/guava/listenablefuture/1.0/listenablefuture-1.0.jar'. Received status code 429 from server: Too Many Requests

--
> Run with --info or --debug option to get more log output.
> Run with --scan to get full insights.
> Get more help at https://help.gradle.org.

* Exception is:
org.gradle.api.tasks.TaskExecutionException: Execution failed for task ':equalizer:mergeReleaseNativeLibs'.
	at org.gradle.api.internal.tasks.execution.CatchExceptionTaskExecuter.execute(CatchExceptionTaskExecuter.java:38)
	at org.gradle.api.internal.tasks.execution.EventFiringTaskExecuter$1.executeTask(EventFiringTaskExecuter.java:77)
--
BUILD FAILED in 25s
18 actionable tasks: 14 executed, 4 from cache
```
