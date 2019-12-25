package com.jetbrains.edu.java.codeforces

import com.jetbrains.edu.java.JConfigurator
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.codeforces.CodeforcesLanguageProvider
import com.jetbrains.edu.learning.configuration.EduConfigurator

class JCodeforcesLanguageProvider : CodeforcesLanguageProvider {
  override val codeforcesLanguageNamings: List<String> = listOf("Java 8")
  override val configurator: EduConfigurator<*> = JConfigurator()
  override val languageId: String = EduNames.JAVA
  override val templateFileName: String = "codeforces.Main.java"

  override fun getLanguageVersion(codeforcesLanguage: String): String? = "8"
}