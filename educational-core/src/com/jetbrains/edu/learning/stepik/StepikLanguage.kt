package com.jetbrains.edu.learning.stepik

import com.jetbrains.edu.learning.EduNames

/**
 * Base on a class from intellij plugin from Stepik
 *
 * @see <a href="https://github.com/StepicOrg/intellij-plugins/blob/develop/stepik-union/src/main/java/org/stepik/core/SupportedLanguages.kt"> SupportedLanguages.kt</a>
 *
 */

private const val DEFAULT_VERSION = ""

enum class StepikLanguage(val id: String?, val version: String = DEFAULT_VERSION, val langName: String?, val isDefault: Boolean = true) {
  JAVA(EduNames.JAVA, "8", "java8"),
  JAVA11(EduNames.JAVA, "11", "java11", isDefault = false),
  KOTLIN(EduNames.KOTLIN, langName = "kotlin"),
  PYTHON(EduNames.PYTHON, EduNames.PYTHON_3_VERSION, langName = "python3"),
  JAVASCRIPT(EduNames.JAVASCRIPT, langName = "javascript"),
  SCALA(EduNames.SCALA, langName = "scala"),
  CPP(EduNames.CPP, langName = "c++"),
  GO(EduNames.GO, langName = "go"),
  PLAINTEXT("TEXT", langName = "TEXT"), // added for tests
  INVALID(null, langName = null);


  override fun toString(): String = id ?: ""

  companion object {
    private val NAME_MAP: Map<String?, StepikLanguage> by lazy {
      values().associateBy { it.langName }
    }

    private val TITLE_MAP: Map<Pair<String?, String>, StepikLanguage> by lazy {
      values().associateBy { it.id to it.version }
    }

    private val DEFAULT_TITLE_MAP: Map<String?, StepikLanguage> by lazy {
      values().filter { it.isDefault }.associateBy { it.id }
    }

    fun langOfName(lang: String): StepikLanguage = NAME_MAP.getOrElse(lang) { INVALID }

    fun langOfId(lang: String, version: String?): StepikLanguage {
      return if (version.isNullOrEmpty()) {
        DEFAULT_TITLE_MAP.getOrElse(lang) { INVALID }
      }
      else {
        TITLE_MAP.getOrElse(lang to version) { INVALID }
      }
    }
  }
}