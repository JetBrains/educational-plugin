package com.jetbrains.edu.learning.stepik.checker

import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.checker.CheckResult
import com.jetbrains.edu.learning.courseFormat.tasks.CodeTask
import com.jetbrains.edu.learning.onError

object StepikCheckConnector : StepikBasedCheckConnector() {
  override fun checkCodeTask(project: Project, task: CodeTask): CheckResult {
    val checkIdResult = task.checkId()
    if (checkIdResult != null) {
      return checkIdResult
    }

    val submission = StepikBasedSubmitConnector.submitCodeTask(project, task).onError { error ->
      return failedToSubmit(project, task, error)
    }
    return periodicallyCheckSubmissionResult(project, submission, task)
  }
}