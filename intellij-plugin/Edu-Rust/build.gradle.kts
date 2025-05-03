plugins {
  id("intellij-plugin-module-conventions")
}

dependencies {
  intellijPlatform {
    val ideVersion = if (!isIdeaIDE && !isClionIDE) ideaVersion else baseVersion
    intellijIde(ideVersion)

    intellijPlugins(rustPlugins)
  }

  implementation(project(":intellij-plugin:educational-core"))

  testImplementation(project(":intellij-plugin:educational-core", "testOutput"))
}


// ATM all tests fail because they can't find test implementation of `CargoProjectsService`.
// It was moved to test sources recently in the Rust plugin project, but plugin manifest declares separate test impl of the service.
// As a result, tests fail.
//
// Enable test again when the corresponding problem is fixed
tasks.withType<Test> {
  enabled = false
}
