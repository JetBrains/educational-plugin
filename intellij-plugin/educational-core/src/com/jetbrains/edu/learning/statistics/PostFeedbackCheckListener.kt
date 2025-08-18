package com.jetbrains.edu.learning.statistics

import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.checker.CheckListener
import com.jetbrains.edu.learning.courseFormat.CheckResult
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.projectView.ProgressUtil

class PostFeedbackCheckListener : CheckListener {
  override fun afterCheck(project: Project, task: Task, result: CheckResult) {
    if (task.status != CheckStatus.Solved || isLeaveFeedbackPrompted()) return
    val lesson = task.lesson
    val course = lesson.course

    val progress = ProgressUtil.countProgress(course)
    if (course.isMarketplace && progressPassed(progress)) {
      showStudentPostFeedbackNotification(project)
    }
  }

  private fun progressPassed(progress: ProgressUtil.CourseProgress): Boolean {
    return progress.tasksSolved.toFloat() / progress.tasksTotalNum.toFloat() > SOLVED_TASKS_PERCENTAGE && progress.tasksSolved > MIN_SOLVED_TASKS_NUMBER
  }

  companion object {
    private const val MIN_SOLVED_TASKS_NUMBER = 1

    private const val SOLVED_TASKS_PERCENTAGE = 0.05
  }
}
