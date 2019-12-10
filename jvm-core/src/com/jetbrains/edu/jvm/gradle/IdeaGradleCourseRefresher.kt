package com.jetbrains.edu.jvm.gradle

import com.intellij.ide.projectView.ProjectView
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.externalSystem.importing.ImportSpecBuilder
import com.intellij.openapi.externalSystem.model.DataNode
import com.intellij.openapi.externalSystem.model.ExternalSystemDataKeys
import com.intellij.openapi.externalSystem.model.project.ProjectData
import com.intellij.openapi.externalSystem.service.execution.ProgressExecutionMode
import com.intellij.openapi.externalSystem.service.project.ExternalProjectRefreshCallback
import com.intellij.openapi.externalSystem.service.project.ProjectDataManager
import com.intellij.openapi.externalSystem.util.ExternalSystemUtil
import com.intellij.openapi.project.Project
import com.intellij.util.PlatformUtils
import com.jetbrains.edu.learning.EduCourseBuilder
import com.jetbrains.edu.learning.isUnitTestMode
import com.jetbrains.edu.learning.projectView.CourseViewPane
import org.jetbrains.plugins.gradle.util.GradleConstants

class IdeaGradleCourseRefresher : GradleCourseRefresher {
  override fun isAvailable(): Boolean = PlatformUtils.isIntelliJ()

  override fun refresh(project: Project, listener: EduCourseBuilder.ProjectRefreshListener?) {
    val projectBasePath = project.basePath
    if (projectBasePath == null) {
      listener?.onFailure("Project path is null")
      return
    }

    val builder = ImportSpecBuilder(project, GradleConstants.SYSTEM_ID)
      .use(ProgressExecutionMode.IN_BACKGROUND_ASYNC)
      .dontReportRefreshErrors()
    if (listener == null) {
      builder.useDefaultCallback()
    } else {
      builder.callback(object : ExternalProjectRefreshCallback {
        override fun onSuccess(externalProject: DataNode<ProjectData>?) {
          // We have to import data manually because we use custom callback
          // but default callback code is private.
          // See `com.intellij.openapi.externalSystem.importing.ImportSpecBuilder#build`
          if (externalProject != null) {
            ServiceManager.getService(ProjectDataManager::class.java).importData(externalProject, project, false)
          }
          listener.onSuccess()
        }

        override fun onFailure(errorMessage: String, errorDetails: String?) {
          listener.onFailure(errorMessage)
        }
      })
    }
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
