package com.jetbrains.edu.remote

import com.intellij.openapi.application.EDT
import com.intellij.openapi.client.ClientProjectSession
import com.intellij.openapi.components.service
import com.jetbrains.codeWithMe.model.projectViewModel
import com.jetbrains.edu.learning.EduUtilsKt.isEduProject
import com.jetbrains.edu.learning.newproject.CourseProjectGenerator.Companion.EDU_PROJECT_CREATED
import com.jetbrains.edu.learning.projectView.CourseViewPane
import com.jetbrains.edu.learning.submissions.SubmissionSettings
import com.jetbrains.edu.learning.taskToolWindow.ui.TaskToolWindowView
import com.jetbrains.edu.remote.termination.EduRemoteDisconnectWatcherService
import com.jetbrains.edu.remote.termination.EduRemoteInactivityWatcherService
import com.jetbrains.rd.platform.util.idea.LifetimedService
import com.jetbrains.rd.util.threading.coroutines.lifetimedCoroutineScope
import com.jetbrains.rdserver.core.protocolModel
import kotlinx.coroutines.*

@Suppress("UnstableApiUsage")
class EduRemoteService(private val session: ClientProjectSession, private val scope: CoroutineScope): LifetimedService() {
  init {
    init()
  }

  private fun init() {
    val project = session.project
    if (!project.isEduProject()) return

    scope.launch(Dispatchers.EDT) {
      lifetimedCoroutineScope(serviceLifetime) {
        val model = session.protocolModel.projectViewModel
        // This flag is set to true because the remote development project is not actually created from scratch when connected,
        // but rather the first course project opening for a user. Therefore, the corresponding existing code in the plugin is not invoked,
        // and we need to explicitly do it here.
        project.putUserData(EDU_PROJECT_CREATED, true)
        SubmissionSettings.getInstance(project).stateOnClose = true
        SubmissionSettings.getInstance(project).applySubmissionsForce = true

        // This hack is needed because at the time this service is loaded our Course view is not present in the model
        withContext(Dispatchers.Default) {
          while (true) {
            delay(1000)
            withContext(Dispatchers.EDT) {
              model.activate.fire(CourseViewPane.ID)
            }
            if (model.currentPane.value == CourseViewPane.ID) {
              withContext(Dispatchers.EDT) {
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
