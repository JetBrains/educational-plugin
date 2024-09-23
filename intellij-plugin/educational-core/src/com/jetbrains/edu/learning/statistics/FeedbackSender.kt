package com.jetbrains.edu.learning.statistics

import com.intellij.ide.util.PropertiesComponent
import com.intellij.notification.Notification
import com.intellij.notification.NotificationListener
import com.intellij.notification.NotificationType.INFORMATION
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.NlsContexts.NotificationContent
import com.intellij.openapi.util.NlsContexts.NotificationTitle
import com.intellij.util.PlatformUtils
import com.jetbrains.edu.learning.EduBrowser
import com.jetbrains.edu.learning.capitalize
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.notification.EduNotificationManager
import javax.swing.event.HyperlinkEvent

fun showCCPostFeedbackNotification(course: Course, project: Project) {
  val language = course.languageId.lowercase().capitalize()
  val feedbackUrl = getFeedbackUrl(course.name, "Educator")
  showPostFeedbackNotification(project, EduCoreBundle.message("check.correct.solution.title"), EduCoreBundle.message("feedback.template.creator", feedbackUrl, language), feedbackUrl)
}

fun showStudentPostFeedbackNotification(courseName: String, project: Project) {
  val feedbackUrl = getFeedbackUrl(courseName, "Learner")
  showPostFeedbackNotification(project, EduCoreBundle.message("feedback.template.student.title"), EduCoreBundle.message("feedback.template.student", feedbackUrl), feedbackUrl)
}

private fun getFeedbackUrl(courseName: String, courseMode: String): String {
  return FEEDBACK_URL_TEMPLATE
    .replace(
      "\$PRODUCT", productMap[PlatformUtils.getPlatformPrefix()] ?: PlatformUtils.getPlatformPrefix()
    )
    .replace("\$COURSE", courseName)
    .replace("\$MODE", courseMode)
}

private fun showPostFeedbackNotification(project: Project, notificationTitle: String, notificationText: String, feedbackUrl: String) {
  PropertiesComponent.getInstance().setValue(LEAVE_FEEDBACK_NOTIFICATION_SHOWN, true)
  showFeedbackNotification(
    project,
    notificationTitle,
    notificationText,
    feedbackUrl
  )
}

@Suppress("DEPRECATION")
private fun showFeedbackNotification(
  project: Project,
  @NotificationTitle title: String,
  @NotificationContent content: String,
  feedbackUrl: String
) {
  EduNotificationManager
    .create(INFORMATION, title, content)
    .setListener(object : NotificationListener.Adapter() {
      override fun hyperlinkActivated(notification: Notification, e: HyperlinkEvent) {
        EduBrowser.getInstance().browse(feedbackUrl)
      }
    })
    .notify(project)
}

fun isLeaveFeedbackPrompted() : Boolean = PropertiesComponent.getInstance().getBoolean(LEAVE_FEEDBACK_NOTIFICATION_SHOWN)

private const val LEAVE_FEEDBACK_NOTIFICATION_SHOWN = "questionnaireAdvertisingNotification"

private const val FEEDBACK_URL_TEMPLATE = "https://www.jetbrains.com/feedback/feedback.jsp?product=EduTools&ide=\$PRODUCT&course=\$COURSE&mode=\$MODE"

@Suppress("UnstableApiUsage")
private val productMap = hashMapOf(
  PlatformUtils.PYCHARM_CE_PREFIX to "PCC",
  PlatformUtils.PYCHARM_PREFIX to "PCP",
  PlatformUtils.PYCHARM_EDU_PREFIX to "PCE",
  PlatformUtils.IDEA_CE_PREFIX to "IIC",
  PlatformUtils.IDEA_PREFIX to "IIU",
  "AndroidStudio" to "AI",
  PlatformUtils.WEB_PREFIX to "WS",
  PlatformUtils.PHP_PREFIX to "PS",
  PlatformUtils.APPCODE_PREFIX to "AC",
  PlatformUtils.CLION_PREFIX to "CL",
  PlatformUtils.DBE_PREFIX to "DG",
  PlatformUtils.GOIDE_PREFIX to "GO",
  PlatformUtils.RIDER_PREFIX to "RD",
  PlatformUtils.RUBY_PREFIX to "RM",
  PlatformUtils.DATASPELL_PREFIX to "DS"
)
