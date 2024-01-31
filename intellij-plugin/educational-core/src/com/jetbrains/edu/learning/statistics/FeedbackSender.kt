package com.jetbrains.edu.learning.statistics

import com.intellij.ide.util.PropertiesComponent
import com.intellij.notification.Notification
import com.intellij.notification.NotificationListener
import com.intellij.notification.NotificationType
import com.intellij.notification.impl.NotificationFullContent
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.NlsContexts.NotificationContent
import com.intellij.openapi.util.NlsContexts.NotificationTitle
import com.intellij.util.PlatformUtils
import com.jetbrains.edu.learning.EduBrowser
import com.jetbrains.edu.learning.capitalize
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.CourseMode
import com.jetbrains.edu.learning.messages.EduCoreBundle
import javax.swing.event.HyperlinkEvent

@Suppress("UnstableApiUsage")
fun showCCPostFeedbackNotification(course: Course, project: Project) {
  val feedbackUrl = FEEDBACK_URL_TEMPLATE
      .replace(
        "\$PRODUCT", productMap[PlatformUtils.getPlatformPrefix()] ?: PlatformUtils.getPlatformPrefix()
      )
      .replace("\$COURSE", course.name)
      .replace("\$MODE", if (course.courseMode == CourseMode.STUDENT) "Learner" else "Educator")

  val language = course.languageId.lowercase().capitalize()

  val notification = FeedbackNotification(EduCoreBundle.message("check.correct.solution.title"), EduCoreBundle.message("feedback.template.creator", feedbackUrl, language), feedbackUrl)
  PropertiesComponent.getInstance().setValue(FEEDBACK_ASKED, true)
  notification.notify(project)
}

fun showStudentPostFeedbackNotification(project: Project) {
  val notification = FeedbackNotification(
    EduCoreBundle.message("feedback.template.student.title"),
    EduCoreBundle.message("feedback.template.student", UX_RESEARCH_LINK),
    UX_RESEARCH_LINK
  )
  PropertiesComponent.getInstance().setValue(FEEDBACK_ASKED, true)
  notification.notify(project)
}

// we have plans to use the showQuestionnaireAdvertisingNotification once again later
@Suppress("unused")
fun showQuestionnaireAdvertisingNotification(project: Project, course: Course) {
  @Suppress("UnstableApiUsage")
  val questionnaireUrl = QUESTIONNAIRE_URL_TEMPLATE
    .replace("\$PRODUCT", productMap[PlatformUtils.getPlatformPrefix()] ?: PlatformUtils.getPlatformPrefix())
    .replace("\$COURSE_ID", course.id.toString())

  val notification = FeedbackNotification(EduCoreBundle.message("check.correct.solution.title"), EduCoreBundle.message("notification.student.survey", course.name, questionnaireUrl), questionnaireUrl)
  PropertiesComponent.getInstance().setValue(QUESTIONNAIRE_ADVERTISING_NOTIFICATION_SHOWN, true)
  notification.notify(project)
}

private class FeedbackNotification(@NotificationTitle title: String, @NotificationContent content: String, feedbackUrl: String) :
  Notification("JetBrains Academy", title, content, NotificationType.INFORMATION),
  NotificationFullContent {
  init {
    setListener(object : NotificationListener.Adapter() {
      override fun hyperlinkActivated(notification: Notification, e: HyperlinkEvent) {
        EduBrowser.getInstance().browse(feedbackUrl)
      }
    })
  }
}

fun isFeedbackAsked() : Boolean = PropertiesComponent.getInstance().getBoolean(FEEDBACK_ASKED)

fun isQuestionnaireAdvertisingNotificationShown() : Boolean = PropertiesComponent.getInstance().getBoolean(QUESTIONNAIRE_ADVERTISING_NOTIFICATION_SHOWN)

private const val FEEDBACK_ASKED = "askFeedbackNotification"

private const val QUESTIONNAIRE_ADVERTISING_NOTIFICATION_SHOWN = "questionnaireAdvertisingNotification"

private const val FEEDBACK_URL_TEMPLATE = "https://www.jetbrains.com/feedback/feedback.jsp?" +
                                          "product=EduTools&ide=\$PRODUCT&course=\$COURSE&mode=\$MODE"

private const val QUESTIONNAIRE_URL_TEMPLATE = "https://surveys.jetbrains.com/s3/marketplace-courses-survey?ide=\$PRODUCT&courseId=\$COURSE_ID"

private const val UX_RESEARCH_LINK = "https://usabi.li/do/s/bda1fa"

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
  PlatformUtils.RUBY_PREFIX to "RM"
)
