import org.jetbrains.intellij.platform.gradle.TestFrameworkType

plugins {
  id("intellij-plugin-module-conventions")
}

dependencies {
  intellijPlatform {
    intellijIde(baseVersion)

    testFramework(TestFrameworkType.Starter)
  }

  testImplementation(project(":validation-results"))
  testImplementation(libs.kotlinx.serialization)
  testImplementation(libs.junit.jupiter)
  testRuntimeOnly(libs.junit.platform.launcher)
}

tasks {
  test {
    useJUnitPlatform()
    // ide-starter ships its JUnit Jupiter lifecycle extensions (test-method tracking, StarterBus IDE-error
    // capture, process cleanup) via ServiceLoader (META-INF/services), so they activate only when JUnit
    // extension auto-detection is enabled.
    systemProperty("junit.jupiter.extensions.autodetection.enabled", "true")
    // The Starter framework installs the plugin into an externally launched IDE.
    // Build the distribution zip first and point the test at the archive file.
    dependsOn(":intellij-plugin:buildPlugin")
    val buildPluginTask = project(":intellij-plugin").tasks.named<Zip>("buildPlugin").get()
    systemProperty(
      "path.to.build.plugin",
      buildPluginTask.archiveFile.get().asFile.absolutePath
    )
  }
}
