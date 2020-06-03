package com.jetbrains.edu.learning.stepik

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.EduSettings
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.courseFormat.ext.allTasks
import com.jetbrains.edu.learning.courseFormat.tasks.CodeTask
import com.jetbrains.edu.learning.courseFormat.tasks.EduTask
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.courseFormat.tasks.choice.ChoiceTask
import com.jetbrains.edu.learning.statistics.EduCounterUsageCollector
import com.jetbrains.edu.learning.stepik.api.StepikConnector
import com.jetbrains.edu.learning.stepik.api.Submission
import com.jetbrains.edu.learning.taskDescription.ui.TaskDescriptionView

class StepikSubmissionsProvider : SubmissionsProvider() {

  override fun loadAllSubmissions(project: Project, course: Course?, onFinish: () -> Unit) {
    if (course is EduCourse && course.isRemote && isLoggedIn()) {
      ApplicationManager.getApplication().executeOnPooledThread {
        val submissionsManager = SubmissionsManager.getInstance(project)
        val allTasks: List<Task> = course.allTasks
        for (task in allTasks) {
          if (task is ChoiceTask) {
            submissionsManager.putToSubmissions(task.id, mutableListOf())
          }
          else if (task is CodeTask || task is EduTask) {
            getAllSubmissions(task.id, submissionsManager)
          }
        }
        onFinish()
        ApplicationManager.getApplication().invokeLater {
          TaskDescriptionView.getInstance(project).updateSubmissionsTab()
        }
      }
    }
  }

  override fun getAllSubmissions(stepIds: Set<Int>, submissionsManager: SubmissionsManager): List<Submission>? {
    val allSubmissions = mutableListOf<Submission>()
    for (stepId in stepIds) {
      allSubmissions.addAll(submissionsManager.getOrPut(stepId) { loadSubmissions(stepId, submissionsManager) })
    }
    return allSubmissions
  }

  override fun loadSubmissions(stepId: Int, submissionsManager: SubmissionsManager): List<Submission> {
    return StepikConnector.getInstance().getAllSubmissions(stepId, submissionsManager)
  }

  override fun submissionsCanBeShown(course: Course?): Boolean {
    return course is EduCourse && course.isStudy && course.isRemote
  }

  override fun getPlatformName(): String = StepikNames.STEPIK

  override fun isLoggedIn(): Boolean = EduSettings.isLoggedIn()

  override fun doAuthorize() {
    StepikAuthorizer.doAuthorize { EduUtils.showOAuthDialog() }
    EduCounterUsageCollector.loggedIn(StepikNames.STEPIK, EduCounterUsageCollector.AuthorizationPlace.SUBMISSIONS_TAB)
  }
}