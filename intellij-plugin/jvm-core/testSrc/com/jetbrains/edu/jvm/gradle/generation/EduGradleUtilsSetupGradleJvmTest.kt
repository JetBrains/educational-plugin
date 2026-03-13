package com.jetbrains.edu.jvm.gradle.generation

import com.intellij.openapi.externalSystem.service.execution.ExternalSystemJdkUtil
import com.intellij.openapi.externalSystem.service.execution.ExternalSystemJdkUtil.USE_PROJECT_JDK
import com.intellij.openapi.projectRoots.JavaSdk
import com.intellij.openapi.projectRoots.JavaSdkVersion
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.projectRoots.SdkType
import com.intellij.openapi.projectRoots.impl.jdkDownloader.JdkDownloadUtil
import com.intellij.openapi.projectRoots.impl.jdkDownloader.JdkItem
import com.intellij.openapi.roots.ui.configuration.projectRoot.SdkDownloadTask
import com.intellij.util.lang.JavaVersion
import com.jetbrains.edu.jvm.gradle.generation.EduGradleUtils.setUpGradleJvm
import com.jetbrains.edu.jvm.lookupJdkByPath
import com.jetbrains.edu.learning.EduTestCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.spyk
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
    every { javaSdk.collectSdkEntries(null) } returns emptyList()
    every { javaSdk.getVersion(any()) } returns null

    gradleProjectSettings = spyk(GradleProjectSettings())
    every { gradleProjectSettings.resolveGradleVersion() } returns GradleVersion.version("8.14.3")

    every { GradleJvmSupportMatrix.isSupported(any(), any()) } returns false
    every { GradleJvmSupportMatrix.suggestLatestSupportedJavaVersion(any()) } returns java21
  }

  @Test
  fun `project sdk is used when compatible`() {
    // Given
    val projectSdk = mockSdk(name = "Project 21", homePath = "/project/jdk", version = "21")
    every { javaSdk.getVersion(projectSdk) } returns JavaSdkVersion.JDK_21
    every { GradleJvmSupportMatrix.isSupported(any(), java21) } returns true

    // When
    setUpGradleJvm(project, gradleProjectSettings, projectSdk)

    // Then
    assertEquals(USE_PROJECT_JDK, gradleProjectSettings.gradleJvm)
  }

  @Test
  fun `compatible auto detected or already configured sdk is used when project sdk is incompatible`() {
    // Given
    val projectSdk = mockSdk(name = "Project 25", homePath = "/project/jdk", version = "25")
    val autoDetectedEntry = sdkEntry(homePath = validJdkHome, version = "21")
    val lookedUpSdk = mockSdk(name = "Auto Detected 21", homePath = validJdkHome, version = "21")

    mockkStatic(ExternalSystemJdkUtil::class)

    every { javaSdk.getVersion(projectSdk) } returns JavaSdkVersion.JDK_25
    every { javaSdk.collectSdkEntries(null) } returns listOf(autoDetectedEntry)
    every { ExternalSystemJdkUtil.isValidJdk(validJdkHome) } returns true
    every { lookupJdkByPath(project, validJdkHome) } returns lookedUpSdk
    every { GradleJvmSupportMatrix.isSupported(any(), java21) } returns true

    // When
    setUpGradleJvm(project, gradleProjectSettings, projectSdk)

    // Then
    assertEquals("Auto Detected 21", gradleProjectSettings.gradleJvm)
  }

  @Test
  fun `downloadable sdk is used when no compatible sdk exists`() {
    // Given
    val projectSdk = mockSdk(name = "Project 25", homePath = "/project/jdk", version = "25")
    val downloadableSdk = mockSdk(name = "Downloadable 21", homePath = "/home/user/.jdks/jdk-21", version = "21")

    every { javaSdk.getVersion(projectSdk) } returns JavaSdkVersion.JDK_25

    val jdkItem = mockk<JdkItem>(relaxed = true)
    val downloadTask = mockk<SdkDownloadTask>(relaxed = true)

    coEvery {
      JdkDownloadUtil.pickJdkItemAndPath(project, any())
    } returns (jdkItem to Path.of("/home/user/.jdks/jdk-21"))

    coEvery { JdkDownloadUtil.createDownloadTask(project, jdkItem, Path.of("/home/user/.jdks/jdk-21")) } returns downloadTask
    coEvery { JdkDownloadUtil.createDownloadSdk(any(), downloadTask) } returns downloadableSdk
    coEvery { JdkDownloadUtil.downloadSdk(downloadableSdk) } returns true

    // When
    setUpGradleJvm(project, gradleProjectSettings, projectSdk)

    // Then
    assertEquals("Downloadable 21", gradleProjectSettings.gradleJvm)
    coVerify(exactly = 1) { JdkDownloadUtil.downloadSdk(downloadableSdk) }
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
