package com.jetbrains.edu.learning.submissions

import com.intellij.execution.process.ProcessIOExecutorService
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.EduFormatNames.CORRECT
import com.jetbrains.edu.learning.courseFormat.ext.allTasks
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.invokeLater
import com.jetbrains.edu.learning.marketplace.api.MarketplaceSubmission
import com.jetbrains.edu.learning.taskToolWindow.ui.TaskToolWindowView
import com.jetbrains.edu.learning.taskToolWindow.ui.tab.TabType.SUBMISSIONS_TAB
import org.jetbrains.annotations.TestOnly
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.stream.Collectors

/**
 * Stores and returns submissions for courses with submissions support if they are already loaded or delegates loading
 * to SubmissionsProvider.
 *
 * @see com.jetbrains.edu.learning.submissions.SubmissionsProvider
 */
@Service(Service.Level.PROJECT)
class SubmissionsManager(private val project: Project) {

  private val submissions = ConcurrentHashMap<Int, List<Submission>>()

  private val communitySubmissions = ConcurrentHashMap<Int, List<Submission>>()

  var course: Course? = project.course
    @TestOnly set

  fun getSubmissionsFromMemory(stepIds: Set<Int>): List<Submission>? {
    val submissionsFromMemory = mutableListOf<Submission>()
    for (stepId in stepIds) {
      val submissionsByStep = submissions[stepId] ?: return null
      submissionsFromMemory.addAll(submissionsByStep)
    }
    return submissionsFromMemory.sortedByDescending { it.time }.toList()
  }

  fun getSubmissions(tasks: List<Task>): List<Submission>? {
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

  fun getSubmissions(task: Task): List<Submission> {
    return getOrLoadSubmissions(task)
  }

  fun getSubmissionWithSolutionText(task: Task, submissionId: Int): Submission? {
    val submission = getOrLoadSubmissions(task).find { it.id == submissionId } ?: return null

    if (submission is MarketplaceSubmission && submission.solutionFiles == null) {
      val course = this.course ?: return null
      val submissionsProvider = course.getSubmissionsProvider() ?: return null
      submissionsProvider.loadSolutionFiles(submission)
    }
    return submission
  }

  private fun getOrLoadSubmissions(task: Task): List<Submission> {
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

  fun addToSubmissions(taskId: Int, submission: Submission) {
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
    project.invokeLater { TaskToolWindowView.getInstance(project).updateTab(SUBMISSIONS_TAB) }
  }

  fun containsCorrectSubmission(stepId: Int): Boolean {
    val submissions = getSubmissionsFromMemory(setOf(stepId)) ?: return false
    return submissions.any { it.status == CORRECT }
  }

  fun addToSubmissionsWithStatus(taskId: Int, checkStatus: CheckStatus, submission: Submission?) {
    if (submission == null || checkStatus == CheckStatus.Unchecked) return
    submission.status = checkStatus.rawStatus
    addToSubmissions(taskId, submission)
  }

  fun submissionsSupported(): Boolean {
    val course = this.course
    if (course == null) return false
    val submissionsProvider = SubmissionsProvider.getSubmissionsProviderForCourse(course) ?: return false
    return submissionsProvider.areSubmissionsAvailable(course)
  }

  fun prepareSubmissionsContentWhenLoggedIn(loadSolutions: () -> Unit = {}) {
    val course = this.course
    val submissionsProvider = course?.getSubmissionsProvider() ?: return

    CompletableFuture.runAsync({
      if (isLoggedIn()) {
        val taskToolWindowView = TaskToolWindowView.getInstance(project)
        taskToolWindowView.showLoadingSubmissionsPanel(getPlatformName())
        loadSubmissionsContent(course, submissionsProvider, taskToolWindowView, loadSolutions)
      }
    }, ProcessIOExecutorService.INSTANCE)
  }

  fun loadCommunitySubmissions() {
    val course = this.course
    val submissionsProvider = course?.getSubmissionsProvider() ?: return

    CompletableFuture.runAsync({
      if (isLoggedIn()) {
        communitySubmissions.putAll(submissionsProvider.loadSharedSolutionsForCourse(course))
      }
    }, ProcessIOExecutorService.INSTANCE)
  }

  fun deleteCourseSubmissionsLocally() {
    course?.allTasks?.forEach { submissions.remove(it.id) }
    updateSubmissionsTab()
  }

  fun isLoggedIn(): Boolean = course?.getSubmissionsProvider()?.isLoggedIn() ?: false

  fun getPlatformName(): String = course?.getSubmissionsProvider()?.getPlatformName() ?: error("Failed to get platform Name")

  fun doAuthorize() = course?.getSubmissionsProvider()?.doAuthorize(Runnable { prepareSubmissionsContentWhenLoggedIn() })

  private fun loadSubmissionsContent(
    course: Course,
    submissionsProvider: SubmissionsProvider,
    taskToolWindowView: TaskToolWindowView,
    loadSolutions: () -> Unit
  ) {
    submissions.putAll(submissionsProvider.loadAllSubmissions(course))
    loadSolutions()
    ApplicationManager.getApplication().invokeLater { taskToolWindowView.updateTab(SUBMISSIONS_TAB) }
  }

  private fun Course.getSubmissionsProvider(): SubmissionsProvider? {
    return SubmissionsProvider.getSubmissionsProviderForCourse(this)
  }

  @TestOnly
  fun clear() {
    submissions.clear()
  }

  companion object {

    fun getInstance(project: Project): SubmissionsManager {
      return project.service()
    }
  }
}