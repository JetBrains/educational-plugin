plugins {
  id("intellij-plugin-module-conventions")
}

dependencies {
  intellijPlatform {
    intellijIde(riderVersion)
    intellijPlugins(csharpPlugins)

    bundledModule("intellij.rider")
  }

  implementation(project(":intellij-plugin:educational-core"))
  testImplementation(project(":intellij-plugin:educational-core", "testOutput"))
}
