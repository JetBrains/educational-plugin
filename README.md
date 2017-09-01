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


