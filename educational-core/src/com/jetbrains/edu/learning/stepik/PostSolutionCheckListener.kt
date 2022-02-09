package com.jetbrains.edu.learning.stepik

import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.checker.CheckListener
import com.jetbrains.edu.learning.checker.CheckResult
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.isUnitTestMode
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.stepik.api.StepikBasedSubmission
import com.jetbrains.edu.learning.submissions.SubmissionsManager

abstract class PostSolutionCheckListener : CheckListener {

  protected abstract fun isUpToDate(course: EduCourse, task: Task): Boolean
  protected abstract fun postSubmission(project: Project, task: Task): StepikBasedSubmission?
  protected abstract fun updateCourseAction(project: Project, course: EduCourse)
  protected abstract fun EduCourse.isToPostSubmissions(): Boolean

  override fun afterCheck(project: Project, task: Task, result: CheckResult) {
    val course = task.lesson.course
    if (course is EduCourse && course.isStudy && course.isToPostSubmissions() && task.isToSubmitToRemote) {
      if (isUpToDate(course, task)) {
        if (!isUnitTestMode) {
          ApplicationManager.getApplication().executeOnPooledThread {
            addSubmissionToSubmissionsManager(project, task)
          }
        }
        else {
          addSubmissionToSubmissionsManager(project, task)
        }
      }
      else {
        showSubmissionNotPostedNotification(project, course, task.name)
      }
    }
  }

  private fun addSubmissionToSubmissionsManager(project: Project, task: Task) {
    val submission = postSubmission(project, task) ?: return
    SubmissionsManager.getInstance(project).addToSubmissionsWithStatus(task.id, task.status, submission)
  }

  private fun showSubmissionNotPostedNotification(project: Project, course: EduCourse, taskName: String) {
    Notification("EduTools",
                 EduCoreBundle.message("error.solution.not.posted"),
                 EduCoreBundle.message("notification.content.task.was.updated", taskName),
                 NotificationType.INFORMATION)
      .setListener(notificationListener(project) { updateCourseAction(project, course) })
      .notify(project)
  }
}
