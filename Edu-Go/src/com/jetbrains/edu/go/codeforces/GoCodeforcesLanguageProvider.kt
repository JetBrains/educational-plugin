package com.jetbrains.edu.go.codeforces

import com.jetbrains.edu.go.GoConfigurator.Companion.GO_MOD
import com.jetbrains.edu.go.GoConfigurator.Companion.MAIN_GO
import com.jetbrains.edu.go.GoCourseBuilder.Companion.FORBIDDEN_SYMBOLS
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.codeforces.CodeforcesLanguageProvider
import com.jetbrains.edu.learning.courseFormat.TaskFile
import com.jetbrains.edu.learning.courseFormat.ext.sourceDir
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils
import icons.EducationalCoreIcons
import javax.swing.Icon

class GoCodeforcesLanguageProvider  : CodeforcesLanguageProvider {
  override val codeforcesLanguageNamings: List<String> = listOf("Go")
  override val languageId: String = EduNames.GO
  override val templateFileName: String = "codeforces.Go main.go"
  override val displayTemplateName: String = "main.go"
  override val languageIcon: Icon = EducationalCoreIcons.GoLogo

  override fun createTaskFiles(task: Task): List<TaskFile> {
    val mainFileTemplate = GeneratorUtils.getJ2eeTemplateText(templateFileName)
    val moduleName = task.name.replace(" ", "_").replace(FORBIDDEN_SYMBOLS, "")
    val goModFileTemplate = GeneratorUtils.getInternalTemplateText(GO_MOD, mapOf("MODULE_NAME" to moduleName))

    return listOf(
      TaskFile(GeneratorUtils.joinPaths(task.sourceDir, MAIN_GO), mainFileTemplate),
      TaskFile(GeneratorUtils.joinPaths(task.sourceDir, GO_MOD), goModFileTemplate).apply { isVisible = false }
    )
  }
}