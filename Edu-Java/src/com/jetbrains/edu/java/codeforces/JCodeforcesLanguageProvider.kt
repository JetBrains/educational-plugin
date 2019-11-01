package com.jetbrains.edu.java.codeforces

import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.codeforces.CodeforcesLanguageProvider

class JCodeforcesLanguageProvider : CodeforcesLanguageProvider {
  override val codeforcesLanguageNamings: List<String> = listOf("Java 8")
  override val languageId: String = EduNames.JAVA

  override fun getLanguageVersion(codeforcesLanguage: String): String? = "8"
}