package com.jetbrains.edu.learning.stepik.checker

import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.EduSettings
import com.jetbrains.edu.learning.Err
import com.jetbrains.edu.learning.Result
import com.jetbrains.edu.learning.checker.CheckResult
import com.jetbrains.edu.learning.checker.CheckUtils
import com.jetbrains.edu.learning.checker.remote.RemoteTaskChecker
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.courseFormat.tasks.CodeTask
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.courseFormat.tasks.choice.ChoiceTask
import com.jetbrains.edu.learning.stepik.StepikCheckerConnector
import com.jetbrains.edu.learning.stepik.StepikNames

class StepikRemoteTaskChecker : RemoteTaskChecker {
  override fun canCheck(project: Project, task: Task): Boolean {
    val course = task.course
    return task.shouldBeCheckedOnStepik && (course is EduCourse && course.isStepikRemote)
  }

  private val Task.shouldBeCheckedOnStepik: Boolean
    get() = this is CodeTask || (this is ChoiceTask && !canCheckLocally)

  override fun check(project: Project, task: Task, indicator: ProgressIndicator): CheckResult {
    val user = EduSettings.getInstance().user ?: return CheckResult.LOGIN_NEEDED
    return when (task) {
      is ChoiceTask -> StepikCheckerConnector.checkChoiceTask(project, task, user)
      is CodeTask -> StepikCheckerConnector.checkCodeTask(project, task, user)
      else -> error("Can't check ${task.itemType} on ${StepikNames.STEPIK}")
    }
  }

  override fun retry(task: Task): Result<Boolean, String> {
    EduSettings.getInstance().user ?: return Err(CheckUtils.LOGIN_NEEDED_MESSAGE)
    return when (task) {
      is ChoiceTask -> StepikCheckerConnector.retryChoiceTask(task)
      else -> error("Can't retry ${task.itemType} on ${StepikNames.STEPIK}")
    }
  }
}
