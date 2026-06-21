plugins {
  id("intellij-plugin-module-conventions")
}

dependencies {
  intellijPlatform {
    intellijIde(baseVersion)

    intellijPlugins(fullinePlugin)
    bundledModules("intellij.fullLine.core.completion")
  }

  implementation(project(":intellij-plugin:educational-core"))
  testImplementation(project(":intellij-plugin:educational-core", "testOutput"))
  testImplementation(libs.kotlinx.coroutine.test)
}
