package com.jetbrains.edu.learning.stepik

import com.google.common.annotations.VisibleForTesting
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindowManager
import com.jetbrains.edu.learning.configuration.SUBMISSIONS_TAB_NAME
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.stepik.api.Submission
import com.jetbrains.edu.learning.taskDescription.ui.AdditionalTabPanel
import com.jetbrains.edu.learning.taskDescription.ui.TaskDescriptionToolWindowFactory
import java.util.concurrent.ConcurrentHashMap

abstract class SubmissionsManager {
  val submissions = ConcurrentHashMap<Int, MutableList<Submission>>()

  fun getSubmissionsFromMemory(taskId: Int): List<Submission>? {
    return submissions[taskId]
  }

  fun putToSubmissions(taskId: Int, submissionsToAdd: MutableList<Submission>) {
    submissions[taskId] = submissionsToAdd
  }

  fun addToSubmissionsMap(taskId: Int, submission: Submission?) {
    if (submission == null) return
    val submissionsList = StepikSubmissionsManager.submissions.getOrPut(taskId) { mutableListOf(submission) }
    if (!submissionsList.contains(submission)) {
      submissionsList.add(submission)
      submissionsList.sortByDescending { it.time }
      //potential race when loading submissions and checking task at one time
    }
  }

  fun prepareSubmissionsContent(project: Project, course: Course, loadSubmissions: (course: Course) -> Unit) {
    val window = ToolWindowManager.getInstance(project).getToolWindow(
      TaskDescriptionToolWindowFactory.STUDY_TOOL_WINDOW)
    if (window != null) {
      val submissionsContent = window.contentManager.findContent(SUBMISSIONS_TAB_NAME)
      if (submissionsContent != null) {
        val submissionsPanel = submissionsContent.component
        if (submissionsPanel is AdditionalTabPanel) {
          ApplicationManager.getApplication().invokeLater { submissionsPanel.addLoadingPanel() }
        }
      }
    }
    loadSubmissions(course)
  }

  @VisibleForTesting
  fun clear() {
    submissions.clear()
  }
}