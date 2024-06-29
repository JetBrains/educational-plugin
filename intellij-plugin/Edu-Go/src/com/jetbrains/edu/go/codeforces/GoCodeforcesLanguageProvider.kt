package com.jetbrains.edu.go.codeforces

import com.jetbrains.edu.EducationalCoreIcons
import com.jetbrains.edu.go.GoConfigurator
import com.jetbrains.edu.go.GoConfigurator.Companion.GO_MOD
import com.jetbrains.edu.go.GoConfigurator.Companion.MAIN_GO
import com.jetbrains.edu.go.GoCourseBuilder.Companion.FORBIDDEN_SYMBOLS
import com.jetbrains.edu.learning.codeforces.CodeforcesLanguageProvider
import com.jetbrains.edu.learning.courseFormat.EduFormatNames.GO
import com.jetbrains.edu.learning.courseFormat.TaskFile
import com.jetbrains.edu.learning.courseFormat.ext.sourceDir
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils
import javax.swing.Icon

class GoCodeforcesLanguageProvider  : CodeforcesLanguageProvider() {
  override val configurator: GoConfigurator = GoConfigurator()
  override val languageId: String = GO
  override val templateFileName: String = "codeforces.Go main.go"
  override val displayTemplateName: String = "main.go"
  override val languageIcon: Icon = EducationalCoreIcons.Language.GoLogo

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