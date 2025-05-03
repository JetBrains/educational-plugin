plugins {
  id("intellij-plugin-module-conventions")
}

dependencies {
  intellijPlatform {
    intellijIde(baseVersion)

    intellijPlugins(shellScriptPlugin)
  }

  implementation(project(":intellij-plugin:educational-core"))

  testImplementation(project(":intellij-plugin:educational-core", "testOutput"))
}
