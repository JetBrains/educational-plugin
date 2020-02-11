package com.jetbrains.edu.scala.codeforces

import com.jetbrains.edu.jvm.JdkProjectSettings
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.codeforces.CodeforcesLanguageProvider
import com.jetbrains.edu.learning.configuration.EduConfigurator
import com.jetbrains.edu.scala.gradle.ScalaGradleConfigurator

class ScalaCodeforcesLanguageProvider : CodeforcesLanguageProvider {
  override val codeforcesLanguageNamings: List<String> = listOf("Scala")
  override val configurator: EduConfigurator<JdkProjectSettings> = ScalaGradleConfigurator()
  override val languageId: String = EduNames.SCALA
  override val templateFileName: String = "codeforces.Main.scala"
}