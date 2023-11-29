## Fleet support

The module provides experimental support of JetBrains Academy functionality for [Fleet](https://www.jetbrains.com/fleet/).
The current implementation is in the prototype stage, and it doesn't have most of the necessary/expected features.
We use Fleet Gradle plugin to build Fleet plugin binary.
At this moment, it's not public so additional [setup](#setup) is required.


Note, this module is **not included** in the project by default to avoid unnecessary setup and downloading Fleet dependencies
for developers who don't develop this plugin.

### Setup
At it was mentioned above, `fleet-plugin` module is not considered as project module by Gradle by default.
At the same time, Fleet Gradle plugin is not public yet, and it's hosted in a private Space repository.

To include module in the project tree and make it work, you need:
- set `fleetIntegration` property in [gradle.properties](../gradle.properties) to `true`
- specify your Space username as `spaceUsername` property's value.
  You can find it by opening [this](https://jetbrains.team/m/me) link and taking url path segment after `https://jetbrains.team/m/`
- specify personal Space token as `spacePassword` property's value.
  The token should have `Read package repositories` permissions. 
  You can obtain it [here](https://jetbrains.team/m/me/authentication?tab=PermanentTokens)

### Run
Just run `./gradlew :fleet-plugin:runFleet` command from command line or run `runFleet` run configuration