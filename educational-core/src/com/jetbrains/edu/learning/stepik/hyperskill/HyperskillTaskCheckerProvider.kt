package com.jetbrains.edu.learning.stepik.hyperskill

import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.checker.CheckResult
import com.jetbrains.edu.learning.checker.TaskChecker
import com.jetbrains.edu.learning.checker.TaskCheckerProvider
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.tasks.EduTask
import com.jetbrains.edu.learning.stepik.hyperskill.courseFormat.HyperskillCourse

class HyperskillTaskCheckerProvider(private val baseProvider: TaskCheckerProvider) : TaskCheckerProvider by baseProvider {
  override fun getEduTaskChecker(task: EduTask, project: Project): TaskChecker<EduTask> {
    val checker = baseProvider.getEduTaskChecker(task, project)

    return object : TaskChecker<EduTask>(task, project) {
      override fun check(indicator: ProgressIndicator): CheckResult {
        val checkResult = checker.check(indicator)
        val course = task.course
        if (checkResult.status == CheckStatus.Solved && course is HyperskillCourse) {
          return CheckResult(checkResult.status, SUCCESS_MESSAGE, needEscape = false)
        }
        return checkResult
      }

      // Can't use Kotlin delegation feature (https://kotlinlang.org/docs/reference/delegation.html)
      // because TaskChecker is abstract class, not an interface
      override fun onTaskSolved(message: String) = checker.onTaskSolved(message)
      override fun onTaskFailed(message: String, details: String?) = checker.onTaskFailed(message, details)
      override fun clearState() = checker.clearState()
    }
  }
}