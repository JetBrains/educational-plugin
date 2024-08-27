package com.jetbrains.edu.jvm

import com.intellij.openapi.projectRoots.JavaSdkVersion
import com.intellij.pom.java.LanguageLevel

/**
 * This is a wrapper for [JavaSdkVersion] with additional values: java version not provided, failed to parse java version
 */
sealed class ParsedJavaVersion {
  companion object {
    private fun fromLanguageLevel(languageLevel: LanguageLevel?): ParsedJavaVersion {
      languageLevel ?: return JavaVersionNotProvided
      return JavaVersionParseSuccess(
        JavaSdkVersion.fromLanguageLevel(languageLevel)
      )
    }

    fun fromStringLanguageLevel(languageLevelAsString: String?): ParsedJavaVersion {
      languageLevelAsString ?: return JavaVersionNotProvided

      val languageLevel = try {
        LanguageLevel.valueOf(languageLevelAsString)
      }
      catch (_: IllegalArgumentException) {
        return JavaVersionParseFailed(languageLevelAsString)
      }

      return fromLanguageLevel(languageLevel)
    }

    fun fromJavaSdkDescriptionString(javaSdkDescription: String?): ParsedJavaVersion {
      javaSdkDescription ?: return JavaVersionNotProvided

      val javaSdkVersion = JavaSdkVersion.values().find { it.description == javaSdkDescription }

      javaSdkVersion ?: return JavaVersionParseFailed(javaSdkDescription)

      return JavaVersionParseSuccess(javaSdkVersion)
    }

    fun fromJavaSdkVersionString(versionString: String?): ParsedJavaVersion {
      versionString ?: return JavaVersionNotProvided
      val parsedVersion = JavaSdkVersion.fromVersionString(versionString)
      return if (parsedVersion == null) {
        JavaVersionParseFailed(versionString)
      }
      else {
        JavaVersionParseSuccess(parsedVersion)
      }
    }
  }
}

data class JavaVersionParseSuccess(val javaSdkVersion: JavaSdkVersion) : ParsedJavaVersion() {
  infix fun isAtLeast(that: JavaVersionParseSuccess): Boolean = javaSdkVersion.isAtLeast(that.javaSdkVersion)
  infix fun isGreater(that: JavaVersionParseSuccess): Boolean = javaSdkVersion > that.javaSdkVersion
}

object JavaVersionNotProvided : ParsedJavaVersion()
data class JavaVersionParseFailed(val versionAsText: String) : ParsedJavaVersion()