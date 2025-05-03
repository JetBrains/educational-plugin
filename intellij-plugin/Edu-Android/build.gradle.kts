plugins {
  id("intellij-plugin-module-conventions")
}

dependencies {
  intellijPlatform {
    intellijIde(studioVersion)

    intellijPlugins(jvmPlugins)
    intellijPlugins(androidPlugin)
    testIntellijPlugins(kotlinPlugin)
  }

  implementation(project(":intellij-plugin:educational-core"))
  implementation(project(":intellij-plugin:jvm-core"))

  testImplementation(project(":intellij-plugin:educational-core", "testOutput"))
  testImplementation(project(":intellij-plugin:jvm-core", "testOutput"))
}


// BACKCOMPAT: enable when 251 studio is available
tasks.withType<Test> {
  enabled = environmentName.toInt() < 251
}
