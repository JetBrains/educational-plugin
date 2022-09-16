package com.jetbrains.edu.learning.stepik.checker

import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.courseFormat.CheckResult
import com.jetbrains.edu.learning.courseFormat.tasks.CodeTask
import com.jetbrains.edu.learning.onError
import com.jetbrains.edu.learning.stepik.StepikNames
import com.jetbrains.edu.learning.stepik.api.StepikConnector
import javax.swing.event.HyperlinkEvent

object StepikCheckConnector : StepikBasedCheckConnector() {
  override val loginListener: StepikBasedLoginListener
    get() = StepikLoginListener
  override val linkToHelp: String = StepikNames.STEPIK_HELP

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

object StepikLoginListener : StepikBasedLoginListener() {
  override fun hyperlinkActivated(e: HyperlinkEvent) {
    doLogin()
  }

  override fun doLogin() {
    StepikConnector.getInstance().doAuthorize()
  }
}
