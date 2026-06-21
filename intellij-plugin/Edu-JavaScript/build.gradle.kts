plugins {
  id("intellij-plugin-module-conventions")
}

dependencies {
  intellijPlatform {
    intellijIde(ideaVersion)

    intellijPlugins(javaScriptPlugins)

    testIntellijPlugins(cssPlugin)
    if (isAtLeast262) {
      testIntellijPlugins(sshPlugin)
    }
  }

  implementation(project(":intellij-plugin:educational-core"))

  testImplementation(project(":intellij-plugin:educational-core", "testOutput"))
}
