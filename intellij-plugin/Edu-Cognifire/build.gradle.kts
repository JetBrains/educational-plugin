plugins {
  id("intellij-plugin-module-conventions")
}

dependencies {
  intellijPlatform {
    intellijIde(baseVersion)

    intellijPlugins(jvmPlugins)
    intellijPlugins(kotlinPlugin)
  }

  implementation(project(":intellij-plugin:educational-core"))
  implementation(rootProject.libs.freemarker)
  api(rootProject.libs.educational.ml.library.core) {
    excludeKotlinDeps()
    excludeKotlinSerializationDeps()
    exclude(group = "net.java.dev.jna")
  }
  api(rootProject.libs.educational.ml.library.cognifire) {
    excludeKotlinDeps()
    excludeKotlinSerializationDeps()
    exclude(group = "net.java.dev.jna")
  }

  testImplementation(project(":intellij-plugin:educational-core", "testOutput"))
}