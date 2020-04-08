package com.jetbrains.edu.learning.stepik.hyperskill.checker

import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.checker.*
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.tasks.*
import com.jetbrains.edu.learning.courseFormat.tasks.choice.ChoiceTask
import com.jetbrains.edu.learning.stepik.hyperskill.courseFormat.HyperskillCourse
import com.jetbrains.edu.learning.stepik.hyperskill.successMessage

class HyperskillTaskCheckerProvider(private val baseProvider: TaskCheckerProvider) : TaskCheckerProvider {
  override fun getEduTaskChecker(task: EduTask, project: Project): TaskChecker<EduTask> {
    val checker = baseProvider.getEduTaskChecker(task, project)

    return object : TaskChecker<EduTask>(task, project) {
      override fun check(indicator: ProgressIndicator): CheckResult {
        val checkResult = checker.check(indicator)
        val course = task.course
        if (checkResult.status == CheckStatus.Solved && course is HyperskillCourse) {
          return CheckResult(checkResult.status, task.successMessage, needEscape = false)
        }
        return checkResult
      }

      override fun onTaskSolved(message: String) = checker.onTaskSolved(message)
      override fun onTaskFailed(message: String, details: String?) = checker.onTaskFailed(message, details)
      override fun clearState() = checker.clearState()
    }
  }

  override fun getOutputTaskChecker(task: OutputTask, project: Project, codeExecutor: CodeExecutor): OutputTaskChecker =
    baseProvider.getOutputTaskChecker(task, project, codeExecutor)

  override fun getTheoryTaskChecker(task: TheoryTask, project: Project): TheoryTaskChecker =
    baseProvider.getTheoryTaskChecker(task, project)

  override fun getCodeExecutor(): CodeExecutor = baseProvider.getCodeExecutor()

  override fun getChoiceTaskChecker(task: ChoiceTask, project: Project): TaskChecker<ChoiceTask>? =
    baseProvider.getChoiceTaskChecker(task, project)

  override fun getCodeTaskChecker(task: CodeTask, project: Project): TaskChecker<CodeTask>? =
    baseProvider.getCodeTaskChecker(task, project)

  override fun getIdeTaskChecker(task: IdeTask, project: Project): TaskChecker<IdeTask> =
    baseProvider.getIdeTaskChecker(task, project)
}