plugins {
  id("intellij-plugin-module-conventions")
  alias(libs.plugins.kotlinSerializationPlugin)
}

dependencies {
  intellijPlatform {
    intellijIde(baseVersion)
  }

  implementation(project(":intellij-plugin:educational-core"))
  implementation(project(":intellij-plugin:AI"))
  api(libs.educational.ml.library.core) {
    excludeKotlinDeps()
    excludeKotlinSerializationDeps()
    exclude(group = "net.java.dev.jna")
  }
  api(libs.educational.ml.library.debugger) {
    excludeKotlinDeps()
    excludeKotlinSerializationDeps()
    exclude(group = "net.java.dev.jna")
  }

  compileOnly(libs.kotlinx.serialization) {
    excludeKotlinDeps()
  }

  testImplementation(project(":intellij-plugin:educational-core", "testOutput"))
}