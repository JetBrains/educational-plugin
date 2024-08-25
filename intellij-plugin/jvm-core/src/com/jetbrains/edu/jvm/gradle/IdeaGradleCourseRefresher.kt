package com.jetbrains.edu.jvm.gradle

import com.intellij.ide.projectView.ProjectView
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.externalSystem.importing.ImportSpecBuilder
import com.intellij.openapi.externalSystem.model.ExternalSystemDataKeys
import com.intellij.openapi.externalSystem.service.execution.ProgressExecutionMode
import com.intellij.openapi.externalSystem.util.ExternalSystemUtil
import com.intellij.openapi.project.Project
import com.intellij.util.PlatformUtils
import com.jetbrains.edu.learning.RefreshCause
import com.jetbrains.edu.learning.isUnitTestMode
import com.jetbrains.edu.learning.projectView.CourseViewPane
import org.jetbrains.plugins.gradle.util.GradleConstants

class IdeaGradleCourseRefresher : GradleCourseRefresher {
  override fun isAvailable(): Boolean = PlatformUtils.isIntelliJ()

  override fun refresh(project: Project, cause: RefreshCause) {
    val projectBasePath = project.basePath ?: return

    val builder = ImportSpecBuilder(project, GradleConstants.SYSTEM_ID)
      .use(ProgressExecutionMode.IN_BACKGROUND_ASYNC)
      .dontReportRefreshErrors()

    // Build toolwindow will be opened if `ExternalSystemDataKeys.NEWLY_IMPORTED_PROJECT` is true while sync
    project.putUserData(ExternalSystemDataKeys.NEWLY_IMPORTED_PROJECT, null)
    ExternalSystemUtil.refreshProject(projectBasePath, builder.build())
    if (!isUnitTestMode) {
      ExternalSystemUtil.invokeLater(project, ModalityState.nonModal()) {
        ProjectView.getInstance(project).changeViewCB(CourseViewPane.ID, null)
      }
    }
  }
}
