package com.jetbrains.edu.coursecreator.actions.marketplace

import com.intellij.notification.Notification
import com.intellij.notification.NotificationType.INFORMATION
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.progress.currentThreadCoroutineScope
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.platform.ide.progress.withBackgroundProgress
import com.jetbrains.edu.coursecreator.CCUtils
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.StudyItem
import com.jetbrains.edu.learning.marketplace.StudyItemIdGenerator
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.notification.EduNotificationManager
import com.jetbrains.edu.learning.notification.RemoteConfigNotificationListener
import com.jetbrains.edu.learning.notification.RemoteConfigNotificationListener.Companion.hyperlinkText
import com.jetbrains.edu.learning.yaml.errorHandling.RemoteYamlLoadingException
import com.jetbrains.edu.learning.yaml.processError
import kotlinx.coroutines.launch

class RegenerateDuplicateIds : DumbAwareAction() {

  override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

  override fun update(e: AnActionEvent) {
    e.presentation.isEnabledAndVisible = false
    val project = e.project ?: return
    if (!CCUtils.isCourseCreator(project)) return
    e.presentation.isEnabledAndVisible = true
  }

  override fun actionPerformed(e: AnActionEvent) {
    val project = e.project ?: return
    val course = project.course ?: return

    currentThreadCoroutineScope().launch {
      withBackgroundProgress(project, EduCoreBundle.message("action.Educational.Educator.RegenerateDuplicateIds.progress.title")) {
        try {
          val changedItems = StudyItemIdGenerator.getInstance(project).regenerateDuplicateIds(course)
          showFinishNotification(project, changedItems)
          e.getData(Notification.KEY)?.expire()
        }
        catch (e: RemoteYamlLoadingException) {
          logger<RegenerateDuplicateIds>().warn(e)
          e.processError(project)
        }
      }
    }
  }

  private fun showFinishNotification(project: Project, changedItems: List<StudyItem>) {
    val message = if (changedItems.isEmpty()) {
      EduCoreBundle.message("action.Educational.Educator.RegenerateDuplicateIds.notification.no.item.changed")
    }
    else {
      EduCoreBundle.message(
        "action.Educational.Educator.RegenerateDuplicateIds.notification.items.changed",
        changedItems.joinToString { it.hyperlinkText() })
    }

    @Suppress("DEPRECATION")
    EduNotificationManager.create(
      INFORMATION,
      EduCoreBundle.message("action.Educational.Educator.RegenerateDuplicateIds.notification.title"),
      message
    )
      .setListener(RemoteConfigNotificationListener(project))
      .notify(project)
  }

  companion object {
    const val ACTION_ID = "Educational.Educator.RegenerateDuplicateIds"
  }
}
