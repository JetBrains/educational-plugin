[![official JetBrains project](http://jb.gg/badges/official-flat-square.svg)](https://confluence.jetbrains.com/display/ALL/JetBrains+on+GitHub)

[![Build Status](https://travis-ci.org/JetBrains/educational-plugin.svg?branch=master)](https://travis-ci.org/JetBrains/educational-plugin)
## Educational Plugin

This plugin allows to learn and teach programming languages and technologies 
directly inside JetBrains IDEs based on IntelliJ Platform.

This plugin is bundled into [PyCharm Edu IDE](https://www.jetbrains.com/pycharm-edu/).

Building and taking courses is currently supported for the following languages: [Python](https://www.python.org/), [Kotlin](https://kotlinlang.org/), [Java](https://www.java.com).

The best place to learn what this plugin is about is to check out PyCharm Edu [documentation](https://www.jetbrains.com/pycharm-edu/learners/#easy-start).

### Issue tracker
Please submit your issues to [Educational Plugin YouTrack](https://youtrack.jetbrains.com/issues/EDU)

### Resources
* Sources of PyCharm Edu IDE are stored in [IDEA community repository](https://github.com/JetBrains/intellij-community/tree/master/python/educational-python)
* This plugins comes with integration with [Stepik](http://welcome.stepik.org/) learning platform
* You can learn more about our Kotlin support from [this](https://blog.jetbrains.com/kotlin/2016/03/kotlin-educational-plugin/) blog post
* You can read more about plugin development in [IntelliJ Sdk Docs](http://www.jetbrains.org/intellij/sdk/docs/index.html)

### Configuring development environment

1. Clone sources of this project to some folder
2. Open this folder in IntelliJ Idea
3. Import gradle project. If you are not familiar with IntelliJ IDEA gradle integration, check out [documentation](https://www.jetbrains.com/help/idea/gradle.html)
4. You can modify gradle.properties if needed
5. If you use openjdk, invoke prepareJavaFx gradle task first
6. For running and debugging plugin with IDEA or PyCharm predefined run configurations *runIdea* and *runPyCharm* 
should be used
7. To build plugin distributions use *buildPlugin* gradle task

### Supporting different platforms

We support some latest releases of platform (for example, 2018.2 and 2018.3) and the latest EAP.
Also, it's important to check API compatibility in Android Studio.
So we have to build plugin with different versions of IDEA/Android Studio/other IDE.
The plugin project has separate settings for each platform version (in general, IDE and plugin versions) 
to avoid manual changes when you want to compiler project with non default platform version.
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
     |   +-- branches/studio-182/src
     |       +-- com/jetbrains/edu/android
     |           +-- AndroidVersionsInfo.kt
     |   +-- branches/studio-183/src
     |       +-- com/jetbrains/edu/android
     |           +-- AndroidVersionsInfo.kt     
     |   +-- src
     |       +-- other platfrom independent code
     
Of course, only one batch of platform dependent code will be used in compilation.
To change platform version which you use during compilation, 
i.e. change IDEA/Android Studio/plugins dependencies and platform dependent code,
you need to modify `environmentName` property in `gradle.properties` file or 
pass `-PenvironmentName=%platform.name%` argument to gradle command  

### Workflow for checking compatibility with different Idea/Studio branches

1. Develop feature in feature branch %feature.branch%
2. Commit changes
3. Change `environmentName` value in `gradle.properties` with `%platform.name%` where %platform.name% is desired platform. 
4. Run tests to ensure that everything works

Everything works:
5. Push all the changes

Something went wrong (some API changes):
5. Ensure that you can't use old API instead of new one for all platforms. If you can, just modify your current implementation
6. If there isn't common API for all platforms, extract problem pieces of code into separate files as new functions or classes
7. Create implementations for each platform and place them into `branches/%platform.name%/src` folders of the corresponding module
8. Commit new files
9. Push all the changes
