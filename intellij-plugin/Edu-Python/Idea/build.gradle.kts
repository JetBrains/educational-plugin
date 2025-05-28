plugins {
  id("intellij-plugin-module-conventions")
}

dependencies {
  intellijPlatform {
    intellijIde(ideaVersion)

    val pluginList = listOf(
      pythonCommunityPlugin,
      javaPlugin
    )
    intellijPlugins(pluginList)
  }

  implementation(project(":intellij-plugin:educational-core"))
  compileOnly(project(":intellij-plugin:Edu-Python"))
  testImplementation(project(":intellij-plugin:educational-core", "testOutput"))
}
