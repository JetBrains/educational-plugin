package com.jetbrains.edu.learning.update

import com.intellij.notification.Notification
import com.intellij.notification.NotificationAction
import com.intellij.notification.NotificationType
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.jetbrains.edu.coursecreator.CCNotificationUtils.showNotification
import com.jetbrains.edu.learning.computeUnderProgress
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.runInBackground

@Suppress("DuplicatedCode")
abstract class EduCourseUpdater(protected val project: Project, protected open val course: Course) {
  protected open val updateAutomatically: Boolean = false

  protected abstract fun areUpdatesAvailable(): Boolean
  protected abstract fun getCourseFromServer(): Course?
  protected abstract fun doUpdate()

  fun update() {
    val updatesAvailable = computeUnderProgress(project, EduCoreBundle.message("update.check")) {
      areUpdatesAvailable()
    }
    if (!updatesAvailable) {
      showNotification(project, EduCoreBundle.message("notification.course.up.to.date"), null)
      return
    }

    if (updateAutomatically) {
      runInBackground(project, EduCoreBundle.message("update.process"), false) {
        doUpdate()
      }
    } else {
      showUpdateAvailableNotification {
        runInBackground(project, EduCoreBundle.message("update.process"), false) {
          doUpdate()
        }
      }
    }
  }

  private fun showUpdateAvailableNotification(updateAction: () -> Unit) {
    Notification(
      "JetBrains Academy",
      EduCoreBundle.message("update.content"),
      EduCoreBundle.message("update.content.request"),
      NotificationType.INFORMATION
    ).addAction(NotificationAction.createSimpleExpiring(EduCoreBundle.message("update.action")) {
      FileEditorManagerEx.getInstanceEx(project).closeAllFiles()
      ProgressManager.getInstance().runProcessWithProgressSynchronously(
        {
          ProgressManager.getInstance().progressIndicator.isIndeterminate = true
          updateAction()
        },
        EduCoreBundle.message("push.course.updating.progress.text"), true, project
      )
    })
      .notify(project)
  }

  companion object {
    @JvmStatic
    protected val LOG: Logger = Logger.getInstance(EduCourseUpdater::class.java)
  }
}