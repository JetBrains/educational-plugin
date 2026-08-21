package com.jetbrains.edu.jvm.environmentSettings

import com.intellij.lang.Language
import com.intellij.testFramework.LightPlatformTestCase
import com.intellij.util.lang.JavaVersion
import com.jetbrains.edu.jvm.JVM_LANGUAGE_LEVEL
import com.jetbrains.edu.jvm.environment.JdkLanguageEnvironment
import com.jetbrains.edu.jvm.environment.JdkLanguageEnvironmentCatalogProvider
import com.jetbrains.edu.jvm.gradle.GradleConfiguratorBase
import com.jetbrains.edu.learning.EnvironmentAwareCourseBuilder
import com.jetbrains.edu.learning.Err
import com.jetbrains.edu.learning.Ok
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.ext.configurator
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import kotlin.test.assertIs

sealed interface ExpectedValidationResult {
  sealed interface Validity : ExpectedValidationResult {
    val valid: Boolean
      get() = when (this) {
        is Valid -> true
        is Invalid -> false
      }
    val preferredVersion: Int?
  }
  data class Valid(override val preferredVersion: Int? = null) : Validity
  data class Invalid(override val preferredVersion: Int? = null) : Validity

  data class Error(val message: String): ExpectedValidationResult
}

@RunWith(Parameterized::class)
abstract class JdkVersionValidationTestBase(
  private val courseLanguageVersion: String?,
  private val courseLanguageLevel: String?,
  private val gradleVersion: String?,

  private val sdkVersion: String,

  private val expectedValidationResult: ExpectedValidationResult,
) : LightPlatformTestCase() {

  protected abstract val language: Language
  protected open val environment: String = ""

  @Test
  fun `jdk validation messages`() {
    val course = course(language = language, environment = environment) {}
    course.languageVersion = courseLanguageVersion
    course.setLanguageLevel(courseLanguageLevel)
    course.setGradleVersion(gradleVersion)

    @Suppress("UNCHECKED_CAST")
    val courseBuilder = course.configurator?.courseBuilder as? EnvironmentAwareCourseBuilder<JdkLanguageEnvironment> ?: error("Course builder is absent")
    val jdkCatalogProvider = courseBuilder.getLanguageEnvironmentCatalogProvider() as JdkLanguageEnvironmentCatalogProvider

    val actualResult = jdkCatalogProvider.suitableJdkVersions(course)

    when (expectedValidationResult) {
      is ExpectedValidationResult.Error -> {
        assertIs<Err<String>>(actualResult, "suitableJdkVersions(course) should return error: ${expectedValidationResult.message}")
        assertEquals("wrong error message", expectedValidationResult.message, actualResult.error)
      }
      is ExpectedValidationResult.Validity -> {
        assertIs<Ok<JdkLanguageEnvironmentCatalogProvider.SuitableJdkVersions>>(actualResult, "suitableJdkVersions(course) should not return error: ${(actualResult as? Err<String>)?.error}")
        val (jdkVersionRange, preferredVersion) = actualResult.value

        assertEquals("preferred version is not correct", expectedValidationResult.preferredVersion, preferredVersion)

        val parsedJdkVersion = JavaVersion.tryParse(sdkVersion) ?: error("Specify parsable JDK version: $sdkVersion")
        assertEquals("jdk version validity is not correct", expectedValidationResult.valid, jdkVersionRange.contains(parsedJdkVersion.feature))
      }
    }
  }

  private fun Course.setLanguageLevel(languageLevel: String?) {
    course.environmentSettings = if (languageLevel == null) {
      course.environmentSettings.minus(JVM_LANGUAGE_LEVEL)
    }
    else {
      course.environmentSettings.plus(JVM_LANGUAGE_LEVEL to languageLevel)
    }
  }

  private fun Course.setGradleVersion(gradleVersion: String?) {
    course.environmentSettings = if (gradleVersion == null) {
      course.environmentSettings.minus(GradleConfiguratorBase.ENV_SETTINGS_GRADLE_VERSION)
    }
    else {
      course.environmentSettings.plus(GradleConfiguratorBase.ENV_SETTINGS_GRADLE_VERSION to gradleVersion)
    }
  }
}
