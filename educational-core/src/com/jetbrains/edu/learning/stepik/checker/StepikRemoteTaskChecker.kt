package com.jetbrains.edu.learning.stepik.checker

import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.EduSettings
import com.jetbrains.edu.learning.Err
import com.jetbrains.edu.learning.Result
import com.jetbrains.edu.learning.checker.remote.RemoteTaskChecker
import com.jetbrains.edu.learning.courseFormat.CheckResult
import com.jetbrains.edu.learning.courseFormat.EduFormatNames.LOGIN_NEEDED_MESSAGE
import com.jetbrains.edu.learning.courseFormat.tasks.AnswerTask
import com.jetbrains.edu.learning.courseFormat.tasks.CodeTask
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.courseFormat.tasks.UnsupportedTask
import com.jetbrains.edu.learning.courseFormat.tasks.choice.ChoiceTask
import com.jetbrains.edu.learning.courseFormat.tasks.data.DataTask
import com.jetbrains.edu.learning.stepik.StepikNames

class StepikRemoteTaskChecker : RemoteTaskChecker {
  override fun canCheck(project: Project, task: Task): Boolean {
    val course = task.course
    return course.isStepikRemote && StepikCheckConnector.isRemotelyChecked(task)
  }

  override fun check(project: Project, task: Task, indicator: ProgressIndicator): CheckResult {
    EduSettings.getInstance().user ?: return CheckResult.LOGIN_NEEDED
    return when (task) {
      is AnswerTask -> StepikCheckConnector.checkAnswerTask(project, task)
      is ChoiceTask -> StepikCheckConnector.checkChoiceTask(project, task)
      is CodeTask -> StepikCheckConnector.checkCodeTask(project, task)
      is DataTask -> StepikCheckConnector.checkDataTask(project, task, indicator)
      is UnsupportedTask -> StepikCheckConnector.checkUnsupportedTask(task)
      else -> error("Can't check ${task.itemType} on ${StepikNames.STEPIK}")
    }
  }

  override fun retry(task: Task): Result<Boolean, String> {
    EduSettings.getInstance().user ?: return Err(LOGIN_NEEDED_MESSAGE)
    return when (task) {
      is ChoiceTask -> StepikCheckConnector.retryChoiceTask(task)
      else -> error("Can't retry ${task.itemType} on ${StepikNames.STEPIK}")
    }
  }
}
