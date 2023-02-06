## Supporting New IntelliJ Versions

The JetBrains Academy plugin supports several versions of the IntelliJ platform.
When the new IntelliJ version comes out, we need to prepare plugin's build with the new version and
handle all the incompatible changes in API.
To reduce the number of supported IntelliJ versions, we also drop versions support from time to time.
The following guide describes how to support new IntelliJ versions and drop old ones.

The following instruction uses old, current and new terms:
* `old` - the oldest supported platform version that should be dropped
* `current` - number (or numbers, depends on context) of the latest major stable platform release(s) that are supported by the plugin
* `new` - new platform version that should be supported

For example, at the moment of writing, `193` is `old`, `201` and `202` are `current`, and `203` is `new`.
See [build_number_ranges](https://jetbrains.org/intellij/sdk/docs/basics/getting_started/build_number_ranges.html) for more info about platform versions.
 
### Step by step instruction on how to support new platform version

* Drop all code related to the `old` platform version, i.e. drop `gradle-%old%.properties` and
all `%module_name%/branches/%old%` directories in each module
* Move common code from all current platform-specific `%module_name%/branches/%current%` directories into common source set,
i.e. from `%module_name%/branches/%current%` into `%module_name%`.
Also, simplify the moved code if possible.
`Compare With` action may help you to determine identical files in platform-specific directories.  
* Add support for `new` platform.
Common steps how to do it:
    - add `gradle-%new%.properties` with all necessary properties
    - copy all `%module_name%/branches/%current%` directories for latest supported platform as `%module_name%/branches/%new%`
    - make it compile
  
  The last step may require extracting some code into platform-specific source sets to make plugin compile with each supported platform.
See [Tips and tricks](../CONTRIBUTING.md#tips-and-tricks) section for the most common examples of how to do it
* Fix tests. If you don't know how to fix some tests (especially if you are not a maintainer of the corresponding subsystem),
just [create](https://youtrack.jetbrains.com/newIssue?project=EDU&c=Priority%20Major&c=Subsystem%20Infrastructure) an issue in YouTrack
* Update TeamCity configuration to provide new subproject instead of the old one.
At the moment of writing, it requires changing `EducationalSupportedReleases` in [`intellij-teamcity-config`](https://jetbrains.team/p/ij/code/intellij-teamcity-config) repo
* Fix `BACKCOMPAT: %old%` comments. You can find all such comments via `Find in Path` action.
Also, you can add `\bbackcompat\b.*` pattern into `Preferences | Editor | TODO` setting panel to show such comments in [`TODO`](https://www.jetbrains.com/help/idea/todo-tool-window.html) tool window
* Delete all Android Studio binaries for `old` platform from `edu-tools` repo at `https://repo.labs.intellij.net`.
`delete_studio.py` script in `helpers` directory may hep you. Just run `python delete_studio.py --apiKey %your_key% --version %old%`.
Note, `https://repo.labs.intellij.net` is only available with VPN
