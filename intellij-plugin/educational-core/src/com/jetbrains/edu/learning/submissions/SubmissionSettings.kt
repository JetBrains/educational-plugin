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

  /**
   * Shows if apply submissions should be performed regardless of the course update version
   * Is added specially for courses launched via Remote Development solution to keep user code in editor on course updates
   * To be fixed properly by EDU-7466
   */
  @Volatile
  var applySubmissionsForce: Boolean = false
    get() = field || System.getProperty(APPLY_SUBMISSIONS_FORCE).toBoolean()

  companion object {
    private const val STATE_ON_CLOSE_PROPERTY: String = "edu.course.state.on.close"

    private const val APPLY_SUBMISSIONS_FORCE: String = "edu.course.apply.submissions.force"

    fun getInstance(project: Project): SubmissionSettings = project.service()
  }
}
