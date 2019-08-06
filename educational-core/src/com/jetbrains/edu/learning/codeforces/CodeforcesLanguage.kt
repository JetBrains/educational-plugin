package com.jetbrains.edu.learning.codeforces

import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.EduNames.PYTHON_2_VERSION
import com.jetbrains.edu.learning.EduNames.PYTHON_3_VERSION
import com.jetbrains.edu.learning.configuration.EduConfiguratorManager

enum class CodeforcesLanguage(val language: String, val id: String = language) {
  CPP11("${EduNames.CPP} 11", EduNames.CPP) {
    override fun toString(): String = "C++ 11"
    override val codeforcesLanguageNamings: List<String> = listOf("GNU C11", "GNU C++11")
  },
  CPP14("${EduNames.CPP} 14", EduNames.CPP) {
    override fun toString(): String = "C++ 14"
    override val codeforcesLanguageNamings: List<String> = listOf("GNU C++14", "MS C++")
  },
  CPP17("${EduNames.CPP} 17", EduNames.CPP) {
    override fun toString(): String = "C++ 17"
    override val codeforcesLanguageNamings: List<String> = listOf("Clang++17 Diagnostics", "GNU C++17", "MS C++ 2017")
  },
  JAVA8("${EduNames.JAVA} 8", EduNames.JAVA) {
    override fun toString(): String = "Java 8"
    override val codeforcesLanguageNamings: List<String> = listOf("Java 8")
  },
  JAVASCRIPT(EduNames.JAVASCRIPT) {
    override fun toString(): String = "JavaScript"
    override val codeforcesLanguageNamings: List<String> = listOf("JavaScript", "Node.js")
  },
  KOTLIN(EduNames.KOTLIN) {
    override fun toString(): String = "Kotlin"
    override val codeforcesLanguageNamings: List<String> = listOf("Kotlin")
  },
  PYTHON2("${EduNames.PYTHON} $PYTHON_2_VERSION", EduNames.PYTHON) {
    override fun toString(): String = "Python 2"
    override val codeforcesLanguageNamings: List<String> = listOf("Python 2", "PyPy 2")
  },
  PYTHON3("${EduNames.PYTHON} $PYTHON_3_VERSION", EduNames.PYTHON) {
    override fun toString(): String = "Python 3"
    override val codeforcesLanguageNamings: List<String> = listOf("Python 3", "PyPy 3")
  },
  RUST(EduNames.RUST) {
    override fun toString(): String = "Rust"
    override val codeforcesLanguageNamings: List<String> = listOf("Rust")
  },
  SCALA(EduNames.SCALA) {
    override fun toString(): String = "Scala"
    override val codeforcesLanguageNamings: List<String> = listOf("Scala")
  };

  abstract val codeforcesLanguageNamings: List<String>

  private fun isSupported(language: String): Boolean = language in codeforcesLanguageNamings

  companion object {
    fun isSupported(language: String): Boolean = availableLanguages.any { it.isSupported(language) }

    fun getLanguageId(language: String): String? {
      return values().firstOrNull { language in it.codeforcesLanguageNamings }?.language
    }

    private val availableLanguages: List<CodeforcesLanguage> by lazy {
      values().filter { it.id in EduConfiguratorManager.supportedEduLanguages }
    }
  }
}