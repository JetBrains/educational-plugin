package com.jetbrains.edu.remote

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.invokeAndWaitIfNeeded
import com.intellij.openapi.fileEditor.ClientFileEditorManager
import com.intellij.openapi.project.Project
import com.jetbrains.codeWithMe.model.*
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseDir
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.ext.getDir
import com.jetbrains.edu.learning.navigation.NavigationUtils
import com.jetbrains.edu.learning.projectView.CourseViewPane
import com.jetbrains.edu.learning.taskDescription.ui.TaskDescriptionView
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
        val course = project.course ?: return@queue

        // This hack is needed because at the time this service is loaded our Course view is not present in the model
        ApplicationManager.getApplication().executeOnPooledThread {
          while (true) {
            Thread.sleep(1000)
            invokeAndWaitIfNeeded {
              model.activate.fire(CourseViewPane.ID)
            }
            if (model.currentPane.value == CourseViewPane.ID) {
              openFirstTask(session, course, project)
              break
            }
          }
        }
      }
    }
  }

  private fun openFirstTask(session: RemoteProjectSession, course: Course, project: Project) {
    val task = EduUtils.getFirstTask(course) ?: return
    val taskDir = task.getDir(project.courseDir) ?: return
    val taskFile = NavigationUtils.getFirstTaskFile(taskDir, task) ?: return
    val fileEditorManager = session.getService(ClientFileEditorManager::class.java) ?: return

    invokeAndWaitIfNeeded {
      fileEditorManager.openFile(taskFile, false)
      TaskDescriptionView.getInstance(project).updateTaskDescription()
    }
  }

  companion object {
    @Suppress("unused")
    fun getInstance(session: RemoteProjectSession): EduRemoteService = session.getService(EduRemoteService::class.java)
  }
}
