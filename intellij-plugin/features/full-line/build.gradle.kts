plugins {
  id("intellij-plugin-module-conventions")
}

dependencies {
  intellijPlatform {
    intellijIde(baseVersion)

    bundledModules("intellij.fullLine.core.completion")
    bundledPlugin("org.jetbrains.completion.full.line")
  }

  implementation(project(":intellij-plugin:educational-core"))
  testImplementation(project(":intellij-plugin:educational-core", "testOutput"))
}
