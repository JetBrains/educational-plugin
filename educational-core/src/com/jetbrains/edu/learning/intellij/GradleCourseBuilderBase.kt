package com.jetbrains.edu.learning.intellij

import com.intellij.ide.projectView.ProjectView
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.externalSystem.service.execution.ProgressExecutionMode
import com.intellij.openapi.externalSystem.util.ExternalSystemUtil
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.EduCourseBuilder
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.intellij.generation.GradleCourseProjectGenerator
import com.jetbrains.edu.learning.projectView.CourseViewPane
import org.jetbrains.plugins.gradle.util.GradleConstants

abstract class GradleCourseBuilderBase : EduCourseBuilder<JdkProjectSettings> {

  abstract val buildGradleTemplateName: String
  abstract val subtaskTestTemplateName: String

  override fun refreshProject(project: Project) {
    ExternalSystemUtil.refreshProjects(project, GradleConstants.SYSTEM_ID, true, ProgressExecutionMode.MODAL_SYNC)
    ExternalSystemUtil.invokeLater(project, ModalityState.NON_MODAL) {
      ProjectView.getInstance(project).changeViewCB(CourseViewPane.ID, null)
    }
  }

  override fun getLanguageSettings(): EduCourseBuilder.LanguageSettings<JdkProjectSettings> = JdkLanguageSettings()

  override fun getCourseProjectGenerator(course: Course): GradleCourseProjectGenerator =
    GradleCourseProjectGenerator(this, course)
}
