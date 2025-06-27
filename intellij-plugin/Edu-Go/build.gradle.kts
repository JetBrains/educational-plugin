plugins {
  id("intellij-plugin-module-conventions")
}

dependencies {
  intellijPlatform {
    intellijIde(ideaVersion)

    intellijPlugins(goPlugin, intelliLangPlugin)
    // Temporary workaround to make test work as expected
    // For some reason, the corresponding module is not loaded automatically
    if (isAtLeast252) {
      bundledModule("com.intellij.modules.ultimate")
    }
  }

  implementation(project(":intellij-plugin:educational-core"))

  testImplementation(project(":intellij-plugin:educational-core", "testOutput"))
}
