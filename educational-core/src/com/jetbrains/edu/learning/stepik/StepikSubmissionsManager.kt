package com.jetbrains.edu.learning.stepik

import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.ext.allTasks
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.courseFormat.tasks.TheoryTask
import com.jetbrains.edu.learning.stepik.api.Reply
import com.jetbrains.edu.learning.stepik.api.StepikConnector
import com.jetbrains.edu.learning.stepik.api.Submission

object StepikSubmissionsManager : SubmissionsManager() {

  @JvmStatic
  fun loadMissingSubmissions(course: Course) {
    val newTasks = course.allTasks.filter { !submissions.containsKey(it.id) }
    for (task in newTasks) {
      getAllSubmissions(task.id)
    }
  }

  @JvmStatic
  fun getSubmissions(taskId: Int, isSolved: Boolean): List<Submission> {
    val status = if (isSolved) EduNames.CORRECT else EduNames.WRONG
    return getAllSubmissions(taskId).filter { it.status == status }
  }

  @JvmStatic
  fun getAllSubmissions(taskId: Int): MutableList<Submission> {
    return submissions.getOrPut(taskId) { StepikConnector.getInstance().getAllSubmissions(taskId) }
  }

  private fun getLastSubmission(taskId: Int, isSolved: Boolean): Submission? {
    val submissions = getSubmissions(taskId, isSolved)
    return submissions.firstOrNull()
  }

  @JvmStatic
  fun getLastSubmissionReply(taskId: Int, isSolved: Boolean): Reply? {
    return getLastSubmission(taskId, isSolved)?.reply
  }

  @JvmStatic
  fun addToSubmissions(taskId: Int, submission: Submission?) {
    super.addToSubmissionsMap(taskId, submission)
  }

  @JvmStatic
  fun isLastSubmissionUpToDate(task: Task, isSolved: Boolean): Boolean {
    if (task is TheoryTask) return true
    val submission = getLastSubmission(task.id, isSolved)
                     ?: return false
    return submission.time?.after(task.updateDate) ?: false
  }

  @JvmStatic
  fun prepareSubmissionsContent(project: Project, course: Course, loadSubmissionsFromStepik: () -> Unit) {
    super.prepareSubmissionsContent(project, course, StepikNames.STEPIK) { loadSubmissionsFromStepik() }
  }
}
