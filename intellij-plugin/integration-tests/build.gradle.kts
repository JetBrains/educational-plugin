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
  // JUnit version must match the one ide-starter depends on for the current platform
  testImplementation(platformLibs.junit.jupiter)
  testRuntimeOnly(platformLibs.junit.platform.launcher)
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
    // Integration tests run in the same IDE versions as the ones the plugin is built against for the current platform.
    // Properties are keyed by the product code (like `IU`), which matches `IdeInfo.productCode` of IntelliJ Starter
    for (versionWithCode in listOf(ideaVersion, pycharmVersion, clionVersion, riderVersion)) {
      val ideVersion = parseStarterIdeVersion(versionWithCode)
      systemProperty("edu.integration.tests.${ideVersion.productCode}.version", ideVersion.version)
      systemProperty("edu.integration.tests.${ideVersion.productCode}.build.type", ideVersion.buildType)
    }
  }
}

private data class StarterIdeVersion(val productCode: String, val version: String, val buildType: String)

/**
 * Parses IDE version from gradle properties (like `IU-2026.3`) into product code, version and build type for IntelliJ Starter.
 *
 * Release versions are passed as is.
 * For EAP versions, only the major version of the current platform is passed,
 * so the tests use the latest EAP build of this major version instead of the exact build from the properties.
 */
private fun Project.parseStarterIdeVersion(versionWithCode: String): StarterIdeVersion {
  // The optional `release` group is matched only if the whole rest of the string is a release version
  // like `2026.3` or `2026.3.0.1`. `$` inside the group prevents matching `2026.3` in `2026.3-EAP8-SNAPSHOT`.
  // Any other version (like `263.7307-EAP-CANDIDATE-SNAPSHOT`) is considered an EAP one.
  // `matchAt` matches from the beginning of the string but doesn't require matching it entirely
  val match = """(?<code>[A-Z]+)-(?<release>\d{4}(?:\.\d+)+$)?""".toRegex().matchAt(versionWithCode, 0)
    ?: error("Unexpected IDE version format: `$versionWithCode`")
  val productCode = match.groups["code"]!!.value
  val releaseVersion = match.groups["release"]?.value
  return if (releaseVersion != null) {
    StarterIdeVersion(productCode, releaseVersion, "RELEASE")
  }
  else {
    StarterIdeVersion(productCode, platformVersion, "EAP")
  }
}
