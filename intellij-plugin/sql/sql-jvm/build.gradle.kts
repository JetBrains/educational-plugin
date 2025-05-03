plugins {
  id("intellij-plugin-module-conventions")
}

dependencies {
  intellijPlatform {
    intellijIde(ideaVersion)

    intellijPlugins(jvmPlugins)
    intellijPlugins(sqlPlugins)
  }

  api(project(":intellij-plugin:sql"))
  api(project(":intellij-plugin:jvm-core"))

  testImplementation(project(":intellij-plugin:educational-core", "testOutput"))
  testImplementation(project(":intellij-plugin:sql", "testOutput"))
  testImplementation(project(":intellij-plugin:jvm-core", "testOutput"))
}
