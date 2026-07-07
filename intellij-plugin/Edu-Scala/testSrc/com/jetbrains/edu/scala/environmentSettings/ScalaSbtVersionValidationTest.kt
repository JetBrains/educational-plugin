package com.jetbrains.edu.scala.environmentSettings

import com.intellij.lang.Language
import com.intellij.pom.java.LanguageLevel
import com.jetbrains.edu.jvm.environmentSettings.ExpectedValidationResult
import com.jetbrains.edu.jvm.environmentSettings.ExpectedValidationResult.*
import com.jetbrains.edu.jvm.environmentSettings.JdkVersionValidationTestBase
import org.jetbrains.plugins.scala.ScalaLanguage
import org.junit.runners.Parameterized.Parameters

class ScalaSbtVersionValidationTest(
  languageVersion: String?,
  languageLevel: String?,
  gradleVersion: String?,
  sdkVersion: String,
  expectedValidationResult: ExpectedValidationResult
) : JdkVersionValidationTestBase(languageVersion, languageLevel, gradleVersion, sdkVersion, expectedValidationResult) {

  override val language: Language get() = ScalaLanguage.INSTANCE
  override val environment: String = "sbt"

  companion object {
    @JvmStatic
    @Parameters(name = "language version: {0}, language level: {1}, gradle version: {2}, selected sdk version: {3}")
    fun data(): Collection<Array<Any?>> {
      val languageVersions = listOf(null, LanguageLevel.JDK_19.name, /*unsupported*/ "57.121")

      return listOf(
        // language level, sdk version to test, validation result

        arrayOf(null, "8", Valid()), // no language level
        arrayOf(null, "17", Valid()),
        arrayOf(null, "25", Valid()),

        // course has old Java version

        arrayOf(LanguageLevel.JDK_1_8.name, "8", Valid(8)),
        arrayOf(LanguageLevel.JDK_1_8.name, "17", Valid(8)),
        arrayOf(LanguageLevel.JDK_1_8.name, "25", Valid(8)),

        // course has modern Java version

        arrayOf(LanguageLevel.JDK_25.name, "8", Invalid(25)),
        arrayOf(LanguageLevel.JDK_25.name, "17", Invalid(25)),
        arrayOf(LanguageLevel.JDK_25.name, "25", Valid(25)),

        // course has funny but incorrect Java version

        arrayOf("57.121.i", "8", Error("Unknown required Java version: 57.121.i")),
        arrayOf("57.121.i", "17", Error("Unknown required Java version: 57.121.i")),
        arrayOf("57.121.i", "25", Error("Unknown required Java version: 57.121.i"))

      ).flatMap { (languageLevel, sdkVersion, message) ->
        // iterate through all language versions, but the validation result must not depend on it for Scala SBT course
        languageVersions.map { languageVersion ->
          arrayOf(languageVersion, languageLevel, null, sdkVersion, message)
        }
      }
    }
  }
}
