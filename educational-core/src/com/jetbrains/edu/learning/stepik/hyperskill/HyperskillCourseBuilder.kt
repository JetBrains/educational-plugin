package com.jetbrains.edu.learning.stepik.hyperskill

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.edu.coursecreator.actions.NewStudyItemInfo
import com.jetbrains.edu.coursecreator.actions.NewStudyItemUiModel
import com.jetbrains.edu.coursecreator.actions.StudyItemType
import com.jetbrains.edu.coursecreator.ui.AdditionalPanel
import com.jetbrains.edu.learning.EduCourseBuilder
import com.jetbrains.edu.learning.LanguageSettings
import com.jetbrains.edu.learning.RefreshCause
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.Lesson
import com.jetbrains.edu.learning.courseFormat.TaskFile
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.newproject.CourseProjectGenerator
import com.jetbrains.edu.learning.stepik.hyperskill.courseFormat.HyperskillCourse

open class HyperskillCourseBuilder<T>(private val baseCourseBuilder: EduCourseBuilder<T>) : EduCourseBuilder<T> {
  override fun getCourseProjectGenerator(course: Course): CourseProjectGenerator<T>? {
    if (course !is HyperskillCourse) return null
    val generatorBase = baseCourseBuilder.getCourseProjectGenerator(course) ?: return null
    return HyperskillCourseProjectGenerator(generatorBase, this, course)
  }

  /**
   * We have to do this stuff because implementation by delegation still works unstable
   */
  override fun getLanguageSettings(): LanguageSettings<T> = baseCourseBuilder.getLanguageSettings()

  override fun showNewStudyItemUi(project: Project, course: Course, model: NewStudyItemUiModel,
                                  additionalPanels: List<AdditionalPanel>, studyItemCreator: (NewStudyItemInfo) -> Unit) =
    baseCourseBuilder.showNewStudyItemUi(project, course, model, additionalPanels, studyItemCreator)

  override fun createLessonContent(project: Project, lesson: Lesson, parentDirectory: VirtualFile): VirtualFile? =
    baseCourseBuilder.createLessonContent(project, lesson, parentDirectory)

  override fun createTaskContent(project: Project, task: Task, parentDirectory: VirtualFile): VirtualFile? =
    baseCourseBuilder.createTaskContent(project, task, parentDirectory)

  override fun refreshProject(project: Project, cause: RefreshCause) = baseCourseBuilder.refreshProject(project, cause)

  override fun refreshProject(project: Project, cause: RefreshCause, listener: EduCourseBuilder.ProjectRefreshListener?) =
    baseCourseBuilder.refreshProject(project, cause, listener)

  override fun createInitialLesson(project: Project, course: Course): Lesson? = baseCourseBuilder.createInitialLesson(project, course)

  override fun initNewTask(project: Project, lesson: Lesson, task: Task, info: NewStudyItemInfo) =
    baseCourseBuilder.initNewTask(project, lesson, task, info)

  override fun getTextForNewTask(taskFile: TaskFile, taskDir: VirtualFile, newTask: Task): String? =
    baseCourseBuilder.getTextForNewTask(taskFile, taskDir, newTask)

  override fun createDefaultTestFile(task: Task): TaskFile? = baseCourseBuilder.createDefaultTestFile(task)

  override val taskTemplateName: String?
    get() = baseCourseBuilder.taskTemplateName

  override val testTemplateName: String?
    get() = baseCourseBuilder.testTemplateName

  override fun validateItemName(name: String, itemType: StudyItemType): String? = baseCourseBuilder.validateItemName(name, itemType)
}