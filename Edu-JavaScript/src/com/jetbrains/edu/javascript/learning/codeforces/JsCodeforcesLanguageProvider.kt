package com.jetbrains.edu.javascript.learning.codeforces

import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.codeforces.CodeforcesLanguageProvider

class JsCodeforcesLanguageProvider : CodeforcesLanguageProvider {
  override val codeforcesLanguageNamings: List<String> = listOf("JavaScript", "Node.js")
  override val languageId: String = EduNames.JAVASCRIPT
}