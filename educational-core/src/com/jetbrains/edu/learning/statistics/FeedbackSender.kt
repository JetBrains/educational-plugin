package com.jetbrains.edu.learning.statistics

import com.intellij.ide.util.PropertiesComponent
import com.intellij.notification.Notification
import com.intellij.notification.NotificationListener
import com.intellij.notification.NotificationType
import com.intellij.notification.impl.NotificationFullContent
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.NlsContexts.NotificationContent
import com.intellij.util.PlatformUtils
import com.jetbrains.edu.learning.EduBrowser
import com.jetbrains.edu.learning.capitalize
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.CourseMode
import com.jetbrains.edu.learning.messages.EduCoreBundle
import javax.swing.event.HyperlinkEvent

@Suppress("UnstableApiUsage")
fun showPostFeedbackNotification(student : Boolean, course: Course, project: Project) {
  val feedbackUrl = feedbackUrlTemplate
      .replace("\$PRODUCT", productMap[PlatformUtils.getPlatformPrefix()] ?:
                                             PlatformUtils.getPlatformPrefix())
      .replace("\$COURSE", course.name)
      .replace("\$MODE", if (course.courseMode == CourseMode.STUDENT) "Learner" else "Educator")

  val product = if (PlatformUtils.isPyCharmEducational()) "PyCharm Edu" else "EduTools"
  val language = course.languageID.lowercase().capitalize()

  val content = if (student) EduCoreBundle.message("feedback.template.student", product, feedbackUrl, language)
  else EduCoreBundle.message("feedback.template.creator", product, feedbackUrl, language)

  val notification = MyNotification(content, feedbackUrl)
  PropertiesComponent.getInstance().setValue(feedbackAsked, true)
  notification.notify(project)
}

fun showQuestionnaireAdvertisingNotification(project: Project, course: Course) {
  @Suppress("UnstableApiUsage")
  val questionnaireUrl = questionnaireUrlTemplate
    .replace("\$PRODUCT", productMap[PlatformUtils.getPlatformPrefix()] ?: PlatformUtils.getPlatformPrefix())
    .replace("\$COURSE_ID", course.id.toString())

  val notification = MyNotification(EduCoreBundle.message("check.correct.solution.no.exclamation"),
                                    EduCoreBundle.message("notification.student.survey", course.name, questionnaireUrl))
  PropertiesComponent.getInstance().setValue(questionnaireAdvertisingNotificationShown, true)
  notification.notify(project)
}

class MyNotification(@Suppress("UnstableApiUsage") @NotificationContent content: String, feedbackUrl: String) :
  Notification("EduTools", EduCoreBundle.message("check.correct.solution.no.exclamation"), content, NotificationType.INFORMATION),
  NotificationFullContent {
  init {
    setListener(object : NotificationListener.Adapter() {
      override fun hyperlinkActivated(notification: Notification, e: HyperlinkEvent) {
        EduBrowser.getInstance().browse(feedbackUrl)
      }
    })
  }
}

fun isFeedbackAsked() : Boolean = PropertiesComponent.getInstance().getBoolean(feedbackAsked)

fun isQuestionnaireAdvertisingNotificationShown() : Boolean = PropertiesComponent.getInstance().getBoolean(questionnaireAdvertisingNotificationShown)

private const val feedbackAsked = "askFeedbackNotification"

private const val questionnaireAdvertisingNotificationShown = "questionnaireAdvertisingNotification"

private const val feedbackUrlTemplate = "https://www.jetbrains.com/feedback/feedback.jsp?" +
                                "product=EduTools&ide=\$PRODUCT&course=\$COURSE&mode=\$MODE"

private const val questionnaireUrlTemplate = "https://surveys.jetbrains.com/s3/marketplace-courses-survey?ide=\$PRODUCT&courseId=\$COURSE_ID"

@Suppress("UnstableApiUsage")
private val productMap = hashMapOf(
    Pair(PlatformUtils.PYCHARM_CE_PREFIX, "PCC"),
    Pair(PlatformUtils.PYCHARM_PREFIX, "PCP"),
    Pair(PlatformUtils.PYCHARM_EDU_PREFIX, "PCE"),
    Pair(PlatformUtils.IDEA_CE_PREFIX, "IIC"),
    Pair(PlatformUtils.IDEA_PREFIX, "IIU"),
    Pair("AndroidStudio", "AI"),
    Pair(PlatformUtils.WEB_PREFIX, "WS"),
    Pair(PlatformUtils.PHP_PREFIX, "PS"),
    Pair(PlatformUtils.APPCODE_PREFIX, "AC"),
    Pair(PlatformUtils.CLION_PREFIX, "CL"),
    Pair(PlatformUtils.DBE_PREFIX, "DG"),
    Pair(PlatformUtils.GOIDE_PREFIX, "GO"),
    Pair(PlatformUtils.RIDER_PREFIX, "RD"),
    Pair(PlatformUtils.RUBY_PREFIX, "RM"))
