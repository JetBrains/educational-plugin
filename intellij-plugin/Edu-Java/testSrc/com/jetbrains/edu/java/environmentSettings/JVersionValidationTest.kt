package com.jetbrains.edu.java.environmentSettings

import com.intellij.lang.java.JavaLanguage
import com.intellij.openapi.projectRoots.JavaSdk
import com.intellij.openapi.projectRoots.JavaSdkVersion.*
import com.intellij.openapi.projectRoots.impl.ProjectJdkImpl
import com.intellij.pom.java.LanguageLevel
import com.intellij.testFramework.LightPlatformTestCase
import com.jetbrains.edu.jvm.JdkLanguageSettings
import com.jetbrains.edu.jvm.setLanguageLevel
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.ext.configurator
import com.jetbrains.edu.learning.newproject.ui.errors.SettingsValidationResult
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.Parameterized.Parameters
import kotlin.test.assertIs

@RunWith(Parameterized::class)
class JVersionValidationTest(
  private val languageVersion: String?,
  private val languageLevel: String?,
  private val selectedSdkVersion: String?,
  private val expectedValidationMessage: String?
) : LightPlatformTestCase() {

  @Test
  fun `jdk validation messages`() {
    val course = course(language = JavaLanguage.INSTANCE) {}
    course.languageVersion = languageVersion
    course.setLanguageLevel(languageLevel)
    val sdk = selectedSdkVersion?.let { ProjectJdkImpl(it, JavaSdk.getInstance(), "", it) }
    val languageSettings = course.configurator?.courseBuilder?.getLanguageSettings()
    assertIs<JdkLanguageSettings>(languageSettings)
    languageSettings.selectJdk(sdk)

    val validationResult = languageSettings.validate(course, "")
    assertIs<SettingsValidationResult.Ready>(validationResult)

    val validationMessage = validationResult.validationMessage?.message
    assertEquals(expectedValidationMessage, validationMessage)
  }

  companion object {
    @JvmStatic
    @Parameters(name = "language version: {0}, language level: {1}, selected sdk version: {2}")
    fun data(): Collection<Array<Any?>> {
      val languageLevels = listOf(null, LanguageLevel.JDK_19.name, /*unsupported*/ "57.121")

      return listOf(
        arrayOf(null, null, "JDK is not selected. In the settings section, choose or download some JDK with a version at least 8"),
        arrayOf(null, JDK_11.description, null),
        arrayOf(null, JDK_17.description, null),
        arrayOf(null, JDK_19.description, null),
        arrayOf(null, "2.39", "Failed to determine Java version from string: 2.39. In the settings section, choose or download another JDK"),
        arrayOf(JDK_11.description, null, "JDK is not selected. In the settings section, choose or download some JDK with a version at least 11"),
        arrayOf(JDK_11.description, JDK_11.description, null),
        arrayOf(JDK_11.description, JDK_17.description, null),
        arrayOf(JDK_11.description, JDK_19.description, null),
        arrayOf(JDK_11.description, "2.39", "Failed to determine Java version from string: 2.39. In the settings section, choose or download another JDK"),
        arrayOf(JDK_17.description, null, "JDK is not selected. In the settings section, choose or download some JDK with a version at least 17"),
        arrayOf(JDK_17.description, JDK_11.description, "Your Java version is 11, while it should be at least 17. In the settings section, choose or download a newer version of JDK"),
        arrayOf(JDK_17.description, JDK_17.description, null),
        arrayOf(JDK_17.description, JDK_19.description, null),
        arrayOf(JDK_17.description, "2.39", "Failed to determine Java version from string: 2.39. In the settings section, choose or download another JDK"),
        arrayOf(JDK_19.description, null, "JDK is not selected. In the settings section, choose or download some JDK with a version at least 19"),
        arrayOf(JDK_19.description, JDK_11.description, "Your Java version is 11, while it should be at least 19. In the settings section, choose or download a newer version of JDK"),
        arrayOf(JDK_19.description, JDK_17.description, "Your Java version is 17, while it should be at least 19. In the settings section, choose or download a newer version of JDK"),
        arrayOf(JDK_19.description, JDK_19.description, null),
        arrayOf(JDK_19.description, "2.39", "Failed to determine Java version from string: 2.39. In the settings section, choose or download another JDK"),
        arrayOf("6.67", null, "Unsupported Java version: 6.67"),
        arrayOf("6.67", JDK_11.description, "Unsupported Java version: 6.67"),
        arrayOf("6.67", JDK_17.description, "Unsupported Java version: 6.67"),
        arrayOf("6.67", JDK_19.description, "Unsupported Java version: 6.67"),
        arrayOf("6.67", "2.39", "Unsupported Java version: 6.67")
      ).flatMap { (languageVersion, sdkVersion, message) ->
        // iterate through all language levels, but the validation result must not depend on it for Java
        languageLevels.map { languageLevel ->
          arrayOf(languageVersion, languageLevel, sdkVersion, message)
        }
      }
    }
  }
}
