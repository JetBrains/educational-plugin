package com.jetbrains.edu.learning.stepik

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.checker.CheckListener
import com.jetbrains.edu.learning.courseFormat.CheckResult
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.marketplace.api.MarketplaceConnector
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.notification.EduNotificationManager
import com.jetbrains.edu.learning.submissions.Submission
import com.jetbrains.edu.learning.submissions.SubmissionsManager

abstract class PostSolutionCheckListener : CheckListener {

  protected abstract fun isUpToDate(course: EduCourse, task: Task): Boolean
  protected abstract fun postSubmission(project: Project, task: Task, result: CheckResult): Submission
  protected abstract fun updateCourseAction(project: Project, course: EduCourse)
  protected abstract fun EduCourse.isToPostSubmissions(): Boolean

  override fun afterCheck(project: Project, task: Task, result: CheckResult) {
    val course = task.lesson.course
    if (course is EduCourse && course.isStudy && course.isToPostSubmissions() && task.isToSubmitToRemote) {
      MarketplaceConnector.getInstance().isLoggedInAsync().thenApplyAsync { loggedIn ->
        if (loggedIn) {
          if (isUpToDate(course, task)) {
            addSubmissionToSubmissionsManager(project, task, result)
          }
          else {
            showSubmissionNotPostedNotification(project, course, task.name)
          }
        }
      }
    }
  }

  private fun addSubmissionToSubmissionsManager(project: Project, task: Task, result: CheckResult) {
    val submission = postSubmission(project, task, result)
    SubmissionsManager.getInstance(project).addToSubmissionsWithStatus(task.id, task.status, submission)
  }

  private fun showSubmissionNotPostedNotification(project: Project, course: EduCourse, taskName: String) {
    EduNotificationManager.showInfoNotification(
      project,
      EduCoreBundle.message("error.solution.not.posted"),
      EduCoreBundle.message("notification.content.task.was.updated", taskName),
    ) {
      addAction(object : AnAction() {
        override fun actionPerformed(e: AnActionEvent) = updateCourseAction(project, course)
      })
    }
  }
}
