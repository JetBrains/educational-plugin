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
You can get the latest IntelliJ IDEA [here](https://www.jetbrains.com/idea/download/).
3. Import Gradle project. If you are not familiar with IntelliJ IDEA Gradle integration, check out the [documentation](https://www.jetbrains.com/help/idea/gradle.html)
4. You can modify `gradle.properties` if needed
5. For running and debugging plugin with IntelliJ IDEA, PyCharm, CLion, WebStorm, GoLand, and Rider predefined run configurations *runIdea*, *runPyCharm*, *runCLion*, *runWebStorm*, *runGoLand*, and *runRider* 
should be used
6. To build plugin distribution use *:intellij-plugin:buildPlugin* Gradle task. 
It creates an archive at `intellij-plugin/build/distributions` which can be installed into your IDE via `Install plugin from disk...` action found in `Settings > Plugins`.

# Supporting different platforms

The plugin supports several versions of the IntelliJ platform and several different IDEs.
See [different-platform-versions.md](/documentation/different-platform-versions.md) for details on how multi-platform support
is organized in the project, how to write platform-specific code, and how to support new platform versions or drop old ones.
