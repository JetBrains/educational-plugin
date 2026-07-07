package com.jetbrains.edu.kotlin.environmentSettings

import com.intellij.lang.Language
import com.intellij.pom.java.LanguageLevel
import com.jetbrains.edu.jvm.environmentSettings.ExpectedValidationResult
import com.jetbrains.edu.jvm.environmentSettings.ExpectedValidationResult.*
import com.jetbrains.edu.jvm.environmentSettings.JdkVersionValidationTestBase
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.junit.runners.Parameterized.Parameters
import kotlin.arrayOf

class KtVersionValidationTest(
  languageVersion: String?,
  languageLevel: String?,
  gradleVersion: String?,
  sdkVersion: String,
  expectedValidationResult: ExpectedValidationResult
) : JdkVersionValidationTestBase(languageVersion, languageLevel, gradleVersion, sdkVersion, expectedValidationResult) {

  override val language: Language get() = KotlinLanguage.INSTANCE

  companion object {
    @JvmStatic
    @Parameters(name = "language version: {0}, language level: {1}, gradle version: {2}, selected sdk version: {3}")
    fun data(): Collection<Array<Any?>> {
      val languageVersions = listOf(null, LanguageLevel.JDK_19.name, /*unsupported*/ "57.121")

      return listOf(
        // language level, gradle version, sdk version to test, validation result

        arrayOf(null, null, "8", Valid(24)), // no gradle (for learners it defaults to 8.14.3), no language level
        arrayOf(null, null, "17", Valid(24)),
        arrayOf(null, null, "25", Invalid(24)),

        arrayOf(null, "7.6.1", "8", Valid(19)), // old gradle, no language level
        arrayOf(null, "7.6.1", "17", Valid(19)),
        arrayOf(null, "7.6.1", "25", Invalid(19)),

        arrayOf(null, "9.3.0", "8", Invalid(25)), // modern gradle, no language level
        arrayOf(null, "9.3.0", "16", Invalid(25)),
        arrayOf(null, "9.3.0", "17", Valid(25)),
        arrayOf(null, "9.3.0", "25", Valid(25)),

        arrayOf(null, "9000.3.2", "8", Invalid()), // cutting age gradle, no language level
        arrayOf(null, "9000.3.2", "16", Invalid()),
        arrayOf(null, "9000.3.2", "17", Valid()),
        arrayOf(null, "9000.3.2", "25", Valid()),

        // course has old Java version

        arrayOf(LanguageLevel.JDK_1_8.name, null, "8", Valid(8)), // no gradle (for learners it defaults to 8.14.3), no language level
        arrayOf(LanguageLevel.JDK_1_8.name, null, "17", Valid(8)),
        arrayOf(LanguageLevel.JDK_1_8.name, null, "25", Invalid(8)),

        arrayOf(LanguageLevel.JDK_1_8.name, "7.6.1", "8", Valid(8)), // old gradle, no language level
        arrayOf(LanguageLevel.JDK_1_8.name, "7.6.1", "17", Valid(8)),
        arrayOf(LanguageLevel.JDK_1_8.name, "7.6.1", "25", Invalid(8)),

        arrayOf(LanguageLevel.JDK_1_8.name, "9.6.1", "8", Invalid(17)), // modern gradle, no language level
        arrayOf(LanguageLevel.JDK_1_8.name, "9.6.1", "16", Invalid(17)),
        arrayOf(LanguageLevel.JDK_1_8.name, "9.6.1", "17", Valid(17)),
        arrayOf(LanguageLevel.JDK_1_8.name, "9.6.1", "25", Valid(17)),

        // course has modern Java version

        arrayOf(LanguageLevel.JDK_18.name, null, "8", Invalid(18)), // no gradle (for learners it defaults to 8.14.3), no language level
        arrayOf(LanguageLevel.JDK_18.name, null, "17", Invalid(18)),
        arrayOf(LanguageLevel.JDK_18.name, null, "25", Invalid(18)),

        arrayOf(LanguageLevel.JDK_18.name, "7.6.1", "8", Invalid(18)), // old gradle, no language level
        arrayOf(LanguageLevel.JDK_18.name, "7.6.1", "17", Invalid(18)),
        arrayOf(LanguageLevel.JDK_18.name, "7.6.1", "25", Invalid(18)),

        arrayOf(LanguageLevel.JDK_18.name, "9.6.1", "8", Invalid(18)), // modern gradle, no language level
        arrayOf(LanguageLevel.JDK_18.name, "9.6.1", "16", Invalid(18)),
        arrayOf(LanguageLevel.JDK_18.name, "9.6.1", "17", Invalid(18)),
        arrayOf(LanguageLevel.JDK_18.name, "9.6.1", "25", Valid(18)),

        // course has funny but incorrect Java version

        arrayOf("57.121.i", null, "8", Error("Unknown required Java version: 57.121.i")),
        arrayOf("57.121.i", "7.6.1", "17", Error("Unknown required Java version: 57.121.i")),
        arrayOf("57.121.i", "9.6.1", "25", Error("Unknown required Java version: 57.121.i")),

        // special cases
        arrayOf(LanguageLevel.JDK_18.name, "9000.3.2", "25", Valid(18)), // cutting age gradle with language version
        arrayOf(LanguageLevel.JDK_25.name, "7.6.1", "25", Error("The course JDK version range (At least JDK 25) contradicts the build system version range (From JDK 8 to JDK 19).")), // old gradle with modern language version

      ).flatMap { (languageLevel, gradleVersion, sdkVersion, message) ->
        // iterate through all language versions, but the validation result must not depend on it for Kotlin
        languageVersions.map { languageVersion ->
          arrayOf(languageVersion, languageLevel, gradleVersion, sdkVersion, message)
        }
      }
    }
  }
}
