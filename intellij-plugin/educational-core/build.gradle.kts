plugins {
  id("intellij-plugin-module-conventions")
  alias(libs.plugins.kotlinSerializationPlugin)
}

dependencies {
  intellijPlatform {
    intellijIde(baseVersion)

    bundledModules("intellij.platform.vcs.impl")
  }

  api(project(":edu-format"))
  api(libs.edu.ai.format) {
    excludeKotlinDeps()
  }
  // For some reason, kotlin serialization plugin doesn't see the corresponding library from IDE dependency
  // and fails Kotlin compilation.
  // Let's provide necessary dependency during compilation to make it work
  compileOnly(libs.kotlinx.serialization) {
    excludeKotlinDeps()
  }
}
