package com.jetbrains.edu.jvm.environmentSettings

import com.intellij.lang.Language
import com.intellij.openapi.projectRoots.JavaSdk
import com.intellij.openapi.projectRoots.impl.ProjectJdkImpl
import com.intellij.testFramework.LightPlatformTestCase
import com.jetbrains.edu.jvm.JdkLanguageSettings
import com.jetbrains.edu.jvm.setLanguageLevel
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.ext.configurator
import com.jetbrains.edu.learning.newproject.ui.errors.SettingsValidationResult
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import kotlin.test.assertIs

@RunWith(Parameterized::class)
abstract class JdkVersionValidationTestBase(
  private val languageVersion: String?,
  private val languageLevel: String?,
  private val selectedSdkVersion: String?,
  private val expectedValidationMessage: String?
) : LightPlatformTestCase() {

  protected abstract val langauge: Language
  protected open val environment: String = ""

  @Test
  fun `jdk validation messages`() {
    val course = course(language = langauge, environment = environment) {}
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
}
