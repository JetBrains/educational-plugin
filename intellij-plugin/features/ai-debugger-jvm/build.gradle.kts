plugins {
  id("intellij-plugin-module-conventions")
}

dependencies {
  intellijPlatform {
    intellijIde(ideaVersion)

    intellijPlugins(jvmPlugins)
  }

  implementation(project(":intellij-plugin:educational-core"))
  implementation(project(":intellij-plugin:features:ai-debugger-core"))

  testImplementation(project(":intellij-plugin:educational-core", "testOutput"))
  testImplementation(project(":intellij-plugin:features:ai-debugger-core", "testOutput"))
}
