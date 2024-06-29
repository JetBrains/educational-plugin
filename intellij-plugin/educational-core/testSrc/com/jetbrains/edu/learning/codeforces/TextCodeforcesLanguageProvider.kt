package com.jetbrains.edu.learning.codeforces

import com.jetbrains.edu.EducationalCoreIcons
import com.jetbrains.edu.learning.configuration.PlainTextConfigurator
import javax.swing.Icon

class TextCodeforcesLanguageProvider: CodeforcesLanguageProvider() {
  override val configurator: PlainTextConfigurator = PlainTextConfigurator()
  override val languageId: String = com.intellij.openapi.fileTypes.PlainTextLanguage.INSTANCE.id
  override val templateFileName: String = "codeforces.Text Main.txt"
  override val displayTemplateName: String = "Main.txt"
  override val languageIcon: Icon = EducationalCoreIcons.Platform.Tab.Codeforces
}