## Create Studio branch

After AS increases major IDE version (i.e. `2017.1` -> `2017.3`) we need to create a new separate branch for this Studio version.

Steps to do:
* Create a new branch from the corresponding branch for IDE (`173` -> `studio-173`)

* Modify both `intelij` blocks in `build.gradle`
    ```groovy
    intellij {
        if (project.hasProperty("androidStudioPath")) {
            localPath androidStudioPath
        } else {
            localPath downloadStudioIfNeededAndGetPath()
        }
        ...
    }
    ```

* Add plugin dependency on `smali` plugin everywhere in `build.gradle` where we have dependency on `gradle` plugin.
We need to do it because `gradle` plugin in AS has dependency on `smali` plugin
    
* Add `studioVersion` and `studioBuildVersion` into `gradle.properties`. 
Actual versions of `studioVersion` and `studioBuildVersion` you can get [here](https://developer.android.com/studio/index.html)

* Change `IJ` in plugin version with `Studio` in `gradle.properties`, i.e. `1.4-IJ-2017.3` -> `1.4-Studio-2017.3`

* Add dependency on `com.intellij.modules.androidstudio` module in `plugin.xml` 
to allow installing builds from this branch only in AS



