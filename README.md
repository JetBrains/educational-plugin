## Educational plugin for Kotlin programming language

### Configuring development environment

1. Clone this project and configure plugin development environment as described [here](http://www.jetbrains.org/intellij/sdk/docs/basics/getting_started/setting_up_environment.html)
2. Add the following plugins' jars to classpath of your Intellij Plugin Sdk:
  * **Kotlin plugin** (<idea_home>/plugins/Kotlin/kotlin-plugin.jar)
  * **Junit** (<idea_home>/plugins/junit/lib/idea-junit.jar)
3. Clone sources of IntelliJ from [github](https://github.com/JetBrains/intellij-community/)
4. Add sources of IntelliJ IDEA to your plugin sdk source roots.
5. Edit gradle.properties file with the following content:

     `intellijCommunity` Path to your IntelliJ sources (downloaded on step 3)

      `ideaPath` Path to your IJ community instance (it is advisable to use the same IntelliJ Idea instance as your IntelliJ Plugin Sdk)


   Examples:
      
```groovy
intellijCommunity = /home/user/progs/intellijCommunity
ideaPath = /home/user/.local/share/JetBrains/Toolbox/apps/IDEA-U/ch-2/171.1796
```


```groovy
intellijCommunity = /home/user/progs/intellijCommunity
deaPath=/Users/liana/Library/Application Support/JetBrains/Toolbox/apps/IDEA-U/ch-1/171.1495/IntelliJ IDEA 2017.1 EAP.app/Contents
```

6. Run `deployPlugins` gradle task. You can create specific gradle run configuration or just run it from terminal:
```bash
./gradlew deployPlugins --refresh-dependencies
```
7. Choose created *idea-sandbox* directory as Sandbox Home for your selected IntelliJ Plugin Sdk (It can be done via File/Project Structure/Sdks)
8. Add *educational-core.jar* from created directory *plugins* to classpath of your your selected IntelliJ Plugin Sdk (It can be done via File/Project Structure/Sdks)

### Notes
*You can read more about plugin development in [IntelliJ Sdk Docs](http://www.jetbrains.org/intellij/sdk/docs/index.html)*

You can run `deployPlugins` task every time you need to get the latest versions of educational-core.jar. 
Appropriate branch should be selected in intellij-community repository.

### Issue tracker
Please submit your issues to [Educational Plugin YouTrack](https://youtrack.jetbrains.com/issues/EDU)
