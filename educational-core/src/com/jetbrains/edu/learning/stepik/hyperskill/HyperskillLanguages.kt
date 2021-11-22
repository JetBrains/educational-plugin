package com.jetbrains.edu.learning.stepik.hyperskill

import com.jetbrains.edu.learning.EduNames


enum class HyperskillLanguages(val id: String?, val langName: String?) {
  GO(EduNames.GO, "go"),
  JAVASCRIPT(EduNames.JAVASCRIPT, "javascript"),
  JAVA(EduNames.JAVA, "java11"),
  KOTLIN(EduNames.KOTLIN, "kotlin"),
  PYTHON(EduNames.PYTHON, "python3"),
  SCALA(EduNames.SCALA, "scala"),
  PLAINTEXT("TEXT", "TEXT"),
  INVALID(null, null);

  companion object {
    private val titleMap: Map<String?, HyperskillLanguages> by lazy {
      values().associateBy { it.id }
    }

    fun langOfId(lang: String) = titleMap.getOrElse(lang) { INVALID }
  }
}