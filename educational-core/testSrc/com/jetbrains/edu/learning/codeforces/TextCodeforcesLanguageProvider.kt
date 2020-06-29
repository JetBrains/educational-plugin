package com.jetbrains.edu.learning.codeforces

import com.jetbrains.edu.learning.configuration.PlainTextConfigurator

class TextCodeforcesLanguageProvider: CodeforcesLanguageProvider {
  override val codeforcesLanguageNamings: List<String> = listOf("TEXT")
  override val configurator: PlainTextConfigurator = PlainTextConfigurator()
  override val languageId: String = com.intellij.openapi.fileTypes.PlainTextLanguage.INSTANCE.id
  override val templateFileName: String = "codeforces.Main.txt"
}