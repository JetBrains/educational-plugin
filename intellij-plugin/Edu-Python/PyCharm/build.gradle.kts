plugins {
  id("intellij-plugin-module-conventions")
}

dependencies {
  intellijPlatform {
    intellijIde(baseVersion)

    intellijPlugins(pythonPlugin)
  }

  implementation(project(":intellij-plugin:educational-core"))
  compileOnly(project(":intellij-plugin:Edu-Python"))
  testImplementation(project(":intellij-plugin:educational-core", "testOutput"))
}
