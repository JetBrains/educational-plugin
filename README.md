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

### Workflow for checking compatibility with different Idea/Studio branches
1. Develop feature in feature branch %feature.branch%
2. Commit changes
3. Run `apply -3 --ignore-space-change --ignore-whitespace patcher/%branch.name%.patch` command where %branch.name% is desired branch.
4. Use appropriate run configuration
Different configurations use different *.properties file. This can be changed manually using `-PenvironmentName=%branch.name%` argument

Everything works:\
5. Revert patch `git reset --hard && git clean -fd`\
6. Push all the changes

Something went wrong:\
5. Fix errors caused by compatibility issues in idea branches\
6. Create new patch file `git diff %feature.branch% > patcher/%branch.name%.patch`\
7. Commit new patch file\
8. Revert `git reset --hard && git clean -fd`\
9. Push all the changes


