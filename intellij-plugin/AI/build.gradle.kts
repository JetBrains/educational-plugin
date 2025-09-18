plugins {
  id("intellij-plugin-module-conventions")
  alias(libs.plugins.kotlinSerializationPlugin)
}

dependencies {
  intellijPlatform {
    intellijIde(baseVersion)
  }

  implementation(project(":intellij-plugin:educational-core"))

  compileOnly(libs.kotlinx.serialization)

  testImplementation(project(":intellij-plugin:educational-core", "testOutput"))
}
