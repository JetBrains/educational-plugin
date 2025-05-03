plugins {
  id("intellij-plugin-module-conventions")
}

dependencies {
  intellijPlatform {
    intellijIde(baseVersion)
  }

  implementation(project(":intellij-plugin:educational-core"))
  implementation(project(":intellij-plugin:code-insight"))

  testImplementation(project(":intellij-plugin:educational-core", "testOutput"))
  testImplementation(project(":intellij-plugin:code-insight", "testOutput"))
}
