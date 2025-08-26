package com.jetbrains.edu.learning.statistics

import com.intellij.ide.util.PropertiesComponent
import com.intellij.notification.NotificationType.INFORMATION
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.NlsContexts.NotificationContent
import com.intellij.openapi.util.NlsContexts.NotificationTitle
import com.jetbrains.edu.learning.actions.LeaveInIdeFeedbackAction
import com.jetbrains.edu.learning.capitalize
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.notification.EduNotificationManager

fun showCCPostFeedbackNotification(course: Course, project: Project) {
  val language = course.languageId.lowercase().capitalize()
  showPostFeedbackNotification(
    project,
    EduCoreBundle.message("check.correct.solution.title"),
    EduCoreBundle.message("feedback.template.creator", language)
  )
}

fun showStudentPostFeedbackNotification(project: Project) = showPostFeedbackNotification(
  project,
  EduCoreBundle.message("feedback.template.student.title"),
  EduCoreBundle.message("feedback.template.student")
)

private fun showPostFeedbackNotification(
  project: Project,
  @NotificationTitle title: String,
  @NotificationContent content: String,
) {
  PropertiesComponent.getInstance().setValue(LEAVE_FEEDBACK_NOTIFICATION_SHOWN, true)
  EduNotificationManager.create(INFORMATION, title, content)
    .addAction(ActionManager.getInstance().getAction(LeaveInIdeFeedbackAction.ACTION_ID))
    .setSuggestionType(true)
    .notify(project)
}

fun isLeaveFeedbackPrompted(): Boolean = PropertiesComponent.getInstance().getBoolean(LEAVE_FEEDBACK_NOTIFICATION_SHOWN)

private const val LEAVE_FEEDBACK_NOTIFICATION_SHOWN = "questionnaireAdvertisingNotification"

