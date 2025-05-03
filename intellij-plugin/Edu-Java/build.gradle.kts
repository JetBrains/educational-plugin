plugins {
  id("intellij-plugin-module-conventions")
}

dependencies {
  intellijPlatform {
    intellijIde(ideaVersion)

    intellijPlugins(jvmPlugins)
  }

  implementation(project(":intellij-plugin:educational-core"))
  implementation(project(":intellij-plugin:jvm-core"))

  testImplementation(project(":intellij-plugin:educational-core", "testOutput"))
  testImplementation(project(":intellij-plugin:jvm-core", "testOutput"))
}
