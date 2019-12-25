package com.jetbrains.edu.kotlin.codeforces

import com.jetbrains.edu.kotlin.KtConfigurator
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.codeforces.CodeforcesLanguageProvider
import com.jetbrains.edu.learning.configuration.EduConfigurator

class KtCodeforcesLanguageProvider : CodeforcesLanguageProvider {
  override val codeforcesLanguageNamings: List<String> = listOf("Kotlin")
  override val configurator: EduConfigurator<*> = KtConfigurator()
  override val languageId: String = EduNames.KOTLIN
  override val templateFileName: String = "codeforces.Main.kt"
}