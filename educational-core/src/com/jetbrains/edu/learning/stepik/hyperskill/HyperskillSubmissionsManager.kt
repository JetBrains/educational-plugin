package com.jetbrains.edu.learning.stepik.hyperskill

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.statistics.EduCounterUsageCollector
import com.jetbrains.edu.learning.stepik.SubmissionsManager
import com.jetbrains.edu.learning.stepik.api.Submission
import com.jetbrains.edu.learning.stepik.hyperskill.api.HyperskillConnector
import com.jetbrains.edu.learning.stepik.hyperskill.api.HyperskillSolutionLoader
import com.jetbrains.edu.learning.stepik.hyperskill.courseFormat.HyperskillCourse
import com.jetbrains.edu.learning.stepik.hyperskill.settings.HyperskillSettings
import com.jetbrains.edu.learning.taskDescription.ui.TaskDescriptionView

class HyperskillSubmissionsManager : SubmissionsManager() {

  fun putToSubmissions(stepIds: Set<Int>, submissionsList: List<Submission>?) {
    if(submissionsList == null) return
    for(stepId in stepIds) {
      val submissionsToStep = submissionsList.filter { it.step == stepId }
      putToSubmissions(stepId, submissionsToStep.toMutableList())
    }
  }

  override fun getAllSubmissions(stepIds: Set<Int>): List<Submission>? {
    return getSubmissionsFromMemory(stepIds) ?: HyperskillConnector.getInstance().getSubmissions(stepIds, this)
  }

  private fun getSubmissionsFromMemory(stepIds: Set<Int>): List<Submission>? {
    val submissionsFromMemory = mutableListOf<Submission>()
    for (stepId in stepIds) {
      val submissionsByStep = submissions[stepId] ?: return null
      submissionsFromMemory.addAll(submissionsByStep)
    }
    return if (submissionsFromMemory.isEmpty()) null
    else {
      submissionsFromMemory.sortedByDescending { it.time }.toList()
    }
  }

  public override fun loadAllSubmissions(project: Project, course: Course?) {
    if (!submissionsCanBeShown(course)|| !isLoggedIn()) return
      ApplicationManager.getApplication().executeOnPooledThread {
        val stepIds = HyperskillSolutionLoader.getInstance(project).provideTasksToUpdate(course!!).map { it.id }.toSet()
        getAllSubmissions(stepIds)
        ApplicationManager.getApplication().invokeLater { TaskDescriptionView.getInstance(project).updateSubmissionsTab() }
      }
  }

  override fun getAllSubmissions(stepId: Int): MutableList<Submission> {
    return getAllSubmissions(setOf(stepId))?.toMutableList() ?: mutableListOf()
  }

  override fun submissionsCanBeShown(course: Course?): Boolean {
    return course is HyperskillCourse && course.isStudy
  }

  override fun getPlatformName(): String = EduNames.JBA

  override fun isLoggedIn(): Boolean = HyperskillSettings.INSTANCE.account != null

  override fun doAuthorize() {
    HyperskillConnector.getInstance().doAuthorize()
    EduCounterUsageCollector.loggedIn(HYPERSKILL, EduCounterUsageCollector.AuthorizationPlace.SUBMISSIONS_TAB)
  }
}


