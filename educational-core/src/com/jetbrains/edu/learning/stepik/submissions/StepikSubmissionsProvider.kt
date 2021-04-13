package com.jetbrains.edu.learning.stepik.submissions

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
import com.jetbrains.edu.learning.stepik.StepikAuthorizer
import com.jetbrains.edu.learning.stepik.StepikNames
import com.jetbrains.edu.learning.stepik.api.StepikConnector
import com.jetbrains.edu.learning.stepik.api.Submission

class StepikSubmissionsProvider : SubmissionsProvider {

  override fun loadAllSubmissions(project: Project, course: Course): Map<Int, MutableList<Submission>> {
    val submissionsById = mutableMapOf<Int, MutableList<Submission>>()
    if (course is EduCourse && course.isStepikRemote && isLoggedIn()) {
      val allTasks: List<Task> = course.allTasks
      for (task in allTasks) {
        if (task is ChoiceTask) {
          submissionsById[task.id] = mutableListOf()
        }
        else if (task is CodeTask || task is EduTask) {
          submissionsById.putAll(loadSubmissions(setOf(task.id)))
        }
      }
    }
    return submissionsById
  }

  override fun loadSubmissions(stepIds: Set<Int>): Map<Int, MutableList<Submission>> {
    val submissionsById = mutableMapOf<Int, MutableList<Submission>>()
    for (stepId in stepIds) {
      submissionsById[stepId] = StepikConnector.getInstance().getSubmissions(stepId).toMutableList()
    }
    return submissionsById
  }

  override fun areSubmissionsAvailable(course: Course): Boolean {
    return course is EduCourse && course.isStudy && course.isStepikRemote
  }

  override fun getPlatformName(): String = StepikNames.STEPIK

  override fun isLoggedIn(): Boolean = EduSettings.isLoggedIn()

  override fun doAuthorize() {
    StepikAuthorizer.doAuthorize { EduUtils.showOAuthDialog() }
    EduCounterUsageCollector.loggedIn(StepikNames.STEPIK, EduCounterUsageCollector.AuthorizationPlace.SUBMISSIONS_TAB)
  }
}