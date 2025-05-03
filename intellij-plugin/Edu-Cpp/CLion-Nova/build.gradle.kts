plugins {
  id("intellij-plugin-module-conventions")
}

dependencies {
  intellijPlatform {
    intellijIde(clionVersion)

    intellijPlugins(cppPlugins)
    intellijPlugins(radlerPlugin)
  }

  implementation(project(":intellij-plugin:educational-core"))
  implementation(project(":intellij-plugin:Edu-Cpp"))
  testImplementation(project(":intellij-plugin:educational-core", "testOutput"))
}
