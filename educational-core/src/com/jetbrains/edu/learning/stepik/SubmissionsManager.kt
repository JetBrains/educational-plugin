package com.jetbrains.edu.learning.stepik

import com.google.common.annotations.VisibleForTesting
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.courseFormat.tasks.TheoryTask
import com.jetbrains.edu.learning.stepik.api.Reply
import com.jetbrains.edu.learning.stepik.api.Submission
import java.util.concurrent.ConcurrentHashMap

class SubmissionsManager {
  private val submissions = ConcurrentHashMap<Int, MutableList<Submission>>()

  fun putToSubmissions(taskId: Int, submissionsToAdd: MutableList<Submission>) {
    submissions[taskId] = submissionsToAdd
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

  fun putToSubmissions(stepIds: Set<Int>, submissionsList: List<Submission>?): List<Submission>? {
    if (submissionsList == null) return submissionsList
    for (stepId in stepIds) {
      putToSubmissions(stepId, submissionsList)
    }
    return submissionsList
  }

  fun putToSubmissions(stepId: Int, submissionsList: List<Submission>?): List<Submission>? {
    if (submissionsList == null) return submissionsList
    val submissionsToStep = submissionsList.filter { it.step == stepId }
    putToSubmissions(stepId, submissionsToStep.toMutableList())
    return submissionsList
  }

  fun addToSubmissionsMapWithStatus(taskId: Int, checkStatus: CheckStatus, submission: Submission?) {
    if (submission == null || checkStatus == CheckStatus.Unchecked) return
    submission.status = if (checkStatus == CheckStatus.Solved) EduNames.CORRECT else EduNames.WRONG
    addToSubmissionsMap(taskId, submission)
  }

  fun addToSubmissionsMap(taskId: Int, submission: Submission?) {
    if (submission == null) return
    val submissionsList = submissions.getOrPut(taskId) { mutableListOf(submission) }
    if (!submissionsList.contains(submission)) {
      submissionsList.add(submission)
      submissionsList.sortByDescending { it.time }
      //potential race when loading submissions and checking task at one time
    }
  }

  fun getSubmissionsFromMemory(taskId: Int): List<Submission>? {
    return submissions[taskId]
  }

  fun getOrPut(stepId: Int, loadSubmissions: () -> List<Submission>): List<Submission> {
    return submissions.getOrPut(stepId) { loadSubmissions().toMutableList() }
  }

  fun contains(stepId: Int): Boolean = submissions.containsKey(stepId)

  private fun getLastSubmission(task: Task, isSolved: Boolean): Submission? {
    val submissions = getSubmissions(task, isSolved)
    return submissions.firstOrNull()
  }

  fun getSubmissions(task: Task, isSolved: Boolean): List<Submission> {
    val status = if (isSolved) EduNames.CORRECT else EduNames.WRONG
    val submissionsProvider = SubmissionsProvider.getSubmissionsProviderForCourse(task.course) ?: error(
      "SubmissionsProvider for course ${task.course.name} not found")
    return submissionsProvider.getSubmissions(task.id, this).filter { it.status == status }
  }

  fun isLastSubmissionUpToDate(task: Task, isSolved: Boolean): Boolean {
    if (task is TheoryTask) return true
    val submission = getLastSubmission(task, isSolved) ?: return false
    return submission.time?.after(task.updateDate) ?: false
  }

  fun getLastSubmissionReply(task: Task, isSolved: Boolean): Reply? {
    return getLastSubmission(task, isSolved)?.reply
  }

  fun submissionsSupported(course: Course): Boolean {
    val submissionsProvider = SubmissionsProvider.getSubmissionsProviderForCourse(course) ?: return false
    return submissionsProvider.submissionsCanBeShown(course)
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