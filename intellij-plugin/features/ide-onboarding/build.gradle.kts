plugins {
  id("intellij-plugin-module-conventions")
}

dependencies {
  intellijPlatform {
    intellijIde(baseVersion)
  }

  implementation(project(":intellij-plugin:educational-core"))

  testImplementation(libs.kotlinx.coroutine.test)
  testImplementation(project(":intellij-plugin:educational-core", "testOutput"))
}
