package com.jetbrains.edu.scala.codeforces

import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.codeforces.CodeforcesLanguageProvider

class ScalaCodeforcesLanguageProvider : CodeforcesLanguageProvider {
  override val codeforcesLanguageNamings: List<String> = listOf("Scala")
  override val languageId: String = EduNames.SCALA
}