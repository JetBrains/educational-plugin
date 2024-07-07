package com.jetbrains.edu.javascript.learning.codeforces

import com.jetbrains.edu.EducationalCoreIcons
import com.jetbrains.edu.javascript.learning.JsConfigurator
import com.jetbrains.edu.javascript.learning.JsNewProjectSettings
import com.jetbrains.edu.learning.codeforces.CodeforcesLanguageProvider
import com.jetbrains.edu.learning.configuration.EduConfigurator
import com.jetbrains.edu.learning.courseFormat.EduFormatNames.JAVASCRIPT
import javax.swing.Icon

class JsCodeforcesLanguageProvider : CodeforcesLanguageProvider() {
  override val configurator: EduConfigurator<JsNewProjectSettings> = JsConfigurator()
  override val languageId: String = JAVASCRIPT
  override val templateFileName: String = "codeforces.JS main.js"
  override val displayTemplateName: String = "main.js"
  override val languageIcon: Icon = EducationalCoreIcons.Language.JavaScript
}