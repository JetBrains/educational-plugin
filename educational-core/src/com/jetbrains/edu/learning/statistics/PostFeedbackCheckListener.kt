package com.jetbrains.edu.learning.statistics

import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.checker.CheckListener
import com.jetbrains.edu.learning.checker.CheckResult
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.projectView.ProgressUtil

class PostFeedbackCheckListener : CheckListener {
  override fun afterCheck(project: Project, task: Task, result: CheckResult) {
    if (isQuestionnaireAdvertisingNotificationShown()) return
    val lesson = task.lesson
    val course = lesson.course

    val progress = ProgressUtil.countProgress(course)
    val solvedTasks = progress.first
    val allTasks = progress.second
    // TODO: remove after we get enough students contacts for RSCH-3470 Conduct interviews with Edu Marketplace Plugins users[https://youtrack.jetbrains.com/issue/RSCH-3470]
    if (course.isMarketplace && progressPassed(solvedTasks, allTasks)) {
      showQuestionnaireAdvertisingNotification(project, course)
    }
    else if (solvedTasks == lesson.taskList.size && !isFeedbackAsked()) {
      showPostFeedbackNotification(true, course, project)
    }
  }

  private fun progressPassed(solvedTasks: Int, allTasks: Int): Boolean {
    return solvedTasks.toFloat() / allTasks.toFloat() > SOLVED_TASKS_PERCENTAGE || solvedTasks > MIN_SOLVED_TASKS_NUMBER
  }

  companion object {
    private const val MIN_SOLVED_TASKS_NUMBER = 1
    
    private const val SOLVED_TASKS_PERCENTAGE = 0.05
  }
}
