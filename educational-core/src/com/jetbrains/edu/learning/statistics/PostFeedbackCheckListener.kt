package com.jetbrains.edu.learning.statistics

import com.intellij.ide.BrowserUtil
import com.intellij.notification.Notification
import com.intellij.notification.NotificationListener
import com.intellij.notification.NotificationType
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.jetbrains.edu.learning.checker.CheckListener
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.ui.taskDescription.TaskDescriptionToolWindow

class PostFeedbackCheckListener : CheckListener {
  private val FEEDBACK_LINK = ""  // TODO: paste proper link here
  private val FEEDBACK_ASKED = Key.create<Boolean>("askFeedbackNotification")

  override fun afterCheck(project: Project, task: Task) {
    val feedbackAsked = project.getUserData(FEEDBACK_ASKED)
    if (feedbackAsked != null && feedbackAsked) return

    val course = task.lesson.course
    val lessons = course.lessons

    val progress = TaskDescriptionToolWindow.countProgressWithoutSubtasks(lessons)
    val solvedTasks = progress.getFirst()
    if (solvedTasks != 0 && solvedTasks!! % 10 == 0) {
      val notification = Notification("eduTools.shareFeedback", "Feedback",
          "<html>Thank you for using the EduTool plugin! <br/> " +
              "Please, <a href=\"" + FEEDBACK_LINK + "\">share</a> your experience. " +
              "Your feedback will help us make learning experience better.</html>",
          NotificationType.INFORMATION, NotificationListener { _, hyperlinkEvent ->
            val description = hyperlinkEvent.description
            BrowserUtil.browse(description)
            if (FEEDBACK_LINK == description) {
              project.putUserData(FEEDBACK_ASKED, true)
            }
          }
      )
      notification.notify(project)
    }
  }
}
