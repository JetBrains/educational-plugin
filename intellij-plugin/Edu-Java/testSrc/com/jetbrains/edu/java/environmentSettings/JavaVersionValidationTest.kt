package com.jetbrains.edu.java.environmentSettings

import com.intellij.openapi.projectRoots.JavaSdkVersion
import com.intellij.openapi.projectRoots.impl.JavaSdkImpl
import com.intellij.openapi.projectRoots.impl.MockSdk
import com.intellij.pom.java.LanguageLevel
import com.intellij.testFramework.LightPlatformTestCase
import com.intellij.util.containers.MultiMap
import com.jetbrains.edu.java.JLanguageSettings
import com.jetbrains.edu.jvm.setLanguageLevel
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.newproject.ui.errors.SettingsValidationResult

class JavaLevelInEnvironmentSettingsTest : LightPlatformTestCase() {

  //ashjdvashd
  //ashbdhsadb
  fun `test jdk validation messages`() {
    val course = course {}

    val courseLanguageVersions = listOf(null, JavaSdkVersion.JDK_11, JavaSdkVersion.JDK_17, JavaSdkVersion.JDK_19)
      .map { it?.description }
      .plus("6.67") // unsupported

    val languageLevels = listOf(null, LanguageLevel.JDK_19.name, /*unsupported*/ "57.121")

    val selectedSdkVersions = listOf(null, JavaSdkVersion.JDK_11, JavaSdkVersion.JDK_17, JavaSdkVersion.JDK_19)
      .map { it?.description }
      .plus("2.39") // unsupported

    fun doTest(courseLanguageVersion: String?, languageLevel: String?, selectedSdkVersion: String?): String? {
      course.setLanguageLevel(languageLevel)
      course.languageId = "Java"
      course.languageVersion = courseLanguageVersion

      val sdk = selectedSdkVersion?.let { MockSdk(it, "", it, MultiMap(), JavaSdkImpl()) }

      val languageSettings = JLanguageSettings()
      languageSettings.selectJdk(sdk)

      val validation = languageSettings.validate(course, "")

      assertTrue(validation is SettingsValidationResult.Ready)
      val ready = validation as SettingsValidationResult.Ready
      return ready.validationMessage?.message
    }

    val csv = StringBuffer("course language|selected jdk|validation message\n")
    for (courseLanguageVersion in courseLanguageVersions)
      for (selectedSdkVersion in selectedSdkVersions) {
        //iterate through all language levels, but the validation result must not depend on it
        val firstTestResult = doTest(courseLanguageVersion, languageLevels[0], selectedSdkVersion)

        for (languageLevel in languageLevels.drop(1)) {
          val anotherTestResult = doTest(courseLanguageVersion, languageLevel, selectedSdkVersion)
          assertEquals(firstTestResult, anotherTestResult)
        }

        csv.append("$courseLanguageVersion|$selectedSdkVersion|$firstTestResult\n")
      }

    val expectedTestResult = """
      course language|selected jdk|validation message
      null|null|JDK is not selected. In the settings section, choose or download some JDK with a version at least 8
      null|11|null
      null|17|null
      null|19|null
      null|2.39|Failed to determine Java version from string: 2.39. In the settings section, choose or download another JDK
      11|null|JDK is not selected. In the settings section, choose or download some JDK with a version at least 11
      11|11|null
      11|17|null
      11|19|null
      11|2.39|Failed to determine Java version from string: 2.39. In the settings section, choose or download another JDK
      17|null|JDK is not selected. In the settings section, choose or download some JDK with a version at least 17
      17|11|Your Java version is 11, while it should be at least 17. In the settings section, choose or download a newer version of JDK
      17|17|null
      17|19|null
      17|2.39|Failed to determine Java version from string: 2.39. In the settings section, choose or download another JDK
      19|null|JDK is not selected. In the settings section, choose or download some JDK with a version at least 19
      19|11|Your Java version is 11, while it should be at least 19. In the settings section, choose or download a newer version of JDK
      19|17|Your Java version is 17, while it should be at least 19. In the settings section, choose or download a newer version of JDK
      19|19|null
      19|2.39|Failed to determine Java version from string: 2.39. In the settings section, choose or download another JDK
      6.67|null|Unsupported Java version: 6.67
      6.67|11|Unsupported Java version: 6.67
      6.67|17|Unsupported Java version: 6.67
      6.67|19|Unsupported Java version: 6.67
      6.67|2.39|Unsupported Java version: 6.67
      
    """.trimIndent()

    assertEquals(expectedTestResult, csv.toString())
  }
}