package com.jetbrains.edu.cpp.codeforces

import com.jetbrains.edu.EducationalCoreIcons
import com.jetbrains.edu.cpp.CppConfigurator
import com.jetbrains.edu.cpp.CppProjectSettings
import com.jetbrains.edu.cpp.addCMakeList
import com.jetbrains.edu.learning.codeforces.CodeforcesLanguageProvider
import com.jetbrains.edu.learning.configuration.EduConfigurator
import com.jetbrains.edu.learning.courseFormat.EduFormatNames.CPP
import com.jetbrains.edu.learning.courseFormat.TaskFile
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils
import javax.swing.Icon

class CppCodeforcesLanguageProvider : CodeforcesLanguageProvider() {
  override val configurator: EduConfigurator<CppProjectSettings> = CppConfigurator()
  override val languageId: String = CPP
  override val templateFileName: String = "codeforces.CPP main.cpp"
  override val displayTemplateName: String = "main.cpp"
  override val languageIcon: Icon = EducationalCoreIcons.CppLogo

  override fun createTaskFiles(task: Task): List<TaskFile> {
    val moduleName = GeneratorUtils.getDefaultName(task)
    task.customPresentableName = task.name
    task.name = moduleName
    task.addCMakeList(moduleName)
    return super.createTaskFiles(task)
  }
}