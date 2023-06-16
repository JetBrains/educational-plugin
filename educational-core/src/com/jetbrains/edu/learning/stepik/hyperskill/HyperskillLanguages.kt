package com.jetbrains.edu.learning.stepik.hyperskill

import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.capitalize


enum class HyperskillLanguages(private val id: String, private val languageName: String) {
  GO(EduNames.GO, "go"),
  JAVASCRIPT(EduNames.JAVASCRIPT, "javascript"),
  JAVA(EduNames.JAVA, "java11") {
    override val languageVersion: String = "11"
    override val requestLanguage: String = "java"
  },
  KOTLIN(EduNames.KOTLIN, "kotlin"),
  PYTHON(EduNames.PYTHON, "python3") {
    override val requestLanguage: String = "python"
  },
  SCALA(EduNames.SCALA, "scala"),
  SHELL(EduNames.SHELL, "shell"),

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
   * @see [com.jetbrains.edu.learning.stepik.hyperskill.courseGeneration.HyperskillOpenStepRequest]
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