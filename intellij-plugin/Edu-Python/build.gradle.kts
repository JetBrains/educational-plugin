plugins {
  id("intellij-plugin-module-conventions")
}

dependencies {
  intellijPlatform {
    // needed to load `org.toml.lang plugin` for Python plugin in tests
    val ideVersion = if (isRiderIDE) ideaVersion else baseVersion
    intellijIde(ideVersion)

    intellijPlugins(pythonPlugin)
    testIntellijPlugins(tomlPlugin)
  }

  implementation(project(":intellij-plugin:educational-core"))

  testImplementation(project(":intellij-plugin:educational-core", "testOutput"))
}
