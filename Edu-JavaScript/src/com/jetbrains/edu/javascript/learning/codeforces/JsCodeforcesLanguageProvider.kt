package com.jetbrains.edu.javascript.learning.codeforces

import com.jetbrains.edu.javascript.learning.JsConfigurator
import com.jetbrains.edu.javascript.learning.JsNewProjectSettings
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.codeforces.CodeforcesLanguageProvider
import com.jetbrains.edu.learning.configuration.EduConfigurator
import icons.EducationalCoreIcons
import javax.swing.Icon

class JsCodeforcesLanguageProvider : CodeforcesLanguageProvider {
  override val codeforcesLanguageNamings: List<String> = listOf("JavaScript", "Node.js")
  override val configurator: EduConfigurator<JsNewProjectSettings> = JsConfigurator()
  override val languageId: String = EduNames.JAVASCRIPT
  override val preferableCodeforcesLanguage: String = "JavaScript"
  override val templateFileName: String = "codeforces.JS main.js"
  override val templateName: String = "main.js"
  override val languageIcon: Icon = EducationalCoreIcons.JsLogo
}