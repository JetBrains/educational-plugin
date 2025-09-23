# Getting started

## Clone

```
git clone https://github.com/JetBrains/educational-plugin
cd educational-plugin
```

## Configuring development environment

1. Java 21 is required for development.
For example, you can install [openJDK](https://openjdk.java.net/install/) or [Amazon Corretto](https://docs.aws.amazon.com/corretto/latest/corretto-17-ug/downloads-list.html)
2. Open project directory in IntelliJ IDEA.
You can get the latest IntelliJ IDEA Community Edition [here](https://www.jetbrains.com/idea/download/).
3. Import Gradle project. If you are not familiar with IntelliJ IDEA Gradle integration, check out the [documentation](https://www.jetbrains.com/help/idea/gradle.html)
4. You can modify `gradle.properties` if needed
5. For running and debugging plugin with IntelliJ IDEA, PyCharm, CLion, Android Studio, WebStorm, and GoLand predefined run configurations *runIdea*, *runPyCharm*, *runCLion*, *runStudio*, *runWebStorm*, and *runGoLand* 
should be used
6. To build plugin distribution use *:buildPlugin* Gradle task. 
It creates an archive at `build/distributions` which can be installed into your IDE via `Install plugin from disk...` action found in `Settings > Plugins`.

# Supporting different platforms

### Different versions

We support some latest releases of platform (for example, 2018.2 and 2018.3) and the latest EAP.
To be sure the plugin works with all releases, we have to build plugin with different versions of IDE.
The plugin project has separate settings for each platform version (in general, IDE and plugin versions) 
to avoid manual changes when you want to compile project with non default platform version.
These settings are stored in `gradle-%platform.version%.properties` files.

Sometimes there are not compatible changes in new platform version.
To avoid creating several parallel vcs branches for each version, we have separate
directories for each version to keep platform dependent code.

For example, `com.jetbrains.edu.android.AndroidVersionsInfoKt#loadTargetVersions` function in `Edu-Android` module
should have separate implementations for 182 and 183 platforms.
Then source code structure of the corresponding module will be

     +-- Edu-Android
     |   +-- branches/182/src
     |       +-- com/jetbrains/edu/android
     |           +-- AndroidVersionsInfo.kt
     |   +-- branches/183/src
     |       +-- com/jetbrains/edu/android
     |           +-- AndroidVersionsInfo.kt
     |   +-- src
     |       +-- other platfrom independent code
     
Of course, only one batch of platform-specific code will be used in compilation.
To change platform version which you use during compilation, 
i.e. change IDEA/Android Studio/plugins dependencies and platform dependent code,
you need to modify `environmentName` property in `gradle.properties` file or 
pass `-PenvironmentName=%platform.version%` argument to gradle command.

See [Tips and tricks](#tips-and-tricks) section to get more details how to create platform-specific code.
See [SupportNewPlatformVersion.md](/documentation/SupportNewPlatformVersion.md) for more details if you need to support a new platform version.

### Different IDEs

The plugin is available in several IDEs like IDEA, Android Studio, PyCharm, CLion, WebStorm, etc.
Generally, they provide common core API that we can be safely used in the plugin. But they also provide some specific API
available only in particular IDE (for example, `java-api` and `java-impl` modules in IDEA).
To prevent accidental usage of specific API in common code, we have to build plugin/run with different IDEs.
At the moment, the plugin tests can be launched with IDEA, Android Studio, CLion and PyCharm.
You can change this IDE via `baseIDE` property in `gradle.properties` file.

Note, not all modules can be compiled with any IDE. For example, `Edu-Android` module is supposed to be built
only with Android Studio. 
Such restrictions are already taken into account in the configuration of the corresponding modules
in `build.gradle.kts` and when you choose base IDE for build unsupported in the particular module, 
it will be compiled with some default IDE.

### Tips and tricks

Please, always remember a general advice - if you have to extract a code into platform-specific source sets, try to minimize it.
It will simplify maintenance of this code in the future.

A non-exhaustive list of tips how you can adapt your code for several platforms:
* to execute specific code for certain platform in gradle build scripts (`build.gradle.kts` or `settings.gradle.kts`),
just use `environmentName` property and `if`/`when` conditions
* if you need to have different sources for each platform:
    - check that you actually need to have specific code for each platform.
    There is quite often a deprecated way to make necessary action.
    If it's your case, don't forget to suppress the deprecation warning and add `// BACKCOMPAT: %platform.version%` comment to mark this code and
    fix the deprecation in the future.
    `// BACKCOMPAT: %platform.version%` means that the corresponding code should be fixed when we will stop support for `%platform.version%`
    - extract different code into a function and place it into `compatibilityUtils.kt` file in each platform-specific source set.
    It usually works well when you need to call specific public code to make the same things in each platform
    - if code that you need to call is not public (for example, it uses some protected methods of parent class), use the inheritance mechanism.
    Extract `AwesomeClassBase` from your `AwesomeClass`, inherit `AwesomeClass` from `AwesomeClassBase`,
    move `AwesomeClassBase` into platform-specific source sets and move all platform-specific code into `AwesomeClassBase` as protected methods.
    - sometimes, signatures of some methods might have changed during platform evolution.
    For example, `protected abstract void foo(Bar<Baz> bar)` can be converted into `protected abstract void foo(Bar<? extends Baz> bar)` since `%platform.version%`
    and you have to override this method in your implementation.
    It introduces source incompatibility (although it doesn't break binary compatibility).
    The simplest way to make it compile for each platform is to introduce platform-specific [`typealias`](https://kotlinlang.org/docs/reference/type-aliases.html),
    i.e. `typealias PlaformBar = Bar<Baz>` for platforms before `%platform.version%` and 
    `typealias PlaformBar = Bar<out Baz>` for `%platform.version%` and newer, and use it in signature of overridden method.
    Also, this approach works well when some class you depend on was moved into another package. 
    - if the creation of a platform-specific source set is too heavy for your case, there is a way how you can choose specific code in runtime.
    Just create the corresponding condition using `com.intellij.openapi.application.ApplicationInfo.getBuild`.
    Usually, it looks like
    ```kotlin
    val BUILD_%platform.version% = BuildNumber.fromString("%platform.version%")!!
    if (ApplicationInfo.getInstance().build < BUILD_%platform.version%) {
        // code for current platform(s) older than %platform.version%
    } 
    else {
        // code for %platform.version% platform and newer
    }
    ```
    Of course, code should be compilable with all supported platforms to use this approach.
    This approach can be used to temporarily disable some tests to find out why they don't work later.
* if you need to register different items in `%xml_name%.xml` for each platform:
    1. create `platform-%xml_name%.xml` in all `%module_name%/branch/%platform.version%/resources/META-INF` directories
    2. put platform specific definitions into these xml files
    3. include platform specific xml into `%module_name%/resources/META-INF/%xml_name%.xml`, i.e. add the following code
    ```xml
    <idea-plugin xmlns:xi="http://www.w3.org/2001/XInclude">
        <xi:include href="/META-INF/platform-%xml_name%.xml" xpointer="xpointer(/idea-plugin/*)"/>
    </idea-plugin>  
    ```


## Workflow for checking compatibility with different platform branches

1. Develop feature in feature branch `%feature.branch%`
2. Commit changes
3. Change `environmentName` value in `gradle.properties` with `%platform.name%` where `%platform.name%` is desired platform. 
4. Run tests to ensure that everything works

Everything works:
5. Push all the changes

Something went wrong (some API changes):
5. Ensure that you can't use old API instead of new one for all platforms. If you can, just modify your current implementation.
Note, if you use deprecated API, don't forget to suppress deprecation warning and 
add `// BACKCOMPAT` comment (see more in [Supporting a new platform version](#Supporting a new platform version) section)
6. If there isn't common API for all platforms, extract problem pieces of code into separate files as new functions or classes
7. Create implementations for each platform and place them into `branches/%platform.name%/src` folders of the corresponding module
8. Commit new files
9. Push all the changes

## Supporting a new platform version

1. Create new `gradle-%new.platform.name%.properties` with the corresponding version of all necessary plugins
2. Copy `%module.name%/branches/%latest.platform.name%` folder as `%module.name%/branches/%new.platform.name%`
for each existing module
3. Check the project can be compiled with a new platform. If compilation fails, fix all new incompatibilities
(see [Supporting different platforms](#Supporting different platforms) section for more details)
4. Fix all new deprecation warnings. There are two different cases:
  * Deprecated piece of code can be replaced with some other equivalent code for all supported platforms.
  In this case, just replace deprecated code
  * If there isn't equivalent non-deprecated code for all platforms, 
  add suppress annotation (or comment if suppress annotation cannot be added because of syntax error).
  Also add a comment `// BACKOMPAT: %platform.name%` where `%platform.name` is the latest platform version where deprecated code has to be used
  
## Dropping old platform version

1. Remove `gradle-%old.platform.name%.properties` file
2. Remove `%module.name%/branches/%old.platform.name%` folders for each module
3. If `%module.name%/branches` contains the same file for each supporting platform version,
drop `%module.name%/branches/%platform.name%/%path.to.file%` for each platform and move it into `%module.name%/%path.to.file%`.
Make refactoring if needed
4. Find all `// BACKOMPAT: %old.platform.name%` comments and replace deprecated code with non-deprecated analog
