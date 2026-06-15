plugins {
  id("intellij-plugin-module-conventions")
}

tasks {
  test {
    setClionSystemProperties(project)
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

// At the moment, the tests for the Edu-Cpp module don't work correctly.
// This started after migrating the tests from the classic engine to the CLion Nova engine.
// The first test to run fails (`CppMoveHandlerTest` and a few other tests consistently fail).
// We should consider using the test base classes from the CLion test framework.

// TODO(re-enable the tests once they are fixed as part of EDU-8917)
tasks.withType<Test> {
  enabled = false
}
