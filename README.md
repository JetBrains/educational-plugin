## Educational plugin for Kotlin programming language

### Build and run instructions

1.  Clone this project and configure plugin development environment as described [here](http://www.jetbrains.org/intellij/sdk/docs/basics/getting_started/setting_up_environment.html)
2.  Add the following plugins' jars to classpath of your Intellij Plugin Sdk:
  * [Educational plugin core (student)](https://plugins.jetbrains.com/plugin/7988?pr=pycharm)
  * [Educational plugin core (course-creator)] (https://plugins.jetbrains.com/plugin/8020?pr=pycharm)
  * [Kotlin plugin] (https://plugins.jetbrains.com/plugin/6954?pr=pycharm) (kotlin-runtime.jar and kotlin-plugin.jar)
  * Junit (idea_home/plugins/junit/lib/idea-junit.jar)
3. Add sources of IntelliJ IDEA to your plugin sdk source roots.
4. Install Educational plugin core on your Intellij IDEA instance that launches with run configuration.

### Issue tracker
Please submit your issues to [Educational Plugin YouTrack](https://youtrack.jetbrains.com/issues/EDU)
