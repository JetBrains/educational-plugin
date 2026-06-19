plugins {
  id("intellij-plugin-module-conventions")
}

dependencies {
  intellijPlatform {
    intellijIde(ideaVersion)

    intellijPlugins(jvmPlugins)
    intellijPlugins(sqlPlugins)
    bundledModule("com.intellij.modules.ultimate")
    if (isAtLeast262) {
      testIntellijPlugins("com.intellij.moduleSet.grid.core")
      testIntellijPlugins("com.intellij.moduleSet.servicesView")
      testIntellijPlugins("intellij.navbar.plugin")
    }
  }

  api(project(":intellij-plugin:sql"))
  api(project(":intellij-plugin:jvm-core"))

  testImplementation(project(":intellij-plugin:educational-core", "testOutput"))
  testImplementation(project(":intellij-plugin:sql", "testOutput"))
  testImplementation(project(":intellij-plugin:jvm-core", "testOutput"))
}
