package com.jetbrains.edu.scala.sbt

import com.intellij.ide.projectView.ProjectView
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.externalSystem.importing.ImportSpecBuilder
import com.intellij.openapi.externalSystem.model.ExternalSystemDataKeys
import com.intellij.openapi.externalSystem.service.execution.ProgressExecutionMode
import com.intellij.openapi.externalSystem.util.ExternalSystemUtil
import com.intellij.openapi.project.Project
import com.jetbrains.edu.jvm.JdkLanguageSettings
import com.jetbrains.edu.jvm.JdkProjectSettings
import com.jetbrains.edu.learning.EduCourseBuilder
import com.jetbrains.edu.learning.LanguageSettings
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.isUnitTestMode
import com.jetbrains.edu.learning.newproject.CourseProjectGenerator
import com.jetbrains.edu.learning.projectView.CourseViewPane
import org.jetbrains.sbt.project.SbtProjectSystem

class ScalaSbtCourseBuilder : EduCourseBuilder<JdkProjectSettings> {

  override fun getLanguageSettings(): LanguageSettings<JdkProjectSettings> = JdkLanguageSettings()
  override fun getTaskTemplateName(): String = ScalaSbtConfigurator.TASK_SCALA
  override fun getTestTemplateName(): String = ScalaSbtConfigurator.TEST_SCALA

  override fun getCourseProjectGenerator(course: Course): CourseProjectGenerator<JdkProjectSettings>? {
    return ScalaSbtCourseProjectGenerator(this, course)
  }

  override fun refreshProject(project: Project) {
    val projectBasePath = project.basePath ?: return
    val builder = ImportSpecBuilder(project, SbtProjectSystem.Id())
      .use(ProgressExecutionMode.IN_BACKGROUND_ASYNC)
      .dontReportRefreshErrors()
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
}
