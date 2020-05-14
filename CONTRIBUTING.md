# Getting started

## Clone

```
git clone https://github.com/JetBrains/educational-plugin
cd educational-plugin
```

## Configuring development environment

1. Java 11 is required for development.
For example, you can install [openJDK](https://openjdk.java.net/install/) or [Amazon Corretto](https://docs.aws.amazon.com/corretto/latest/corretto-11-ug/downloads-list.html)
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
These settings are stored in `gradle-%platform.name%.properties` files.

Sometimes there are not compatible changes in new platform version.
To avoid creating several parallel vcs branches for each version, we have separate
folders for each version to keep platform dependent code.

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
     
Of course, only one batch of platform dependent code will be used in compilation.
To change platform version which you use during compilation, 
i.e. change IDEA/Android Studio/plugins dependencies and platform dependent code,
you need to modify `environmentName` property in `gradle.properties` file or 
pass `-PenvironmentName=%platform.name%` argument to gradle command

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
