package com.jetbrains.edu.scala.codeforces

import com.jetbrains.edu.EducationalCoreIcons
import com.jetbrains.edu.jvm.JdkProjectSettings
import com.jetbrains.edu.learning.codeforces.CodeforcesLanguageProvider
import com.jetbrains.edu.learning.configuration.EduConfigurator
import com.jetbrains.edu.learning.courseFormat.EduFormatNames.SCALA
import com.jetbrains.edu.scala.gradle.ScalaGradleConfigurator
import javax.swing.Icon

class ScalaCodeforcesLanguageProvider : CodeforcesLanguageProvider() {
  override val configurator: EduConfigurator<JdkProjectSettings> = ScalaGradleConfigurator()
  override val languageId: String = SCALA
  override val templateFileName: String = "codeforces.Scala Main.scala"
  override val displayTemplateName: String = "Main.scala"
  override val languageIcon: Icon = EducationalCoreIcons.Language.ScalaLogo
}