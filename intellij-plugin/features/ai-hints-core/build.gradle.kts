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
  api(libs.educational.ml.library.hints) {
    excludeKotlinDeps()
    excludeKotlinSerializationDeps()
    exclude(group = "net.java.dev.jna")
  }
  // For some reason, kotlin serialization plugin doesn't see the corresponding library from IDE dependency
  // and fails Kotlin compilation.
  // Let's provide necessary dependency during compilation to make it work
  compileOnly(libs.kotlinx.serialization) {
    excludeKotlinDeps()
  }

  testImplementation(project(":intellij-plugin:educational-core", "testOutput"))
}
