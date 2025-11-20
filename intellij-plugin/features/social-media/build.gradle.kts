plugins {
  id("intellij-plugin-module-conventions")
}

dependencies {
  intellijPlatform {
    intellijIde(baseVersion)
  }

  implementation(project(":intellij-plugin:educational-core"))

  testImplementation(project(":intellij-plugin:educational-core", "testOutput"))
}

tasks {
  processTestResources {
    // Minor hack to have the corresponding files at the same place in tests as in the production builds
    from("../../socialMedia") {
      into("socialMedia")
    }
  }
}
