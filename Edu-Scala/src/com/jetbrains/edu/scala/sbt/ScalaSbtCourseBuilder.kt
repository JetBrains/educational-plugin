package com.jetbrains.edu.scala.sbt

import com.intellij.ide.projectView.ProjectView
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.externalSystem.importing.ImportSpecBuilder
import com.intellij.openapi.externalSystem.model.ExternalSystemDataKeys
import com.intellij.openapi.externalSystem.service.execution.ProgressExecutionMode
import com.intellij.openapi.externalSystem.util.ExternalSystemUtil
import com.intellij.openapi.project.Project
import com.jetbrains.edu.coursecreator.actions.NewStudyItemInfo
import com.jetbrains.edu.coursecreator.actions.TemplateFileInfo
import com.jetbrains.edu.jvm.JdkLanguageSettings
import com.jetbrains.edu.jvm.JdkProjectSettings
import com.jetbrains.edu.learning.EduCourseBuilder
import com.jetbrains.edu.learning.LanguageSettings
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.Lesson
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.isUnitTestMode
import com.jetbrains.edu.learning.newproject.CourseProjectGenerator
import com.jetbrains.edu.learning.projectView.CourseViewPane
import org.jetbrains.sbt.project.SbtProjectSystem

class ScalaSbtCourseBuilder : EduCourseBuilder<JdkProjectSettings> {

  override val taskTemplateName: String = ScalaSbtConfigurator.TASK_SCALA
  override val testTemplateName: String = ScalaSbtConfigurator.TEST_SCALA

  override fun getLanguageSettings(): LanguageSettings<JdkProjectSettings> = JdkLanguageSettings()

  override fun getCourseProjectGenerator(course: Course): CourseProjectGenerator<JdkProjectSettings>? {
    return ScalaSbtCourseProjectGenerator(this, course)
  }

  override fun initNewTask(project: Project, lesson: Lesson, task: Task, info: NewStudyItemInfo) {
    super.initNewTask(project, lesson, task, info)
    val templateInfo = TemplateFileInfo(TASK_BUILD_SBT, BUILD_SBT, false)
    val taskFile = templateInfo.toTaskFile(mapOf("TASK_NAME" to info.name.replace(" ", "-"))) ?: return
    task.addTaskFile(taskFile)
  }

  override fun refreshProject(project: Project) {
    val projectBasePath = project.basePath ?: return
    val builder = ImportSpecBuilder(project, SbtProjectSystem.Id()).use(ProgressExecutionMode.IN_BACKGROUND_ASYNC).dontReportRefreshErrors()
    builder.useDefaultCallback()

    // Build toolwindow will be opened if `ExternalSystemDataKeys.NEWLY_IMPORTED_PROJECT` is true while sync
    project.putUserData(ExternalSystemDataKeys.NEWLY_IMPORTED_PROJECT, null)
    ExternalSystemUtil.refreshProject(projectBasePath, builder.build())
    if (!isUnitTestMode) {
      ExternalSystemUtil.invokeLater(project, ModalityState.NON_MODAL) {
        ProjectView.getInstance(project).changeViewCB(CourseViewPane.ID, null)
      }
    }
  }

  companion object {
    private const val TASK_BUILD_SBT: String = "task-build.sbt"
    const val BUILD_SBT: String = "build.sbt"
  }
}
