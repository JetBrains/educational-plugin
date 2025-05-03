plugins {
  id("intellij-plugin-module-conventions")
}

intellijPlatform {
  // Set custom plugin directory name for tests.
  // Otherwise, `prepareTestSandbox` merges directories of `markdown` plugin and `markdown` modules
  // into single one
  projectName = "edu-markdown"
}

dependencies {
  intellijPlatform {
    intellijIde(baseVersion)

    intellijPlugins(markdownPlugin)
  }

  implementation(project(":intellij-plugin:educational-core"))
  implementation(project(":intellij-plugin:code-insight"))

  testImplementation(project(":intellij-plugin:educational-core", "testOutput"))
  testImplementation(project(":intellij-plugin:code-insight", "testOutput"))
}
