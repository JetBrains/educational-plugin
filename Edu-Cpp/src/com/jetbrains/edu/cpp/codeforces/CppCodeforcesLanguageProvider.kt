package com.jetbrains.edu.cpp.codeforces

import com.jetbrains.edu.EducationalCoreIcons
import com.jetbrains.edu.cpp.CppConfigurator
import com.jetbrains.edu.cpp.CppProjectSettings
import com.jetbrains.edu.cpp.addCMakeList
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.codeforces.CodeforcesLanguageProvider
import com.jetbrains.edu.learning.configuration.EduConfigurator
import com.jetbrains.edu.learning.courseFormat.TaskFile
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils
import javax.swing.Icon

class CppCodeforcesLanguageProvider : CodeforcesLanguageProvider {
  override val codeforcesLanguageNamings: List<String> =
    listOf(GNU_C_11, GNU_C_PLUS_11, GNU_C_PLUS_14, MS_C_PLUS, CLANG_PLUS_17, GNU_C_PLUS_17, MS_C_PLUS_2017)
  override val configurator: EduConfigurator<CppProjectSettings> = CppConfigurator()
  override val languageId: String = EduNames.CPP
  override val preferableCodeforcesLanguage: String = GNU_C_PLUS_17
  override val templateFileName: String = "codeforces.CPP main.cpp"
  override val displayTemplateName: String = "main.cpp"
  override val languageIcon: Icon = EducationalCoreIcons.CppLogo

  override fun getLanguageVersion(codeforcesLanguage: String): String? =
    when (codeforcesLanguage) {
      in listOf(GNU_C_11, GNU_C_PLUS_11) -> "11"
      in listOf(GNU_C_PLUS_14, MS_C_PLUS) -> "14"
      in listOf(CLANG_PLUS_17, GNU_C_PLUS_17, MS_C_PLUS_2017) -> "17"
      else -> null
    }

  override fun createTaskFiles(task: Task): List<TaskFile> {
    val moduleName = GeneratorUtils.getDefaultName(task)
    task.customPresentableName = task.name
    task.name = moduleName
    task.addCMakeList(moduleName)
    return super.createTaskFiles(task)
  }

  companion object {
    private const val GNU_C_11 = "GNU C11"
    private const val GNU_C_PLUS_11 = "GNU C++11"
    private const val GNU_C_PLUS_14 = "GNU C++14"
    private const val MS_C_PLUS = "MS C++"
    private const val CLANG_PLUS_17 = "Clang++17 Diagnostics"
    private const val GNU_C_PLUS_17 = "GNU C++17"
    private const val MS_C_PLUS_2017 = "MS C++ 2017"
  }
}