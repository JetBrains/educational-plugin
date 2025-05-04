plugins {
  id("intellij-plugin-module-conventions")
}

dependencies {
  intellijPlatform {
    intellijIde(baseVersion)
  }

  implementation(project(":intellij-plugin:educational-core"))
  implementationWithoutKotlin(rootProject.libs.twitter4j.core)
  implementationWithoutKotlin(rootProject.libs.twitter4j.v2)

  testImplementation(project(":intellij-plugin:educational-core", "testOutput"))
}
