package com.jetbrains.edu.go

import com.intellij.openapi.project.Project
import com.jetbrains.edu.coursecreator.actions.NewStudyItemInfo
import com.jetbrains.edu.coursecreator.actions.StudyItemType
import com.jetbrains.edu.coursecreator.actions.TemplateFileInfo
import com.jetbrains.edu.go.GoConfigurator.Companion.GO_MOD
import com.jetbrains.edu.go.GoConfigurator.Companion.MAIN_GO
import com.jetbrains.edu.go.GoConfigurator.Companion.TASK_GO
import com.jetbrains.edu.go.GoConfigurator.Companion.TEST_GO
import com.jetbrains.edu.learning.EduCourseBuilder
import com.jetbrains.edu.learning.EduNames.TEST
import com.jetbrains.edu.learning.LanguageSettings
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.Lesson
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils.joinPaths
import com.jetbrains.edu.learning.newproject.CourseProjectGenerator

class GoCourseBuilder : EduCourseBuilder<GoProjectSettings> {
  override val taskTemplateName: String = TASK_GO
  override val testTemplateName: String = TEST_GO

  override fun getCourseProjectGenerator(course: Course): CourseProjectGenerator<GoProjectSettings> =
    GoCourseProjectGenerator(this, course)

  override fun getLanguageSettings(): LanguageSettings<GoProjectSettings> = GoLanguageSettings()

  override fun initNewTask(project: Project, lesson: Lesson, task: Task, info: NewStudyItemInfo) {
    if (task.taskFiles.isNotEmpty()) return
    val params = mapOf("MODULE_NAME" to task.name.replace(" ", "_").toLowerCase())
    for (templateInfo in defaultTaskFiles) {
      val taskFile = templateInfo.toTaskFile(params) ?: continue
      task.addTaskFile(taskFile)
    }
  }

  // https://golang.org/ref/spec#Import_declarations
  override fun validateItemName(name: String, itemType: StudyItemType): String? {
    val forbiddenSymbols = Regex.escape("!\"#\$%&'()*,:;<=>?[\\]^`{|}~")
    val badRegex = "[${forbiddenSymbols}]+".toRegex()
    return if (itemType == StudyItemType.TASK && name.contains(badRegex)) "Name contains forbidden symbols" else null
  }

  private val defaultTaskFiles: List<TemplateFileInfo>
    get() = listOf(
      TemplateFileInfo(TASK_GO, TASK_GO, true),
      TemplateFileInfo(MAIN_GO, joinPaths("main", MAIN_GO), true),
      TemplateFileInfo(TEST_GO, joinPaths(TEST, TEST_GO), false),
      TemplateFileInfo(GO_MOD, GO_MOD, false)
    )
}
