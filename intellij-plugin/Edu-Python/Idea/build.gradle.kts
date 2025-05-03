plugins {
  id("intellij-plugin-module-conventions")
}

dependencies {
  intellijPlatform {
    val ideVersion = if (!isJvmCenteredIDE) ideaVersion else baseVersion
    intellijIde(ideVersion)

    val pluginList = listOf(
      if (!isJvmCenteredIDE) pythonCommunityPlugin else pythonPlugin,
      javaPlugin
    )
    intellijPlugins(pluginList)
  }

  implementation(project(":intellij-plugin:educational-core"))
  compileOnly(project(":intellij-plugin:Edu-Python"))
  testImplementation(project(":intellij-plugin:educational-core", "testOutput"))
}
