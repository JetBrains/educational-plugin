package com.jetbrains.edu.cpp

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.VfsUtil
import com.jetbrains.cidr.cpp.cmake.workspace.CMakeWorkspace
import com.jetbrains.edu.coursecreator.actions.NewStudyItemInfo
import com.jetbrains.edu.coursecreator.actions.NewStudyItemUiModel
import com.jetbrains.edu.coursecreator.ui.AdditionalPanel
import com.jetbrains.edu.coursecreator.ui.showNewStudyItemDialog
import com.jetbrains.edu.learning.EduCourseBuilder
import com.jetbrains.edu.learning.LanguageSettings
import com.jetbrains.edu.learning.courseDir
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.Lesson
import com.jetbrains.edu.learning.courseFormat.TaskFile
import com.jetbrains.edu.learning.courseFormat.ext.sourceDir
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils
import com.jetbrains.edu.learning.newproject.CourseProjectGenerator

class CppCourseBuilder : EduCourseBuilder<CppProjectSettings> {
  override fun getCourseProjectGenerator(course: Course): CourseProjectGenerator<CppProjectSettings>? =
    CppCourseProjectGenerator(this, course)

  override fun getTaskTemplateName(): String = CppConfigurator.TASK_CPP
  override fun getTestTemplateName(): String = CppConfigurator.TEST_CPP

  override fun getLanguageSettings(): LanguageSettings<CppProjectSettings> = CppLanguageSettings()

  override fun initNewTask(project: Project, lesson: Lesson, task: Task, info: NewStudyItemInfo) {
    super.initNewTask(project, lesson, task, info)

    val cMakeProjectName = getCMakeProjectUniqueName(task) { FileUtil.sanitizeFileName(it.name, true) }
    addCMakeList(task, cMakeProjectName, languageSettings.settings.languageStandard)

    val mainName = GeneratorUtils.joinPaths(task.sourceDir, EDU_RUN_CPP)
    val mainText = GeneratorUtils.getInternalTemplateText(EDU_RUN_CPP)
    task.addTaskFile(TaskFile(mainName, mainText))
  }

  override fun showNewStudyItemUi(
    project: Project,
    model: NewStudyItemUiModel,
    additionalPanels: MutableList<AdditionalPanel>
  ): NewStudyItemInfo? {
    return showNewStudyItemDialog(project, model, additionalPanels, ::CppNewTaskDialog)
  }

  override fun refreshProject(project: Project) {
    // if it is a new project it will be initialized, else it will be reloaded only.
    CMakeWorkspace.getInstance(project).selectProjectDir(VfsUtil.virtualToIoFile(project.courseDir))
  }

  companion object {
    private const val EDU_RUN_CPP = "run.cpp"
  }
}