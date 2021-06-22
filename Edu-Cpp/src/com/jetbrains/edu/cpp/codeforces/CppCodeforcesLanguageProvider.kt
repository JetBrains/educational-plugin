package com.jetbrains.edu.cpp.codeforces

import com.jetbrains.edu.cpp.CppConfigurator
import com.jetbrains.edu.cpp.CppProjectSettings
import com.jetbrains.edu.cpp.addCMakeList
import com.jetbrains.edu.cpp.getDefaultName
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.codeforces.CodeforcesLanguageProvider
import com.jetbrains.edu.learning.configuration.EduConfigurator
import com.jetbrains.edu.learning.courseFormat.TaskFile
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import icons.EducationalCoreIcons
import javax.swing.Icon

class CppCodeforcesLanguageProvider : CodeforcesLanguageProvider {
  override val codeforcesLanguageNamings: List<String> =
    listOf("GNU C11", "GNU C++11", "GNU C++14", "MS C++", "Clang++17 Diagnostics", "GNU C++17", "MS C++ 2017")
  override val configurator: EduConfigurator<CppProjectSettings> = CppConfigurator()
  override val languageId: String = EduNames.CPP
  override val preferableCodeforcesLanguage: String = "GNU C++17"
  override val templateFileName: String = "codeforces.CPP main.cpp"
  override val templateName: String = "main.cpp"
  override val languageIcon: Icon = EducationalCoreIcons.CppLogo

  override fun getLanguageVersion(codeforcesLanguage: String): String? =
    when (codeforcesLanguage) {
      in listOf("GNU C11", "GNU C++11") -> "11"
      in listOf("GNU C++14", "MS C++") -> "14"
      in listOf("Clang++17 Diagnostics", "GNU C++17", "MS C++ 2017") -> "17"
      else -> null
    }

  override fun createTaskFiles(task: Task): List<TaskFile> {
    val moduleName = getDefaultName(task)
    task.customPresentableName = task.name
    task.name = moduleName
    task.addCMakeList(moduleName)
    return super.createTaskFiles(task)
  }
}