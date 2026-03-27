plugins {
  id("intellij-plugin-module-conventions")
}

dependencies {
  intellijPlatform {
    intellijIde(baseVersion)

    bundledModules("intellij.fullLine.core.completion")
    bundledPlugin(fullinePlugin)
  }

  implementation(project(":intellij-plugin:educational-core"))
  testImplementation(project(":intellij-plugin:educational-core", "testOutput"))
  testImplementation(libs.kotlinx.coroutine.test)
}
