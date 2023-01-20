package com.jetbrains.edu.remote

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.invokeAndWaitIfNeeded
import com.jetbrains.codeWithMe.model.projectViewModel
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.projectView.CourseViewPane
import com.jetbrains.rd.platform.util.idea.LifetimedService
import com.jetbrains.rdserver.core.RemoteProjectSession

@Suppress("UnstableApiUsage")
class EduRemoteService(private val session: RemoteProjectSession) : LifetimedService() {
  init {
    val project = session.project
    val scheduler = session.protocol.scheduler
    scheduler.queue {
      val model = session.protocol.projectViewModel
      if (EduUtils.isEduProject(project)) {
        // This hack is needed because at the time this service is loaded our Course view is not present in the model
        ApplicationManager.getApplication().executeOnPooledThread {
          while (true) {
            Thread.sleep(1000)
            invokeAndWaitIfNeeded {
              model.activate.fire(CourseViewPane.ID)
            }
            if (model.currentPane.value == CourseViewPane.ID) {
              break
            }
          }
        }
      }
    }
  }

  companion object {
    @Suppress("unused")
    fun getInstance(session: RemoteProjectSession): EduRemoteService = session.getService(EduRemoteService::class.java)
  }
}
