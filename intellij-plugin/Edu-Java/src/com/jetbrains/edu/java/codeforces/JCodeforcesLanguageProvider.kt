package com.jetbrains.edu.java.codeforces

import com.jetbrains.edu.EducationalCoreIcons
import com.jetbrains.edu.java.JConfigurator
import com.jetbrains.edu.jvm.JdkProjectSettings
import com.jetbrains.edu.learning.codeforces.CodeforcesLanguageProvider
import com.jetbrains.edu.learning.configuration.EduConfigurator
import com.jetbrains.edu.learning.courseFormat.EduFormatNames.JAVA
import javax.swing.Icon

class JCodeforcesLanguageProvider : CodeforcesLanguageProvider() {
  override val configurator: EduConfigurator<JdkProjectSettings> = JConfigurator()
  override val languageId: String = JAVA
  override val templateFileName: String = "codeforces.Java Main.java"
  override val displayTemplateName: String = "Main.java"
  override val languageIcon: Icon = EducationalCoreIcons.Language.JavaLogo
}