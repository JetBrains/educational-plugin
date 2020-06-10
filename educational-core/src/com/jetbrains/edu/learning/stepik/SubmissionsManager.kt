package com.jetbrains.edu.learning.stepik

import com.google.common.annotations.VisibleForTesting
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindowManager
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.courseFormat.tasks.TheoryTask
import com.jetbrains.edu.learning.stepik.api.Submission
import com.jetbrains.edu.learning.taskDescription.ui.AdditionalTabPanel
import com.jetbrains.edu.learning.taskDescription.ui.TaskDescriptionToolWindowFactory
import java.util.concurrent.ConcurrentHashMap

/**
 * Stores and returns submissions for courses with submissions support if they are already loaded or delegates loading
 * to SubmissionsProvider.
 *
 * @see com.jetbrains.edu.learning.stepik.SubmissionsProvider
 */
class SubmissionsManager {
  private val submissions = ConcurrentHashMap<Int, MutableList<Submission>>()

  fun putToSubmissions(stepIds: Set<Int>, submissionsList: List<Submission>): List<Submission> {
    for (stepId in stepIds) {
      val submissionsToStep = submissionsList.filter { it.step == stepId }
      submissions[stepId] = submissionsToStep.toMutableList()
    }
    return submissionsList
  }

  fun getSubmissionsFromMemory(stepIds: Set<Int>): List<Submission>? {
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

  fun getSubmissions(course: Course, stepIds: Set<Int>): List<Submission>? {
    val submissionsForSteps = mutableListOf<Submission>()
    for (stepId in stepIds) {
      submissionsForSteps.addAll(getOrLoadSubmissions(course, stepId))
    }
    return submissionsForSteps
  }

  fun getSubmissions(task: Task, isSolved: Boolean): List<Submission>? {
    val status = if (isSolved) EduNames.CORRECT else EduNames.WRONG
    return getOrLoadSubmissions(task.course, task.id).filter { it.status == status }
  }

  fun getLastSubmission(task: Task, isSolved: Boolean): Submission? {
    val submissions = getSubmissions(task, isSolved) ?: return null
    return submissions.firstOrNull()
  }

  private fun getOrLoadSubmissions(course: Course, stepId: Int): List<Submission> {
    val submissionsProvider = course.getSubmissionsProvider() ?: return emptyList()
    return submissions.getOrPut(stepId) { submissionsProvider.loadSubmissions(stepId).toMutableList() }
  }

  fun addToSubmissions(taskId: Int, submission: Submission?) {
    if (submission == null) return
    val submissionsList = submissions.getOrPut(taskId) { mutableListOf(submission) }
    if (!submissionsList.contains(submission)) {
      submissionsList.add(submission)
      submissionsList.sortByDescending { it.time }
      //potential race when loading submissions and checking task at one time
    }
  }

  fun addToSubmissionsWithStatus(taskId: Int, checkStatus: CheckStatus, submission: Submission?) {
    if (submission == null || checkStatus == CheckStatus.Unchecked) return
    submission.status = if (checkStatus == CheckStatus.Solved) EduNames.CORRECT else EduNames.WRONG
    addToSubmissions(taskId, submission)
  }

  fun isLastSubmissionUpToDate(task: Task, isSolved: Boolean): Boolean {
    if (task is TheoryTask) return true
    val submission = getLastSubmission(task, isSolved) ?: return false
    return submission.time?.after(task.updateDate) ?: false
  }

  fun submissionsSupported(course: Course): Boolean {
    val submissionsProvider = SubmissionsProvider.getSubmissionsProviderForCourse(course) ?: return false
    return submissionsProvider.submissionsCanBeShown(course)
  }

  fun prepareSubmissionsContent(project: Project, course: Course, loadSolutions: () -> Unit) {
    val window = ToolWindowManager.getInstance(project).getToolWindow(TaskDescriptionToolWindowFactory.STUDY_TOOL_WINDOW)
    val submissionsProvider = course.getSubmissionsProvider() ?: return
    if (window != null) {
      val submissionsContent = window.contentManager.findContent(SUBMISSIONS_TAB_NAME)
      if (submissionsContent != null) {
        val submissionsPanel = submissionsContent.component
        if (submissionsPanel is AdditionalTabPanel) {
          ApplicationManager.getApplication().invokeLater { submissionsPanel.addLoadingPanel(submissionsProvider.getPlatformName()) }
        }
      }
    }
    submissionsProvider.loadAllSubmissions(project, course) { loadSolutions() }
  }

  private fun Course.getSubmissionsProvider(): SubmissionsProvider? {
    return SubmissionsProvider.getSubmissionsProviderForCourse(this)
  }

  companion object {
    const val SUBMISSIONS_TAB_NAME = "Submissions"

    @JvmStatic
    fun getInstance(project: Project): SubmissionsManager = ServiceManager.getService(project, SubmissionsManager::class.java)
  }

  @VisibleForTesting
  fun clear() {
    submissions.clear()
  }
}