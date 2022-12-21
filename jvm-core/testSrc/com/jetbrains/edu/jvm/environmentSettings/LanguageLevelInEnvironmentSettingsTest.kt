package com.jetbrains.edu.jvm.environmentSettings

import com.intellij.openapi.projectRoots.JavaSdkVersion
import com.intellij.openapi.projectRoots.impl.JavaSdkImpl
import com.intellij.openapi.projectRoots.impl.MockSdk
import com.intellij.pom.java.LanguageLevel
import com.intellij.testFramework.LightPlatformTestCase
import com.intellij.util.containers.MultiMap
import com.jetbrains.edu.jvm.JdkLanguageSettings
import com.jetbrains.edu.jvm.setLanguageLevel
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.newproject.ui.errors.SettingsValidationResult

class LanguageLevelInEnvironmentSettingsTest : LightPlatformTestCase() {

  fun `test jdk validation messages`() {
    val course = course {}

    val languageLevels = listOf(null, LanguageLevel.JDK_1_8, LanguageLevel.JDK_17, LanguageLevel.JDK_19_PREVIEW)
      .map { it?.name }
      .plus("57.121") // unsupported

    val selectedSdkVersions = listOf(null, JavaSdkVersion.JDK_11, JavaSdkVersion.JDK_17, JavaSdkVersion.JDK_19)
      .map { it?.description }
      .plus("2.39") // unsupported

    fun doTest(languageLevel: String?, selectedSdkVersion: String?): String? {
      course.setLanguageLevel(languageLevel)

      val sdk = selectedSdkVersion?.let { MockSdk(it, "", it, MultiMap(), JavaSdkImpl()) }
      val languageSettings = JdkLanguageSettings()
      languageSettings.selectJdk(sdk)

      val validation = languageSettings.validate(course, "")

      assertTrue(validation is SettingsValidationResult.Ready)
      val ready = validation as SettingsValidationResult.Ready
      return ready.validationMessage?.message
    }

    val csv = StringBuffer("language level|selected jdk|validation message\n")
    for (languageLevel in languageLevels)
      for (selectedSdkVersion in selectedSdkVersions)
        csv.append("$languageLevel|$selectedSdkVersion|${doTest(languageLevel, selectedSdkVersion)}\n")

    val expectedTestResult = """
      language level|selected jdk|validation message
      null|null|JDK is not selected. In the settings section, choose or download some JDK
      null|11|null
      null|17|null
      null|19|null
      null|2.39|Failed to determine Java version from string: 2.39. In the settings section, choose or download another JDK
      JDK_1_8|null|JDK is not selected. In the settings section, choose or download some JDK with a version at least 8
      JDK_1_8|11|null
      JDK_1_8|17|null
      JDK_1_8|19|null
      JDK_1_8|2.39|Failed to determine Java version from string: 2.39. In the settings section, choose or download another JDK
      JDK_17|null|JDK is not selected. In the settings section, choose or download some JDK with a version at least 17
      JDK_17|11|Your Java version is 11, while it should be at least 17. In the settings section, choose or download a newer version of JDK
      JDK_17|17|null
      JDK_17|19|null
      JDK_17|2.39|Failed to determine Java version from string: 2.39. In the settings section, choose or download another JDK
      JDK_19_PREVIEW|null|JDK is not selected. In the settings section, choose or download some JDK with a version at least 19
      JDK_19_PREVIEW|11|Your Java version is 11, while it should be at least 19. In the settings section, choose or download a newer version of JDK
      JDK_19_PREVIEW|17|Your Java version is 17, while it should be at least 19. In the settings section, choose or download a newer version of JDK
      JDK_19_PREVIEW|19|null
      JDK_19_PREVIEW|2.39|Failed to determine Java version from string: 2.39. In the settings section, choose or download another JDK
      57.121|null|Unsupported Java version: 57.121
      57.121|11|Unsupported Java version: 57.121
      57.121|17|Unsupported Java version: 57.121
      57.121|19|Unsupported Java version: 57.121
      57.121|2.39|Unsupported Java version: 57.121

    """.trimIndent()

    assertEquals(expectedTestResult, csv.toString())
  }
}