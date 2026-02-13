import org.jetbrains.intellij.platform.gradle.extensions.IntelliJPlatformDependenciesExtension
import org.jetbrains.intellij.platform.gradle.models.Coordinates

plugins {
  id("intellij-plugin-module-conventions")
}

dependencies {
  intellijPlatform {
    intellijIde(riderVersion)
    intellijPlugins(csharpPlugins)

    bundledModule("intellij.rider")
    if (isAtLeast261) {
      bundledModule("intellij.rider.debugger.shared")
      // TODO: remove it and use testFramework(TestFrameworkType.Plugins.Rider) when new version of IPGP will be available
      riderTestFramework()
    }
  }

  implementation(project(":intellij-plugin:educational-core"))
  testImplementation(project(":intellij-plugin:educational-core", "testOutput"))
}

// See: RIDER-131602
// Implementation from a related commit in issue
fun IntelliJPlatformDependenciesExtension.riderTestFramework() {
  val (_, riderVersionNumber) = riderVersion.toTypeWithVersion()
  val testFrameworkVersion = "RIDER-$riderVersionNumber"
  fun riderTestPlatformDependency(artifact: String) {
    testPlatformDependency(Coordinates("com.jetbrains.intellij.rider", artifact), testFrameworkVersion)
  }
  riderTestPlatformDependency("rider-test-framework")
  riderTestPlatformDependency("rider-test-framework-core")
  riderTestPlatformDependency("rider-test-framework-testng")
}
