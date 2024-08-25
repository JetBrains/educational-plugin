package com.jetbrains.edu.android.gradle

import com.android.tools.idea.gradle.project.sync.GradleSyncListener
import com.intellij.ide.projectView.ProjectView
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.externalSystem.util.ExternalSystemUtil
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.projectView.CourseViewPane

class ChangeViewGradleSyncListener : GradleSyncListener {
  override fun syncSucceeded(project: Project) {
    ExternalSystemUtil.invokeLater(project, ModalityState.nonModal()) {
      if (!project.isDisposed && project.course != null) {
        ProjectView.getInstance(project).changeViewCB(CourseViewPane.ID, null)
      }
    }
  }
}