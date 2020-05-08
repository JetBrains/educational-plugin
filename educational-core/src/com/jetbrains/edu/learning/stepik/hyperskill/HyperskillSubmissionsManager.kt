package com.jetbrains.edu.learning.stepik.hyperskill

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.statistics.EduCounterUsageCollector
import com.jetbrains.edu.learning.stepik.SubmissionsManager
import com.jetbrains.edu.learning.stepik.api.Submission
import com.jetbrains.edu.learning.stepik.hyperskill.api.HyperskillConnector
import com.jetbrains.edu.learning.stepik.hyperskill.courseFormat.HyperskillCourse
import com.jetbrains.edu.learning.stepik.hyperskill.settings.HyperskillSettings
import com.jetbrains.edu.learning.taskDescription.ui.TaskDescriptionView

object HyperskillSubmissionsManager : SubmissionsManager() {

  fun getLastSubmission(taskId: Int): Submission? {
    val submissionsList = submissions[taskId] ?: return null
    if(submissionsList.isEmpty()) return null
    submissionsList.sortedByDescending { it.time }
    return submissionsList[0]
  }

  public override fun loadAllSubmissions(project: Project, course: Course?) {
    if (course !is HyperskillCourse || !isLoggedIn()) return
    if (course.isStudy && HyperskillSettings.INSTANCE.account != null) {
      ApplicationManager.getApplication().executeOnPooledThread {
        val stages = course.stages
        for (stage in stages) {
          getAllSubmissions(stage.stepId)
        }
        ApplicationManager.getApplication().invokeLater { TaskDescriptionView.getInstance(project).updateSubmissionsTab() }
      }
    }
  }

  override fun loadAllSubmissions(stepId: Int): MutableList<Submission> {
    return HyperskillConnector.getInstance().getAllSubmissions(stepId)
  }

  override fun submissionsCanBeShown(course: Course): Boolean {
    return course is HyperskillCourse && course.isStudy
  }

  override fun platformName(): String = EduNames.JBA

  override fun isLoggedIn(): Boolean = HyperskillSettings.INSTANCE.account != null

  override fun doAuthorize() {
    HyperskillConnector.getInstance().doAuthorize()
    EduCounterUsageCollector.loggedIn(HYPERSKILL, EduCounterUsageCollector.AuthorizationPlace.SUBMISSIONS_TAB)
  }
}


