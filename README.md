[![official JetBrains project](http://jb.gg/badges/official-flat-square.svg)](https://confluence.jetbrains.com/display/ALL/JetBrains+on+GitHub)

## Educational Plugin

This plugin allows to learn and teach programming languages and technologies 
directly inside JetBrains IDEs based on IntelliJ Platform.

This plugin is bundled into [PyCharm Edu IDE](https://www.jetbrains.com/education/download/#section=pycharm-edu) and [IntelliJ IDEA Edu](https://www.jetbrains.com/education/download/#section=idea).

Building and taking courses is currently supported for the following languages: 
 * [Python](https://www.python.org/)
 * [Kotlin](https://kotlinlang.org/)
 * [Java](https://www.java.com)
 * [Scala](https://www.scala-lang.org/)
 * [Rust](https://www.rust-lang.org/)
 * [JavaScript](https://developer.mozilla.org/en-US/docs/Web/JavaScript)
 * [C++](https://isocpp.org/)
 * [Go](https://golang.org/)

The best place to learn what this plugin is about is to check out the EduTools plugin [documentation](https://www.jetbrains.com/help/education/educational-products.html).

### Issue tracker
Please submit your issues to [Educational Plugin YouTrack](https://youtrack.jetbrains.com/issues/EDU)

### Resources
* Sources of PyCharm Edu and IntelliJ IDEA Edu are stored in [IntelliJ IDEA repository](https://jetbrains.team/p/idea/code/intellij?path=%2Fedu)
* This plugin comes with integration with [Stepik](http://welcome.stepik.org/) learning platform
* You can read more about plugin development in [IntelliJ Platform SDK docs](http://www.jetbrains.org/intellij/sdk/docs/index.html)

### Configuring development environment

1. Clone sources of this project to some folder
2. Open this folder in IntelliJ IDEA
3. Import Gradle project. If you are not familiar with IntelliJ IDEA Gradle integration, check out the [documentation](https://www.jetbrains.com/help/idea/gradle.html)
4. You can modify gradle.properties if needed
5. If you use OpenJDK, invoke *prepareJavaFx* Gradle task first
6. For running and debugging plugin with IntelliJ IDEA, PyCharm, CLion, Android Studio, WebStorm, and GoLand predefined run configurations *runIdea*, *runPyCharm*, *runCLion*, *runStudio*, *runWebStorm*, and *runGoLand* 
should be used
7. To build plugin distributions use *buildPlugin* Gradle task
