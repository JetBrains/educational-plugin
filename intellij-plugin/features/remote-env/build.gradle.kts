plugins {
  id("intellij-plugin-module-conventions")
}

dependencies {
  intellijPlatform {
    val ideVersion = if (isStudioIDE || isRiderIDE) ideaVersion else baseVersion
    intellijIde(ideVersion)

    intellijPlugins(codeWithMePlugin)
  }

  implementation(project(":intellij-plugin:educational-core"))

  testImplementation(project(":intellij-plugin:educational-core", "testOutput"))
}
