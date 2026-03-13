package com.jetbrains.edu.jvm.gradle.generation

import com.intellij.openapi.externalSystem.service.execution.ExternalSystemJdkUtil.USE_PROJECT_JDK
import com.intellij.openapi.projectRoots.JavaSdk
import com.intellij.openapi.projectRoots.JavaSdkVersion
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.projectRoots.SdkType
import com.intellij.openapi.projectRoots.impl.jdkDownloader.JdkDownloadUtil
import com.intellij.util.lang.JavaVersion
import com.jetbrains.edu.jvm.gradle.generation.EduGradleUtils.setUpGradleJvm
import com.jetbrains.edu.jvm.lookupJdkByPath
import com.jetbrains.edu.learning.EduTestCase
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.spyk
import io.mockk.unmockkAll
import org.gradle.util.GradleVersion
import org.jetbrains.plugins.gradle.jvmcompat.GradleJvmSupportMatrix
import org.jetbrains.plugins.gradle.settings.GradleProjectSettings
import org.junit.Test
import java.nio.file.Path

class EduGradleUtilsSetupGradleJvmTest : EduTestCase() {

  private lateinit var javaSdk: JavaSdk
  private lateinit var gradleProjectSettings: GradleProjectSettings
  private val java21: JavaVersion = JavaVersion.parse("21")
  private val validJdkHome: String = "/home/user/.jdks/openjdk-21.0.2"

  override fun setUp() {
    super.setUp()

    mockkObject(GradleJvmSupportMatrix)
    mockkObject(JdkDownloadUtil)
    mockkStatic(::lookupJdkByPath)

    javaSdk = JavaSdk.getInstance()
    mockkObject(javaSdk)
    every { javaSdk.collectSdkEntries(null) } returns emptyList<SdkType.SdkEntry>()
    every { javaSdk.getVersion(any()) } returns null

    gradleProjectSettings = spyk(GradleProjectSettings())
    every { gradleProjectSettings.resolveGradleVersion() } returns GradleVersion.version("8.14.3")

    every { GradleJvmSupportMatrix.isSupported(any(), any()) } returns false
    every { GradleJvmSupportMatrix.suggestLatestSupportedJavaVersion(any()) } returns java21
  }

  @Test
  fun `project sdk is used when compatible`() {
    val projectSdk = mockSdk(name = "Project 21", homePath = "/project/jdk", version = "21")
    every { javaSdk.getVersion(projectSdk) } returns JavaSdkVersion.JDK_21
    every { GradleJvmSupportMatrix.isSupported(any(), java21) } returns true

    setUpGradleJvm(project, gradleProjectSettings, projectSdk)

    assertEquals(USE_PROJECT_JDK, gradleProjectSettings.gradleJvm)
  }

  @Test
  fun `compatible auto detected sdk is used when project sdk is incompatible`() {
    val projectSdk = mockSdk(name = "Project 17", homePath = "/project/jdk", version = "17")
    val autoDetectedEntry = sdkEntry(homePath = validJdkHome, version = "21")
    val lookedUpSdk = mockSdk(name = "Auto Detected 21", homePath = validJdkHome, version = "21")

    every { javaSdk.getVersion(projectSdk) } returns JavaSdkVersion.JDK_17
    every { javaSdk.collectSdkEntries(null) } returns listOf(autoDetectedEntry)
    every { lookupJdkByPath(project, validJdkHome) } returns lookedUpSdk
    every { GradleJvmSupportMatrix.isSupported(any(), java21) } returns true

    setUpGradleJvm(project, gradleProjectSettings, projectSdk)

    assertEquals("Auto Detected 21", gradleProjectSettings.gradleJvm)
  }

  @Test
  fun `test compatible configured sdk is used when project sdk is incompatible`() {
    val projectSdk = mockSdk(name = "Project 17", homePath = "/project/jdk", version = "17")
    val configuredEntry = sdkEntry(homePath = validJdkHome, version = "21")
    val configuredSdk = mockSdk(name = "Configured 21", homePath = validJdkHome, version = "21")

    every { javaSdk.getVersion(projectSdk) } returns JavaSdkVersion.JDK_17
    every { javaSdk.collectSdkEntries(null) } returns listOf(configuredEntry)
    every { lookupJdkByPath(project, validJdkHome) } returns configuredSdk
    every { GradleJvmSupportMatrix.isSupported(any(), java21) } returns true

    setUpGradleJvm(project, gradleProjectSettings, projectSdk)

    assertEquals("Configured 21", gradleProjectSettings.gradleJvm)
  }

  @Test
  fun `test downloadable sdk is used when no compatible sdk exists`() {
    val projectSdk = mockSdk(name = "Project 17", homePath = "/project/jdk", version = "17")
    val downloadedSdk = mockSdk(name = "Downloaded 21", homePath = "/downloaded/jdk", version = "21")

    every { javaSdk.getVersion(projectSdk) } returns JavaSdkVersion.JDK_17

    val jdkItem = mockk<com.intellij.openapi.projectRoots.impl.jdkDownloader.JdkItem>(relaxed = true)
    val downloadTask = mockk<com.intellij.openapi.roots.ui.configuration.projectRoot.SdkDownloadTask>(relaxed = true)

    coEvery {
      JdkDownloadUtil.pickJdkItemAndPath(project, any())
    } returns (jdkItem to Path.of("/downloaded/jdk"))

    coEvery { JdkDownloadUtil.createDownloadTask(project, jdkItem, Path.of("/downloaded/jdk")) } returns downloadTask
    coEvery { JdkDownloadUtil.createDownloadSdk(any(), downloadTask) } returns downloadedSdk
    coEvery { JdkDownloadUtil.downloadSdk(downloadedSdk) } returns true

    setUpGradleJvm(project, gradleProjectSettings, projectSdk)

    assertEquals("Downloaded 21", gradleProjectSettings.gradleJvm)
  }

  @Test
  fun `test no compatible sdk entries means download branch is used`() {
    val projectSdk = mockSdk(name = "Project 17", homePath = "/project/jdk", version = "17")
    val downloadedSdk = mockSdk(name = "Downloaded 21", homePath = "/downloaded/jdk", version = "21")

    every { javaSdk.getVersion(projectSdk) } returns JavaSdkVersion.JDK_17
    every { javaSdk.collectSdkEntries(null) } returns emptyList()

    val jdkItem = mockk<com.intellij.openapi.projectRoots.impl.jdkDownloader.JdkItem>(relaxed = true)
    val downloadTask = mockk<com.intellij.openapi.roots.ui.configuration.projectRoot.SdkDownloadTask>(relaxed = true)

    coEvery {
      JdkDownloadUtil.pickJdkItemAndPath(project, any())
    } returns (jdkItem to Path.of("/downloaded/jdk"))

    coEvery { JdkDownloadUtil.createDownloadTask(project, jdkItem, Path.of("/downloaded/jdk")) } returns downloadTask
    coEvery { JdkDownloadUtil.createDownloadSdk(any(), downloadTask) } returns downloadedSdk
    coEvery { JdkDownloadUtil.downloadSdk(downloadedSdk) } returns true

    setUpGradleJvm(project, gradleProjectSettings, projectSdk)

    assertEquals("Downloaded 21", gradleProjectSettings.gradleJvm)
  }

  private fun mockSdk(name: String, homePath: String, version: String): Sdk {
    return mockk {
      every { this@mockk.name } returns name
      every { this@mockk.homePath } returns homePath
      every { this@mockk.versionString } returns version
    }
  }

  private fun sdkEntry(homePath: String, version: String): SdkType.SdkEntry = SdkType.SdkEntry(homePath, version)
}
