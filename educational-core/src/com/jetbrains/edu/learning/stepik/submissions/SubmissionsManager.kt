package com.jetbrains.edu.learning.stepik.submissions

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.stepik.api.Submission
import com.jetbrains.edu.learning.taskDescription.ui.TaskDescriptionView
import org.jetbrains.annotations.TestOnly
import java.util.concurrent.ConcurrentHashMap
import java.util.stream.Collectors

/**
 * Stores and returns submissions for courses with submissions support if they are already loaded or delegates loading
 * to SubmissionsProvider.
 *
 * @see com.jetbrains.edu.learning.stepik.submissions.SubmissionsProvider
 */
@Service
class SubmissionsManager(private val project: Project) {
  private val submissions = ConcurrentHashMap<Int, MutableList<Submission>>()
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

  fun getSubmissions(stepIds: Set<Int>): List<Submission>? {
    val course = this.course
    val submissionsFromMemory = getSubmissionsFromMemory(stepIds)
    return if (submissionsFromMemory != null) submissionsFromMemory
    else {
      if (course == null) return null
      val submissionsProvider = SubmissionsProvider.getSubmissionsProviderForCourse(course) ?: return null
      val submissionsById = submissionsProvider.loadSubmissions(stepIds)
      submissions.putAll(submissionsById)
      ApplicationManager.getApplication().invokeLater { TaskDescriptionView.getInstance(project).updateSubmissionsTab() }
      submissionsById.values.stream()
        .flatMap(List<Submission>::stream)
        .collect(Collectors.toList())
    }
  }

  fun getSubmissions(stepId: Int, isSolved: Boolean): List<Submission>? {
    val status = if (isSolved) EduNames.CORRECT else EduNames.WRONG
    return getOrLoadSubmissions(stepId).filter { it.status == status }
  }

  fun getSubmission(stepId: Int, submissionId: Int): Submission? {
    return getOrLoadSubmissions(stepId).find { it.id == submissionId }
  }

  private fun getOrLoadSubmissions(stepId: Int): List<Submission> {
    val submissionsProvider = course?.getSubmissionsProvider() ?: return emptyList()
    val submissionsList = submissions[stepId]
    return if (submissionsList != null) {
      submissionsList
    }
    else {
      val loadedSubmissions = submissionsProvider.loadSubmissions(setOf(stepId))
      submissions.putAll(loadedSubmissions)
      ApplicationManager.getApplication().invokeLater { TaskDescriptionView.getInstance(project).updateSubmissionsTab() }
      return loadedSubmissions[stepId] ?: emptyList()
    }
  }

  fun addToSubmissions(taskId: Int, submission: Submission) {
    val submissionsList = submissions.getOrPut(taskId) { mutableListOf(submission) }
    if (!submissionsList.contains(submission)) {
      submissionsList.add(submission)
      submissionsList.sortByDescending { it.time }
      //potential race when loading submissions and checking task at one time
    }
    ApplicationManager.getApplication().invokeLater { TaskDescriptionView.getInstance(project).updateSubmissionsTab() }
  }

  fun addToSubmissionsWithStatus(taskId: Int, checkStatus: CheckStatus, submission: Submission?) {
    if (submission == null || checkStatus == CheckStatus.Unchecked) return
    submission.status = if (checkStatus == CheckStatus.Solved) EduNames.CORRECT else EduNames.WRONG
    addToSubmissions(taskId, submission)
  }

  fun submissionsSupported(): Boolean {
    val course = this.course
    if (course == null) return false
    val submissionsProvider = SubmissionsProvider.getSubmissionsProviderForCourse(
      course) ?: return false
    return submissionsProvider.areSubmissionsAvailable(course)
  }

  fun prepareSubmissionsContent(loadSolutions: () -> Unit = {}) {
    val course = this.course
    val submissionsProvider = course?.getSubmissionsProvider() ?: return

    val taskDescriptionView = TaskDescriptionView.getInstance(project)
    taskDescriptionView.addLoadingPanel(getPlatformName())

    ApplicationManager.getApplication().executeOnPooledThread {
      submissions.putAll(submissionsProvider.loadAllSubmissions(project, course))
      loadSolutions()
      ApplicationManager.getApplication().invokeLater { taskDescriptionView.updateSubmissionsTab() }
    }
  }

  fun isLoggedIn(): Boolean = course?.getSubmissionsProvider()?.isLoggedIn() ?: false

  fun getPlatformName(): String = course?.getSubmissionsProvider()?.getPlatformName() ?: error("Failed to get platform Name")

  fun doAuthorize() = course?.getSubmissionsProvider()?.doAuthorize()

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