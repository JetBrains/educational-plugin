package com.jetbrains.edu.kotlin.codeforces

import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.codeforces.CodeforcesLanguageProvider

class KtCodeforcesLanguageProvider : CodeforcesLanguageProvider {
  override val codeforcesLanguageNamings: List<String> = listOf("Kotlin")
  override val languageId: String = EduNames.KOTLIN
}