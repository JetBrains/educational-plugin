package com.jetbrains.edu.learning.statistics

import com.intellij.ide.util.PropertiesComponent
import com.intellij.notification.Notification
import com.intellij.notification.NotificationType.INFORMATION
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionUiKind
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.ex.ActionUtil
import com.intellij.openapi.actionSystem.impl.SimpleDataContext
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.NlsContexts.NotificationContent
import com.intellij.openapi.util.NlsContexts.NotificationTitle
import com.jetbrains.edu.learning.actions.LeaveInIdeFeedbackAction
import com.jetbrains.edu.learning.capitalize
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.invokeLater
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.notification.EduNotificationManager
import javax.swing.event.HyperlinkEvent

fun showCCPostFeedbackNotification(course: Course, project: Project) {
  val language = course.languageId.lowercase().capitalize()
  showPostFeedbackNotification(
    project,
    EduCoreBundle.message("check.correct.solution.title"),
    EduCoreBundle.message("feedback.template.creator", language)
  )
}

fun showStudentPostFeedbackNotification(project: Project) {
  showPostFeedbackNotification(
    project,
    EduCoreBundle.message("feedback.template.student.title"),
    EduCoreBundle.message("feedback.template.student")
  )
}

private fun showPostFeedbackNotification(project: Project, notificationTitle: String, notificationText: String) {
  PropertiesComponent.getInstance().setValue(LEAVE_FEEDBACK_NOTIFICATION_SHOWN, true)
  showFeedbackNotification(
    project,
    notificationTitle,
    notificationText,
  )
}

@Suppress("DEPRECATION")
private fun showFeedbackNotification(
  project: Project,
  @NotificationTitle title: String,
  @NotificationContent content: String,
) {
  EduNotificationManager.create(INFORMATION, title, content).apply {
    setListener { notification, event ->
      handleSurveyNotificationLinkClick(notification, event, project)
    }
  }.notify(project)
}

private fun handleSurveyNotificationLinkClick(
  notification: Notification,
  event: HyperlinkEvent,
  project: Project
) {
  if (event.eventType != HyperlinkEvent.EventType.ACTIVATED || event.description != "action://survey") {
    return
  }
  val actionManager = ActionManager.getInstance()
  val action = actionManager.getAction(LeaveInIdeFeedbackAction.ACTION_ID)

  if (action != null) {
    val actionEvent = AnActionEvent.createEvent(
      SimpleDataContext.builder().add(CommonDataKeys.PROJECT, project).build(),
      action.templatePresentation.clone(),
      "FeedbackNotification",
      ActionUiKind.NONE,
      null
    )
    project.invokeLater {
      ActionUtil.invokeAction(action, actionEvent) {}
    }
  }
  notification.expire()
}

fun isLeaveFeedbackPrompted(): Boolean = PropertiesComponent.getInstance().getBoolean(LEAVE_FEEDBACK_NOTIFICATION_SHOWN)

private const val LEAVE_FEEDBACK_NOTIFICATION_SHOWN = "questionnaireAdvertisingNotification"

