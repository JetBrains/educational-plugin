plugins {
  id("intellij-plugin-module-conventions")
}

dependencies {
  intellijPlatform {
    val ideVersion = if (isPycharmIDE) ideaVersion else baseVersion
    intellijIde(ideVersion)

    intellijPlugins(sqlPlugins)
  }

  api(project(":intellij-plugin:educational-core"))

  testImplementation(project(":intellij-plugin:educational-core", "testOutput"))
}
