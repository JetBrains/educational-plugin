plugins {
  id("intellij-plugin-module-conventions")
}

dependencies {
  intellijPlatform {
    // TODO: use `baseVersion` when https://github.com/JetBrains/intellij-platform-gradle-plugin/issues/1790 is resolved
    intellijIde(ideaVersion)
  }

  implementation(project(":intellij-plugin:educational-core"))
  implementationWithoutKotlin(libs.clikt.core)

  testImplementation(project(":intellij-plugin:educational-core", "testOutput"))
}
