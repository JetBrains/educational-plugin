package com.jetbrains.edu.learning.stepik.hyperskill

import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.compatibility.CourseCompatibilityProviderEP


enum class HyperskillLanguages(private val id: String, private val languageName: String) {
  GO(EduNames.GO, "go"),
  JAVASCRIPT(EduNames.JAVASCRIPT, "javascript"),
  JAVA(EduNames.JAVA, "java11") {
    override val eduLanguage: String = "${EduNames.JAVA} 11"
    override val requestLanguage: String = "java"
  },
  KOTLIN(EduNames.KOTLIN, "kotlin"),
  PYTHON(EduNames.PYTHON, "python3") {
    override val requestLanguage: String = "python"
  },
  SCALA(EduNames.SCALA, "scala"),

  // last three needed for tests
  PLAINTEXT("TEXT", "TEXT"),
  @Suppress("unused")
  FAKE_GRADLE_BASE("FakeGradleBasedLanguage", "FakeGradleBasedLanguage"),
  UNSUPPORTED("Unsupported", "Unsupported");

  /**
   * Edu language is language used for Course
   * @see [com.jetbrains.edu.learning.courseFormat.Course.getLanguage]
   */
  open val eduLanguage: String = id

  /**
   * Request language is language plugin received from JBA in requests
   * @see [com.jetbrains.edu.learning.stepik.hyperskill.courseGeneration.HyperskillOpenStepRequest]
   */
  open val requestLanguage: String = languageName

  override fun toString(): String = id.toLowerCase().capitalize()

  companion object {
    @JvmStatic
    fun getEduLanguage(hyperskillLanguage: String): String? {
      return values().find { it.requestLanguage == hyperskillLanguage }?.eduLanguage
    }

    @JvmStatic
    fun getRequestLanguage(eduLanguage: String): String? {
      return values().find {it.eduLanguage == eduLanguage}?.requestLanguage
    }

    @JvmStatic
    fun getLanguageName(languageId: String): String? {
      return values().find { it.id == languageId }?.languageName
    }

    @JvmStatic
    fun getAvailableLanguages(): Set<HyperskillLanguages> {
      return values().filter { language -> CourseCompatibilityProviderEP.find(language.id) != null }.toSet()
    }
  }
}