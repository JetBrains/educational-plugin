plugins {
  id("intellij-plugin-module-conventions")
}

tasks {
  test {
    setClionSystemProperties(project, withRadler = false)
  }
}

dependencies {
  intellijPlatform {
    intellijIde(clionVersion)

    intellijPlugins(cppPlugins)
  }

  implementation(project(":intellij-plugin:educational-core"))

  testImplementation(project(":intellij-plugin:educational-core", "testOutput"))
}
