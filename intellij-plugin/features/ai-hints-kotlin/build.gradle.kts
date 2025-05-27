plugins {
  id("intellij-plugin-module-conventions")
}

dependencies {
  intellijPlatform {
    val ideVersion = if (!isJvmCenteredIDE) ideaVersion else baseVersion
    intellijIde(ideVersion)

    intellijPlugins(kotlinPlugin)
    testIntellijPlugins(jvmPlugins)
  }

  implementation(project(":intellij-plugin:educational-core"))
  implementation(project(":intellij-plugin:features:ai-hints-core"))

  testImplementation(project(":intellij-plugin:educational-core", "testOutput"))
  testImplementation(project(":intellij-plugin:jvm-core"))
  testImplementation(project(":intellij-plugin:jvm-core", "testOutput"))
  testImplementation(project(":intellij-plugin:features:ai-hints-core", "testOutput"))
  testImplementation(project(":intellij-plugin:Edu-Kotlin"))
}
