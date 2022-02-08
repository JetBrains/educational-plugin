package com.jetbrains.edu.learning.submissions

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.isUnitTestMode
import com.jetbrains.edu.learning.taskDescription.ui.TaskDescriptionView
import com.jetbrains.edu.learning.taskDescription.ui.tab.TabType.SUBMISSIONS_TAB
import org.jetbrains.annotations.TestOnly
import java.util.concurrent.ConcurrentHashMap
import java.util.stream.Collectors

/**
 * Stores and returns submissions for courses with submissions support if they are already loaded or delegates loading
 * to SubmissionsProvider.
 *
 * @see com.jetbrains.edu.learning.submissions.SubmissionsProvider
 */
@Service
class SubmissionsManager(private val project: Project) {
  private val submissions = ConcurrentHashMap<Int, List<SubmissionBase>>()
  var course: Course? = project.course
    @TestOnly set

  fun getSubmissionsFromMemory(stepIds: Set<Int>): List<SubmissionBase>? {
    val submissionsFromMemory = mutableListOf<SubmissionBase>()
    for (stepId in stepIds) {
      val submissionsByStep = submissions[stepId] ?: return null
      submissionsFromMemory.addAll(submissionsByStep)
    }
    return submissionsFromMemory.sortedByDescending { it.time }.toList()
  }

  fun getSubmissions(tasks: List<Task>): List<SubmissionBase>? {
    val course = this.course
    val stepIds = tasks.stream().map { task -> task.id }.collect(Collectors.toSet())
    val submissionsFromMemory = getSubmissionsFromMemory(stepIds)
    return if (submissionsFromMemory != null) submissionsFromMemory
    else {
      if (course == null) return null
      val submissionsProvider = SubmissionsProvider.getSubmissionsProviderForCourse(course) ?: return null
      val submissionsById = submissionsProvider.loadSubmissions(tasks, course.id)
      submissions.putAll(submissionsById)
      updateSubmissionsTab()
      submissionsById.values.flatten()
    }
  }

  fun getSubmissions(task: Task): List<SubmissionBase> {
    return getOrLoadSubmissions(task)
  }

  fun getSubmission(task: Task, submissionId: Int): SubmissionBase? {
    return getOrLoadSubmissions(task).find { it.id == submissionId }
  }

  private fun getOrLoadSubmissions(task: Task): List<SubmissionBase> {
    val course = this.course ?: return emptyList()
    val submissionsProvider = course.getSubmissionsProvider() ?: return emptyList()
    val submissionsList = submissions[task.id]
    return if (submissionsList != null) {
      submissionsList
    }
    else {
      val loadedSubmissions = submissionsProvider.loadSubmissions(listOf(task), course.id)
      submissions.putAll(loadedSubmissions)
      updateSubmissionsTab()
      return loadedSubmissions[task.id] ?: emptyList()
    }
  }

  fun addToSubmissions(taskId: Int, submission: SubmissionBase) {
    val submissionsList = submissions.getOrPut(taskId) { listOf(submission) }.toMutableList()
    if (!submissionsList.contains(submission)) {
      submissionsList.add(submission)
      submissionsList.sortByDescending { it.time }
      submissions[taskId] = submissionsList
      //potential race when loading submissions and checking task at one time
    }
    updateSubmissionsTab()
  }

  private fun updateSubmissionsTab() {
    ApplicationManager.getApplication().invokeLater { TaskDescriptionView.getInstance(project).updateTab(SUBMISSIONS_TAB) }
  }

  fun containsCorrectSubmission(stepId: Int): Boolean {
    val submissions = getSubmissionsFromMemory(setOf(stepId)) ?: return false
    return submissions.any { it.status == EduNames.CORRECT }
  }

  fun addToSubmissionsWithStatus(taskId: Int, checkStatus: CheckStatus, submission: SubmissionBase?) {
    if (submission == null || checkStatus == CheckStatus.Unchecked) return
    submission.status = if (checkStatus == CheckStatus.Solved) EduNames.CORRECT else EduNames.WRONG
    addToSubmissions(taskId, submission)
  }

  fun submissionsSupported(): Boolean {
    val course = this.course
    if (course == null) return false
    val submissionsProvider = SubmissionsProvider.getSubmissionsProviderForCourse(course) ?: return false
    return submissionsProvider.areSubmissionsAvailable(course)
  }

  fun prepareSubmissionsContent(loadSolutions: () -> Unit = {}) {
    val course = this.course
    val submissionsProvider = course?.getSubmissionsProvider() ?: return

    val taskDescriptionView = TaskDescriptionView.getInstance(project)
    taskDescriptionView.showLoadingSubmissionsPanel(getPlatformName())

    if (!isUnitTestMode) {
      ApplicationManager.getApplication().executeOnPooledThread {
        loadSubmissionsContent(course, submissionsProvider, taskDescriptionView, loadSolutions)
      }
    }
    else {
      loadSubmissionsContent(course, submissionsProvider, taskDescriptionView, loadSolutions)
    }
  }

  fun isLoggedIn(): Boolean = course?.getSubmissionsProvider()?.isLoggedIn() ?: false

  fun getPlatformName(): String = course?.getSubmissionsProvider()?.getPlatformName() ?: error("Failed to get platform Name")

  fun doAuthorize() = course?.getSubmissionsProvider()?.doAuthorize()

  private fun loadSubmissionsContent(course: Course,
                                     submissionsProvider: SubmissionsProvider,
                                     taskDescriptionView: TaskDescriptionView,
                                     loadSolutions: () -> Unit) {
    submissions.putAll(submissionsProvider.loadAllSubmissions(project, course))
    loadSolutions()
    ApplicationManager.getApplication().invokeLater { taskDescriptionView.updateTab(SUBMISSIONS_TAB) }
  }

  private fun Course.getSubmissionsProvider(): SubmissionsProvider? {
    return SubmissionsProvider.getSubmissionsProviderForCourse(this)
  }

  @TestOnly
  fun clear() {
    submissions.clear()
  }

  companion object {

    @JvmStatic
    fun getInstance(project: Project): SubmissionsManager {
      return project.service()
    }
  }
}