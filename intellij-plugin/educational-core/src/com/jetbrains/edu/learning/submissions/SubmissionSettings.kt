package com.jetbrains.edu.learning.submissions

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project

@Service(Service.Level.PROJECT)
class SubmissionSettings {
  /**
   * Shows if saving and restoring course state on project closing is enabled or not
   */
  @Volatile
  var stateOnClose: Boolean = false
    get() = field || System.getProperty(STATE_ON_CLOSE_PROPERTY).toBoolean()

  companion object {
    private const val STATE_ON_CLOSE_PROPERTY: String = "edu.course.state.on.close"

    fun getInstance(project: Project): SubmissionSettings = project.service()
  }
}
