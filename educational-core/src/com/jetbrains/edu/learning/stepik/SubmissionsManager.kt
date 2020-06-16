package com.jetbrains.edu.learning.stepik

import com.google.common.annotations.VisibleForTesting
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.ext.project
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.stepik.api.Submission
import com.jetbrains.edu.learning.taskDescription.ui.TaskDescriptionView
import java.util.concurrent.ConcurrentHashMap
import java.util.stream.Collectors

/**
 * Stores and returns submissions for courses with submissions support if they are already loaded or delegates loading
 * to SubmissionsProvider.
 *
 * @see com.jetbrains.edu.learning.stepik.SubmissionsProvider
 */
class SubmissionsManager {
  private val submissions = ConcurrentHashMap<Int, MutableList<Submission>>()

  private fun getSubmissionsFromMemory(stepIds: Set<Int>): List<Submission>? {
    val submissionsFromMemory = mutableListOf<Submission>()
    for (stepId in stepIds) {
      val submissionsByStep = submissions[stepId] ?: return null
      submissionsFromMemory.addAll(submissionsByStep)
    }
    return submissionsFromMemory.sortedByDescending { it.time }.toList()
  }

  fun getSubmissions(course: Course, stepIds: Set<Int>): List<Submission>? {
    val submissionsFromMemory = getSubmissionsFromMemory(stepIds)
    return if (submissionsFromMemory != null) submissionsFromMemory
    else {
      val submissionsProvider = SubmissionsProvider.getSubmissionsProviderForCourse(course) ?: return null
      val submissionsById = submissionsProvider.loadSubmissions(stepIds)
      submissions.putAll(submissionsById)
      course.updateSubmissionsTab()
      submissionsById.values.stream()
        .flatMap(List<Submission>::stream)
        .collect(Collectors.toList())
    }
  }

  fun getSubmissions(task: Task, isSolved: Boolean): List<Submission>? {
    val status = if (isSolved) EduNames.CORRECT else EduNames.WRONG
    return getOrLoadSubmissions(task.course, task.id).filter { it.status == status }
  }

  fun getSubmissions(task: Task): List<Submission>? {
    return getOrLoadSubmissions(task.course, task.id)
  }

  fun getSubmission(course: Course, stepId: Int, submissionId: Int): Submission? {
    return getOrLoadSubmissions(course, stepId).find { it.id == submissionId }
  }

  private fun getOrLoadSubmissions(course: Course, stepId: Int): List<Submission> {
    val submissionsProvider = course.getSubmissionsProvider() ?: return emptyList()
    val submissionsList = submissions[stepId]
    return if (submissionsList != null) {
      submissionsList
    }
    else {
      val loadedSubmissions = submissionsProvider.loadStepSubmissions(stepId).toMutableList()
      submissions[stepId] = loadedSubmissions
      course.updateSubmissionsTab()
      loadedSubmissions
    }
  }

  fun addToSubmissions(project: Project, taskId: Int, submission: Submission) {
    val submissionsList = submissions.getOrPut(taskId) { mutableListOf(submission) }
    if (!submissionsList.contains(submission)) {
      submissionsList.add(submission)
      submissionsList.sortByDescending { it.time }
      //potential race when loading submissions and checking task at one time
    }
    ApplicationManager.getApplication().invokeLater { TaskDescriptionView.getInstance(project).updateSubmissionsTab() }
  }

  fun addToSubmissionsWithStatus(project: Project, taskId: Int, checkStatus: CheckStatus, submission: Submission?) {
    if (submission == null || checkStatus == CheckStatus.Unchecked) return
    submission.status = if (checkStatus == CheckStatus.Solved) EduNames.CORRECT else EduNames.WRONG
    addToSubmissions(project, taskId, submission)
  }

  fun submissionsSupported(course: Course): Boolean {
    val submissionsProvider = SubmissionsProvider.getSubmissionsProviderForCourse(course) ?: return false
    return submissionsProvider.submissionsCanBeShown(course)
  }

  fun prepareSubmissionsContent(project: Project, course: Course, loadSolutions: () -> Unit) {
    val submissionsProvider = course.getSubmissionsProvider() ?: return

    val toolWindow = TaskDescriptionView.getInstance(project).toolWindow
    if (toolWindow != null) {
      val submissionsContent = toolWindow.contentManager.findContent(EduCoreBundle.message("submissions.tab.name"))
      if (submissionsContent != null) {
        val submissionsPanel = submissionsContent.component
        if (submissionsPanel is SubmissionsTabPanel) {
          ApplicationManager.getApplication().invokeLater { submissionsPanel.addLoadingPanel(submissionsProvider.getPlatformName()) }
        }
      }
    }

    ApplicationManager.getApplication().executeOnPooledThread {
      submissions.putAll(submissionsProvider.loadAllSubmissions(project, course))
      loadSolutions()
      ApplicationManager.getApplication().invokeLater { TaskDescriptionView.getInstance(project).updateSubmissionsTab() }
    }
  }

  fun isLoggedIn(course: Course): Boolean = course.getSubmissionsProvider()?.isLoggedIn() ?: false

  fun getPlatformName(course: Course): String = course.getSubmissionsProvider()?.getPlatformName() ?: error("Failed to get platform Name")

  fun doAuthorize(course: Course) = course.getSubmissionsProvider()?.doAuthorize()

  private fun Course.getSubmissionsProvider(): SubmissionsProvider? {
    return SubmissionsProvider.getSubmissionsProviderForCourse(this)
  }

  private fun Course.updateSubmissionsTab() {
    val project = course.project ?: return
    ApplicationManager.getApplication().invokeLater { TaskDescriptionView.getInstance(project).updateSubmissionsTab() }
  }

  companion object {

    @JvmStatic
    fun getInstance(project: Project): SubmissionsManager = ServiceManager.getService(project, SubmissionsManager::class.java)
  }

  @VisibleForTesting
  fun clear() {
    submissions.clear()
  }
}