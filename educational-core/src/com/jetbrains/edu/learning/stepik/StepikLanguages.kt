package com.jetbrains.edu.learning.stepik

import com.jetbrains.edu.learning.EduNames

/**
 * Base on a class from intellij plugin from Stepik
 *
 * @see <a href="https://github.com/StepicOrg/intellij-plugins/blob/develop/stepik-union/src/main/java/org/stepik/core/SupportedLanguages.kt"> SupportedLanguages.kt</a>
 *
 */

private const val DEFAULT_VERSION = ""

enum class StepikLanguages(val id: String?, val version: String = DEFAULT_VERSION, val langName: String?, val isDefault: Boolean = true) {
  JAVA(EduNames.JAVA, "8", "java8"),
  JAVA11(EduNames.JAVA, "11", "java11", isDefault = false),
  KOTLIN(EduNames.KOTLIN, langName = "kotlin"),
  PYTHON(EduNames.PYTHON, "3", langName = "python3"),
  JAVASCRIPT(EduNames.JAVASCRIPT, langName = "javascript"),
  SCALA(EduNames.SCALA, langName = "scala"),
  CPP(EduNames.CPP, langName = "c++"),
  GO(EduNames.GO, langName = "go"),
  PLAINTEXT("TEXT", langName = "TEXT"), // added for tests
  INVALID(null, langName = null);


  override fun toString(): String = id ?: ""

  companion object {
    private val nameMap: Map<String?, StepikLanguages> by lazy {
      values().associateBy { it.langName }
    }

    private val titleMap: Map<Pair<String?, String>, StepikLanguages> by lazy {
      values().associateBy { it.id to it.version }
    }

    private val defaultTitleMap: Map<String?, StepikLanguages> by lazy {
      values().filter { it.isDefault }.associateBy { it.id }
    }

    @JvmStatic
    fun langOfName(lang: String): StepikLanguages = nameMap.getOrElse(lang, { INVALID })

    @JvmStatic
    fun langOfId(lang: String, version: String?): StepikLanguages {
      return if (version.isNullOrEmpty()) {
        defaultTitleMap.getOrElse(lang, { INVALID })
      }
      else {
        titleMap.getOrElse(lang to version, { INVALID })
      }
    }
  }
}