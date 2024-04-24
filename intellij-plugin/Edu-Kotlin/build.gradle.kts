plugins {
  id("intellij-plugin-module-conventions")
}

dependencies {
  intellijPlatform {
    val ideVersion = if (!isJvmCenteredIDE) ideaVersion else baseVersion

    intellijIde(ideVersion)

    intellijPlugins(jvmPlugins)
    intellijPlugins(kotlinPlugin)
  }
  implementation(project(":intellij-plugin:educational-core"))
  implementation(project(":intellij-plugin:jvm-core"))
  implementation(project(":intellij-plugin:Edu-Cognifire"))

  testImplementation(project(":intellij-plugin:educational-core", "testOutput"))
  testImplementation(project(":intellij-plugin:jvm-core", "testOutput"))
  testImplementation(project(":intellij-plugin:Edu-Cognifire", "testOutput"))
}