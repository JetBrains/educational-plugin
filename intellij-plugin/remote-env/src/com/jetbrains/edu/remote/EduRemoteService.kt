package com.jetbrains.edu.remote

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.invokeAndWaitIfNeeded
import com.intellij.openapi.client.ClientProjectSession
import com.intellij.openapi.components.service
import com.intellij.openapi.rd.util.launchOnUi
import com.jetbrains.codeWithMe.model.projectViewModel
import com.jetbrains.edu.learning.EduUtilsKt.isEduProject
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.newproject.CourseProjectGenerator.Companion.EDU_PROJECT_CREATED
import com.jetbrains.edu.learning.projectView.CourseViewPane
import com.jetbrains.edu.learning.submissions.SubmissionSettings
import com.jetbrains.edu.learning.taskToolWindow.ui.TaskToolWindowView
import com.jetbrains.edu.remote.termination.EduRemoteDisconnectWatcherService
import com.jetbrains.edu.remote.termination.EduRemoteInactivityWatcherService
import com.jetbrains.rd.platform.util.idea.LifetimedService
import com.jetbrains.rdserver.core.protocolModel

@Suppress("UnstableApiUsage")
class EduRemoteService(private val session: ClientProjectSession): LifetimedService() {
  init {
    val project = session.project
    serviceLifetime.launchOnUi {
      val model = session.protocolModel.projectViewModel
      if (project.isEduProject()) {
        // This flag is set to true because the remote development project is not actually created from scratch when connected,
        // but rather the first course project opening for a user. Therefore, the corresponding existing code in the plugin is not invoked,
        // and we need to explicitly do it here.
        project.putUserData(EDU_PROJECT_CREATED, true)
        SubmissionSettings.getInstance(project).stateOnClose = true
        SubmissionSettings.getInstance(project).applySubmissionsForce = true

        val course = project.course ?: return@launchOnUi
        // This hack is needed because at the time this service is loaded our Course view is not present in the model
        ApplicationManager.getApplication().executeOnPooledThread {
          while (true) {
            Thread.sleep(1000)
            invokeAndWaitIfNeeded {
              model.activate.fire(CourseViewPane.ID)
            }
            if (model.currentPane.value == CourseViewPane.ID) {
              invokeAndWaitIfNeeded {
                TaskToolWindowView.getInstance(project).updateTaskDescription()
                service<EduRemoteDisconnectWatcherService>().start()
                service<EduRemoteInactivityWatcherService>().start()
                project.service<EduRemoteUidRetrieverService>().start()
              }
              break
            }
          }
        }
      }
    }
  }

  companion object {
    @Suppress("unused")
    fun getInstance(session: ClientProjectSession): EduRemoteService = session.getService(EduRemoteService::class.java)
  }
}
