package com.jetbrains.edu.kotlin.codeforces

import com.jetbrains.edu.EducationalCoreIcons
import com.jetbrains.edu.jvm.JdkProjectSettings
import com.jetbrains.edu.kotlin.KtConfigurator
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.codeforces.CodeforcesLanguageProvider
import com.jetbrains.edu.learning.configuration.EduConfigurator
import javax.swing.Icon

class KtCodeforcesLanguageProvider : CodeforcesLanguageProvider {
  override val codeforcesLanguageNamings: List<String> = listOf("Kotlin 1.4")
  override val configurator: EduConfigurator<JdkProjectSettings> = KtConfigurator()
  override val languageId: String = EduNames.KOTLIN
  override val templateFileName: String = "codeforces.Kotlin Main.kt"
  override val displayTemplateName: String = "Main.kt"
  override val languageIcon: Icon = EducationalCoreIcons.KotlinLogo
}