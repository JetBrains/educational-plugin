package com.jetbrains.edu.learning.stepik.checker

import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.EduSettings
import com.jetbrains.edu.learning.checker.CheckResult
import com.jetbrains.edu.learning.checker.remote.RemoteTaskChecker
import com.jetbrains.edu.learning.courseFormat.RemoteCourse
import com.jetbrains.edu.learning.courseFormat.tasks.ChoiceTask
import com.jetbrains.edu.learning.courseFormat.tasks.CodeTask
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.stepik.StepikAdaptiveConnector

class StepikRemoteTaskChecker : RemoteTaskChecker {
  override fun canCheck(project: Project, task: Task): Boolean {
    return task.course is RemoteCourse && task.shouldBeCheckedOnStepik
  }

  private val Task.shouldBeCheckedOnStepik: Boolean
    get() = this is ChoiceTask || this is CodeTask

  override fun check(project: Project, task: Task): CheckResult {
    val user = EduSettings.getInstance().user ?: return CheckResult.LOGIN_NEEDED
    return when (task) {
      is ChoiceTask -> StepikAdaptiveConnector.checkChoiceTask(task, user)
      is CodeTask -> StepikAdaptiveConnector.checkCodeTask(project, task, user)
      else -> error("Can't check ${task.taskType} on Stepik")
    }
  }
}