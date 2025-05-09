plugins {
  id("intellij-plugin-module-conventions")
}

dependencies {
  intellijPlatform {
    intellijIde(baseVersion)
  }

  implementation(project(":intellij-plugin:educational-core"))
  api(rootProject.libs.educational.ml.library.core) {
    excludeKotlinDeps()
    excludeKotlinSerializationDeps()
    exclude(group = "net.java.dev.jna")
  }
  api(rootProject.libs.educational.ml.library.debugger) {
    excludeKotlinDeps()
    excludeKotlinSerializationDeps()
    exclude(group = "net.java.dev.jna")
  }

  testImplementation(project(":intellij-plugin:educational-core", "testOutput"))
}