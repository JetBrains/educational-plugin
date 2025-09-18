plugins {
  id("intellij-plugin-module-conventions")
}

dependencies {
  intellijPlatform {
    intellijIde(baseVersion)
  }

  api(libs.educational.ml.library.core) {
    excludeKotlinSerializationDeps()
    exclude(group = "net.java.dev.jna")
  }
  api(libs.educational.ml.library.test.generation) {
    excludeKotlinSerializationDeps()
    exclude(group = "net.java.dev.jna")
  }

  implementation(project(":intellij-plugin:educational-core"))
  implementation(project(":intellij-plugin:AI"))

  testImplementation(project(":intellij-plugin:educational-core", "testOutput"))
}
