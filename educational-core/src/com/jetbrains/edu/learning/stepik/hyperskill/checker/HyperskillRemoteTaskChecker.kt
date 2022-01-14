package com.jetbrains.edu.learning.stepik.hyperskill.checker

import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.Err
import com.jetbrains.edu.learning.Result
import com.jetbrains.edu.learning.checker.CheckResult
import com.jetbrains.edu.learning.checker.remote.RemoteTaskChecker
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.tasks.*
import com.jetbrains.edu.learning.courseFormat.tasks.data.DataTask
import com.jetbrains.edu.learning.courseFormat.tasks.choice.ChoiceTask
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.stepik.hyperskill.checker.HyperskillCheckConnector.checkChoiceTask
import com.jetbrains.edu.learning.stepik.hyperskill.checker.HyperskillCheckConnector.checkCodeTask
import com.jetbrains.edu.learning.stepik.hyperskill.checker.HyperskillCheckConnector.checkDataTask
import com.jetbrains.edu.learning.stepik.hyperskill.checker.HyperskillCheckConnector.checkRemoteEduTask
import com.jetbrains.edu.learning.stepik.hyperskill.checker.HyperskillCheckConnector.checkAnswerTask
import com.jetbrains.edu.learning.stepik.hyperskill.checker.HyperskillCheckConnector.retryChoiceTask
import com.jetbrains.edu.learning.stepik.hyperskill.courseFormat.HyperskillCourse
import com.jetbrains.edu.learning.stepik.hyperskill.courseFormat.HyperskillCourse.Companion.isRemotelyChecked
import com.jetbrains.edu.learning.stepik.hyperskill.courseFormat.RemoteEduTask
import com.jetbrains.edu.learning.stepik.hyperskill.settings.HyperskillSettings

class HyperskillRemoteTaskChecker : RemoteTaskChecker {
  override fun canCheck(project: Project, task: Task): Boolean {
    return task.course is HyperskillCourse && task.isRemotelyChecked()
  }

  override fun check(project: Project, task: Task, indicator: ProgressIndicator): CheckResult {
    HyperskillSettings.INSTANCE.account ?: return CheckResult(CheckStatus.Unchecked,
                                                              EduCoreBundle.message("check.login.error", EduNames.JBA))
    return when (task) {
      is DataTask -> checkDataTask(project, task, indicator)
      is CodeTask -> checkCodeTask(project, task)
      is AnswerTask -> checkAnswerTask(project, task)
      is ChoiceTask -> checkChoiceTask(project, task)
      is RemoteEduTask -> checkRemoteEduTask(project, task)
      else -> error("Can't check ${task.itemType} on ${EduNames.JBA}")
    }
  }

  override fun retry(task: Task): Result<Boolean, String> {
    HyperskillSettings.INSTANCE.account ?: Err(EduCoreBundle.message("check.login.error", EduNames.JBA))
    return when (task) {
      is ChoiceTask -> retryChoiceTask(task)
      else -> error("Can't retry ${task.itemType} on ${EduNames.JBA}")
    }
  }
}
