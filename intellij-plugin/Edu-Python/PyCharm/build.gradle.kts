plugins {
  id("intellij-plugin-module-conventions")
}

dependencies {
  intellijPlatform {
    val ideVersion = if (isStudioIDE) ideaVersion else baseVersion
    intellijIde(ideVersion)

    // TODO: incorrect plugin version in case of AS
    intellijPlugins(pythonPlugin)
  }

  implementation(project(":intellij-plugin:educational-core"))
  compileOnly(project(":intellij-plugin:Edu-Python"))
  testImplementation(project(":intellij-plugin:educational-core", "testOutput"))
}
