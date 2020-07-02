package com.jetbrains.edu.go

import com.intellij.openapi.project.Project
import com.jetbrains.edu.coursecreator.StudyItemType
import com.jetbrains.edu.coursecreator.actions.NewStudyItemInfo
import com.jetbrains.edu.coursecreator.actions.TemplateFileInfo
import com.jetbrains.edu.go.GoConfigurator.Companion.GO_MOD
import com.jetbrains.edu.go.GoConfigurator.Companion.MAIN_GO
import com.jetbrains.edu.go.GoConfigurator.Companion.TASK_GO
import com.jetbrains.edu.go.GoConfigurator.Companion.TEST_GO
import com.jetbrains.edu.go.messages.EduGoBundle
import com.jetbrains.edu.learning.EduCourseBuilder
import com.jetbrains.edu.learning.EduNames.TEST
import com.jetbrains.edu.learning.LanguageSettings
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils.joinPaths
import com.jetbrains.edu.learning.newproject.CourseProjectGenerator

class GoCourseBuilder : EduCourseBuilder<GoProjectSettings> {

  override fun getCourseProjectGenerator(course: Course): CourseProjectGenerator<GoProjectSettings> =
    GoCourseProjectGenerator(this, course)

  override fun getLanguageSettings(): LanguageSettings<GoProjectSettings> = GoLanguageSettings()

  override fun getTestTaskTemplates(course: Course, info: NewStudyItemInfo, withSources: Boolean): List<TemplateFileInfo> {
    val templates = mutableListOf(TemplateFileInfo(TEST_GO, joinPaths(TEST, TEST_GO), false))
    if (withSources) {
      templates += TemplateFileInfo(EDU_TASK_TEMPLATE, TASK_GO, true)
      templates += TemplateFileInfo(EDU_MAIN_TEMPLATE, joinPaths("main", MAIN_GO), true)
      templates += TemplateFileInfo(GO_MOD, GO_MOD, false)
    }
    return templates
  }

  override fun getExecutableTaskTemplates(course: Course, info: NewStudyItemInfo, withSources: Boolean): List<TemplateFileInfo> {
    if (!withSources) return emptyList()
    return listOf(
      TemplateFileInfo(MAIN_GO, MAIN_GO, true),
      TemplateFileInfo(GO_MOD, GO_MOD, false)
    )
  }

  override fun extractInitializationParams(project: Project, info: NewStudyItemInfo): Map<String, String> {
    val moduleName = info.name.replace(" ", "_").toLowerCase()
    val moduleQuoted = "\"$moduleName\""
    return mapOf(
      "MODULE_NAME" to moduleName,
      "MAIN_IMPORTS" to createImportsSection(FMT, "task $moduleQuoted"),
      "TEST_IMPORTS" to createImportsSection(TESTING, "task $moduleQuoted")
    )
  }

  // https://golang.org/ref/spec#Import_declarations
  override fun validateItemName(name: String, itemType: StudyItemType): String? {
    return if (itemType == StudyItemType.TASK && name.contains(FORBIDDEN_SYMBOLS)) EduGoBundle.message("error.invalid.name") else null
  }

  /**
   * We have 2 types of imports in Go:
   * 1. "import/without/spaces"
   * 2. alias_without_spaces "import/without/spaces"
   * All imports should be sorted by value in quotes.
   * */
  private fun createImportsSection(vararg imports: String): String =
    imports.sortedBy { it.split(" ").last() }.joinToString("\n\t")

  companion object {
    val FORBIDDEN_SYMBOLS = """[!"#$%&'()*,:;<=>?\[\]^`{|}~]+""".toRegex()
    private const val TESTING = "\"testing\""
    private const val FMT = "\"fmt\""

    private const val EDU_TASK_TEMPLATE = "edu_task.go"
    private const val EDU_MAIN_TEMPLATE = "edu_main.go"
  }
}
