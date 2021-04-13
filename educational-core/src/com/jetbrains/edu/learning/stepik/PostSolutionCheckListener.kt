package com.jetbrains.edu.learning.stepik

import com.intellij.notification.Notification
import com.intellij.notification.NotificationDisplayType
import com.intellij.notification.NotificationGroup
import com.intellij.notification.NotificationType
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.EduSettings
import com.jetbrains.edu.learning.checker.CheckListener
import com.jetbrains.edu.learning.checker.CheckResult
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.isUnitTestMode
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.stepik.api.Submission
import com.jetbrains.edu.learning.stepik.submissions.SubmissionsManager

class PostSolutionCheckListener : CheckListener {
  override fun afterCheck(project: Project, task: Task, result: CheckResult) {
    val course = task.lesson.course
    if (course is EduCourse && course.isStepikRemote && course.isStudy && EduSettings.isLoggedIn() && task.isToSubmitToStepik) {
      if (task.isUpToDate) {
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
    val submission: Submission? = StepikSolutionsLoader.postSolution(task, task.status == CheckStatus.Solved, project)
    SubmissionsManager.getInstance(project).addToSubmissionsWithStatus(task.id, task.status, submission)
  }

  private fun showSubmissionNotPostedNotification(project: Project, course: EduCourse, taskName: String) {
    val notificationGroup = NotificationGroup(EduCoreBundle.message("error.solution.not.posted"), NotificationDisplayType.NONE, true)
    val notification = Notification(notificationGroup.displayId,
                                    EduCoreBundle.message("error.solution.not.posted"),
                                    EduCoreBundle.message("stepik.task.was.updated", StepikNames.STEPIK, taskName),
                                    NotificationType.INFORMATION,
                                    notificationListener(project) { updateCourse(project, course) })
    notification.notify(project)
  }
}
