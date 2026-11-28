# radio candidate compile failure
Source: 0b3da44567756d6a428d819a5c988f6ea1868b88
```
FAILURE: Build failed with an exception.

* What went wrong:
A problem occurred configuring root project 'RP-Ideas'.
> Could not resolve all artifacts for configuration 'classpath'.
   > Could not download kotlin-stdlib-jdk8-2.2.0.jar (org.jetbrains.kotlin:kotlin-stdlib-jdk8:2.2.0)
      > Could not get resource 'https://repo.maven.apache.org/maven2/org/jetbrains/kotlin/kotlin-stdlib-jdk8/2.2.0/kotlin-stdlib-jdk8-2.2.0.jar'.
         > Could not GET 'https://repo.maven.apache.org/maven2/org/jetbrains/kotlin/kotlin-stdlib-jdk8/2.2.0/kotlin-stdlib-jdk8-2.2.0.jar'. Received status code 429 from server: Too Many Requests
   > Could not download kotlin-reflect-2.0.21.jar (org.jetbrains.kotlin:kotlin-reflect:2.0.21)
      > Could not get resource 'https://repo.maven.apache.org/maven2/org/jetbrains/kotlin/kotlin-reflect/2.0.21/kotlin-reflect-2.0.21.jar'.
--
> Run with --info or --debug option to get more log output.
> Run with --scan to get full insights.
> Get more help at https://help.gradle.org.

* Exception is:
org.gradle.api.ProjectConfigurationException: A problem occurred configuring root project 'RP-Ideas'.
	at org.gradle.configuration.project.LifecycleProjectEvaluator.wrapException(LifecycleProjectEvaluator.java:84)
	at org.gradle.configuration.project.LifecycleProjectEvaluator.addConfigurationFailure(LifecycleProjectEvaluator.java:77)
--
BUILD FAILED in 1m 25s
```
