package com.jetbrains.edu.learning.stepik.checker

import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.EduSettings
import com.jetbrains.edu.learning.checker.CheckResult
import com.jetbrains.edu.learning.checker.remote.RemoteTaskChecker
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.courseFormat.tasks.CodeTask
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.courseFormat.tasks.choice.ChoiceTask
import com.jetbrains.edu.learning.stepik.StepikCheckerConnector

class StepikRemoteTaskChecker : RemoteTaskChecker {
  override fun canCheck(project: Project, task: Task): Boolean {
    val course = task.course
    return task.shouldBeCheckedOnStepik && (course is EduCourse && course.isRemote)
  }

  private val Task.shouldBeCheckedOnStepik: Boolean
    get() = this is CodeTask || (this is ChoiceTask && !canCheckLocally)

  override fun check(project: Project, task: Task, indicator: ProgressIndicator): CheckResult {
    val user = EduSettings.getInstance().user ?: return CheckResult.LOGIN_NEEDED
    return when (task) {
      is ChoiceTask -> StepikCheckerConnector.checkChoiceTask(project, task, user)
      is CodeTask -> StepikCheckerConnector.checkCodeTask(project, task, user)
      else -> error("Can't check ${task.itemType} on Stepik")
    }
  }
}
