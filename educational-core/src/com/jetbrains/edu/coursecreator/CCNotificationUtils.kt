package com.jetbrains.edu.coursecreator

import com.intellij.notification.Notification
import com.intellij.notification.NotificationListener
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.jetbrains.edu.coursecreator.stepik.CCStepikConnector
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.courseFormat.*
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.stepik.StepikNames
import org.jetbrains.annotations.Nls
import javax.swing.event.HyperlinkEvent

object CCNotificationUtils {
  private val LOG = Logger.getInstance(CCNotificationUtils::class.java)

  const val FAILED_TITLE = "Failed to publish "
  const val UPDATE_NOTIFICATION_GROUP_ID = "Update.course"
  private const val PUSH_COURSE_GROUP_ID = "Push.course"

  @JvmStatic
  fun getErrorMessage(item: StudyItem, isNew: Boolean): String {
    return "Failed to " + (if (isNew) "post " else "update ") + getItemInfo(item, isNew)
  }

  @JvmStatic
  fun getErrorMessage(item: StudyItem, parent: StudyItem, isNew: Boolean): String {
    return getErrorMessage(item, isNew) + " in " + getItemInfo(parent, false)
  }

  private fun getItemInfo(item: StudyItem, isNew: Boolean): String {
    val id = if (isNew) "" else " (id = " + item.id + ")"
    return getPrintableType(item) + " `" + item.name + "`" + id
  }

  private fun getPrintableType(item: StudyItem): String {
    if (item is Course) return EduNames.COURSE
    if (item is Section) return EduNames.SECTION
    if (item is FrameworkLesson) return EduNames.FRAMEWORK_LESSON
    if (item is Lesson) return EduNames.LESSON
    return if (item is Task) EduNames.TASK else "item"
  }

  @JvmStatic
  fun showErrorNotification(project: Project,
                            @Nls(capitalization = Nls.Capitalization.Sentence) title: String,
                            @Nls(capitalization = Nls.Capitalization.Sentence) message: String) {
    LOG.info(message)
    val notification = Notification(PUSH_COURSE_GROUP_ID, title, message, NotificationType.ERROR)
    notification.notify(project)
  }

  @JvmStatic
  fun showNoRightsToUpdateOnStepikNotification(project: Project, course: EduCourse) {
    showNoRightsToUpdateNotification(project, course, StepikNames.STEPIK) { CCStepikConnector.postCourseWithProgress(project, course) }
  }

  fun showNoRightsToUpdateNotification(project: Project, course: EduCourse, platformName: String, action: () -> Unit) {
    val message = "You don't have permission to update the course <br> <a href=\"upload\">Upload to $platformName as New Course</a>"
    val notification = Notification(PUSH_COURSE_GROUP_ID, FAILED_TITLE, message, NotificationType.ERROR,
                                    createPostCourseNotificationListener(course, action))
    notification.notify(project)
  }

  fun createPostCourseNotificationListener(course: EduCourse, action: () -> Unit): NotificationListener.Adapter {
    return object : NotificationListener.Adapter() {
      override fun hyperlinkActivated(notification: Notification, e: HyperlinkEvent) {
        notification.expire()
        course.convertToLocal()
        action()
      }
    }
  }

  @JvmStatic
  fun createPostStepikCourseNotificationListener(project: Project, course: EduCourse): NotificationListener.Adapter {
    return createPostCourseNotificationListener(course) { CCStepikConnector.postCourseWithProgress(project, course) }
  }

  @JvmStatic
  fun showNotification(project: Project,
                       @Nls(capitalization = Nls.Capitalization.Sentence) title: String,
                       action: AnAction?) {
  showNotification(project, action, title, "")
  }

  fun showNotification(project: Project,
                       action: AnAction?,
                       @Nls(capitalization = Nls.Capitalization.Sentence) title: String,
                       @Nls(capitalization = Nls.Capitalization.Sentence) message: String,) {
    val notification = Notification(PUSH_COURSE_GROUP_ID, title, message, NotificationType.INFORMATION)
    if (action != null) {
      notification.addAction(action)
    }
    notification.notify(project)
  }
}
