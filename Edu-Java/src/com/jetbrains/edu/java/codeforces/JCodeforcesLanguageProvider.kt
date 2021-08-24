package com.jetbrains.edu.java.codeforces

import com.intellij.openapi.projectRoots.JavaSdkVersion.JDK_11
import com.intellij.openapi.projectRoots.JavaSdkVersion.JDK_1_8
import com.jetbrains.edu.EducationalCoreIcons
import com.jetbrains.edu.java.JConfigurator
import com.jetbrains.edu.jvm.JdkProjectSettings
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.codeforces.CodeforcesLanguageProvider
import com.jetbrains.edu.learning.configuration.EduConfigurator
import javax.swing.Icon

class JCodeforcesLanguageProvider : CodeforcesLanguageProvider {
  override val codeforcesLanguageNamings: List<String> = listOf("Java 8", "Java 11")
  override val configurator: EduConfigurator<JdkProjectSettings> = JConfigurator()
  override val languageId: String = EduNames.JAVA
  override val preferableCodeforcesLanguage: String = "Java 8"
  override val templateFileName: String = "codeforces.Java Main.java"
  override val displayTemplateName: String = "Main.java"
  override val languageIcon: Icon = EducationalCoreIcons.JavaLogo

  override fun getLanguageVersion(codeforcesLanguage: String): String? {
    val version = codeforcesLanguage.split(" ").last()
    return if (version in listOf(JDK_1_8, JDK_11).map { it.description }) version else null
  }
}