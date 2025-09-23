plugins {
  id("intellij-plugin-module-conventions")
}

dependencies {
  intellijPlatform {
    intellijIde(ideaVersion)

    intellijPlugins(goPlugin)
    if (!isAtLeast253) {
      intellijPlugins(intelliLangPlugin)
    }

    bundledModule("com.intellij.modules.ultimate")
  }

  implementation(project(":intellij-plugin:educational-core"))

  testImplementation(project(":intellij-plugin:educational-core", "testOutput"))
}
