package com.jetbrains.edu.python.learning.codeforces

import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.EduNames.PYTHON_2_VERSION
import com.jetbrains.edu.learning.EduNames.PYTHON_3_VERSION
import com.jetbrains.edu.learning.codeforces.CodeforcesLanguageProvider

class PyCodeforcesLanguageProvider : CodeforcesLanguageProvider {
  override val codeforcesLanguageNamings: List<String> = listOf("Python 2", "PyPy 2", "Python 3", "PyPy 3")
  override val languageId: String = EduNames.PYTHON

  override fun getLanguageVersion(codeforcesLanguage: String): String? =
    when (codeforcesLanguage.split(" ").last()) {
      "2" -> PYTHON_2_VERSION
      "3" -> PYTHON_3_VERSION
      else -> null
    }
}