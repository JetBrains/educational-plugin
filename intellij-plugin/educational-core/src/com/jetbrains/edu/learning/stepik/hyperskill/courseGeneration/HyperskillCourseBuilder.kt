package com.jetbrains.edu.learning.stepik.hyperskill.courseGeneration

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.edu.coursecreator.StudyItemType
import com.jetbrains.edu.coursecreator.actions.TemplateFileInfo
import com.jetbrains.edu.coursecreator.actions.studyItem.NewStudyItemInfo
import com.jetbrains.edu.coursecreator.actions.studyItem.NewStudyItemUiModel
import com.jetbrains.edu.learning.*
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.Lesson
import com.jetbrains.edu.learning.courseFormat.StudyItem
import com.jetbrains.edu.learning.courseFormat.TaskFile
import com.jetbrains.edu.learning.courseFormat.hyperskill.HyperskillCourse
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.newproject.CourseProjectGenerator
import com.jetbrains.edu.learning.newproject.EduProjectSettings

open class HyperskillCourseBuilder<T : EduProjectSettings>(private val baseCourseBuilder: EduCourseBuilder<T>) : EduCourseBuilder<T> {
  override fun getCourseProjectGenerator(course: Course): CourseProjectGenerator<T>? {
    if (course !is HyperskillCourse) return null
    val generatorBase = baseCourseBuilder.getCourseProjectGenerator(course) ?: return null
    return HyperskillCourseProjectGenerator(generatorBase, this, course)
  }

  /**
   * We have to do this stuff because implementation by delegation still works unstable
   */
  override fun getLanguageSettings(): LanguageSettings<T> = baseCourseBuilder.getLanguageSettings()

  override fun getDefaultSettings(): Result<T, String> = baseCourseBuilder.getDefaultSettings()

  override fun getSupportedLanguageVersions(): List<String> = baseCourseBuilder.getSupportedLanguageVersions()

  override fun showNewStudyItemUi(
    project: Project,
    course: Course,
    model: NewStudyItemUiModel,
    studyItemCreator: (NewStudyItemInfo) -> Unit
  ) = baseCourseBuilder.showNewStudyItemUi(project, course, model, studyItemCreator)

  override fun onStudyItemCreation(project: Project, item: StudyItem) {
    baseCourseBuilder.onStudyItemCreation(project, item)
  }

  override fun refreshProject(project: Project, cause: RefreshCause) = baseCourseBuilder.refreshProject(project, cause)

  override fun createInitialLesson(holder: CourseInfoHolder<Course>): Lesson? = baseCourseBuilder.createInitialLesson(holder)

  override fun getTestTaskTemplates(course: Course, info: NewStudyItemInfo, withSources: Boolean): List<TemplateFileInfo> {
    return baseCourseBuilder.getTestTaskTemplates(course, info, withSources)
  }

  override fun getExecutableTaskTemplates(course: Course, info: NewStudyItemInfo, withSources: Boolean): List<TemplateFileInfo> {
    return baseCourseBuilder.getExecutableTaskTemplates(course, info, withSources)
  }

  override fun getDefaultTaskTemplates(
    course: Course,
    info: NewStudyItemInfo,
    withSources: Boolean,
    withTests: Boolean
  ): List<TemplateFileInfo> {
    return baseCourseBuilder.getDefaultTaskTemplates(course, info, withSources, withTests)
  }

  override fun extractInitializationParams(info: NewStudyItemInfo): Map<String, String> {
    return baseCourseBuilder.extractInitializationParams(info)
  }

  override fun initNewTask(course: Course, task: Task, info: NewStudyItemInfo, withSources: Boolean) =
    baseCourseBuilder.initNewTask(course, task, info, withSources)

  override fun getTextForNewTask(taskFile: TaskFile, taskDir: VirtualFile, newTask: Task): String? =
    baseCourseBuilder.getTextForNewTask(taskFile, taskDir, newTask)

  override fun taskTemplateName(course: Course): String? = baseCourseBuilder.taskTemplateName(course)

  override fun mainTemplateName(course: Course): String? = baseCourseBuilder.mainTemplateName(course)

  override fun testTemplateName(course: Course): String? = baseCourseBuilder.testTemplateName(course)

  override fun validateItemName(project: Project, name: String, itemType: StudyItemType): String? =
    baseCourseBuilder.validateItemName(project, name, itemType)
}
