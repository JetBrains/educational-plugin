package com.jetbrains.edu.cpp.codeforces

import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.codeforces.CodeforcesLanguageProvider

class CppCodeforcesLanguageProvider : CodeforcesLanguageProvider {
  override val codeforcesLanguageNamings: List<String> =
    listOf("GNU C11", "GNU C++11", "GNU C++14", "MS C++", "Clang++17 Diagnostics", "GNU C++17", "MS C++ 2017")
  override val languageId: String = EduNames.CPP

  override fun getLanguageVersion(codeforcesLanguage: String): String? =
    when (codeforcesLanguage) {
      in listOf("GNU C11", "GNU C++11") -> "11"
      in listOf("GNU C++14", "MS C++") -> "14"
      in listOf("Clang++17 Diagnostics", "GNU C++17", "MS C++ 2017") -> "17"
      else -> null
    }
}