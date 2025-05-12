package com.jetbrains.edu.learning.stepik.hyperskill

import com.jetbrains.edu.learning.capitalize
import com.jetbrains.edu.learning.courseFormat.EduFormatNames


enum class HyperskillLanguages(private val id: String, private val languageName: String) {
  GO(EduFormatNames.GO, "go"),
  JAVASCRIPT(EduFormatNames.JAVASCRIPT, "javascript"),
  JAVA(EduFormatNames.JAVA, "java11") {
    override val languageVersion: String = "11"
    override val requestLanguage: String = "java"
  },
  KOTLIN(EduFormatNames.KOTLIN, "kotlin"),
  PYTHON(EduFormatNames.PYTHON, "python3") {
    override val requestLanguage: String = "python"
  },
  PYTHON3_10(EduFormatNames.PYTHON, "python3") {
    override val requestLanguage: String = "python3.10"
  },
  SCALA(EduFormatNames.SCALA, "scala"),
  SCALA3(EduFormatNames.SCALA, "scala3"),
  SHELL(EduFormatNames.SHELL, "shell"),
  CPP(EduFormatNames.CPP, "cpp"),
  CSHARP(EduFormatNames.CSHARP, "unity"),

  // last three needed for tests
  PLAINTEXT("TEXT", "TEXT"),
  @Suppress("unused")
  FAKE_GRADLE_BASE("FakeGradleBasedLanguage", "FakeGradleBasedLanguage"),
  UNSUPPORTED("Unsupported", "Unsupported");

  /**
   * @see [com.jetbrains.edu.learning.courseFormat.Course.languageVersion]
   */
  open val languageVersion: String? = null

  /**
   * Request language is language plugin received from JBA in requests
   * @see [com.jetbrains.edu.learning.stepik.hyperskill.courseGeneration.HyperskillOpenStepWithProjectRequest]
   */
  open val requestLanguage: String = languageName

  override fun toString(): String = id.lowercase().capitalize() + if (languageVersion != null) " $languageVersion" else ""

  companion object {
    fun getLanguageIdAndVersion(hyperskillLanguage: String): Pair<String, String?>? {
      val language = values().find { it.requestLanguage == hyperskillLanguage } ?: return null
      return Pair(language.id, language.languageVersion)
    }

    fun getRequestLanguage(languageId: String): String? {
      return getHyperskillLanguage(languageId)?.requestLanguage
    }

    fun getLanguageName(languageId: String): String? {
      return getHyperskillLanguage(languageId)?.languageName
    }

    fun getHyperskillLanguage(languageId: String): HyperskillLanguages? {
      return values().find { it.id == languageId }
    }
  }
}