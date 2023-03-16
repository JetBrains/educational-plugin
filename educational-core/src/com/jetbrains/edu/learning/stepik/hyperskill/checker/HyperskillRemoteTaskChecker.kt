package com.jetbrains.edu.learning.stepik.hyperskill.checker

import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.Err
import com.jetbrains.edu.learning.Result
import com.jetbrains.edu.learning.courseFormat.CheckResult
import com.jetbrains.edu.learning.checker.remote.RemoteTaskChecker
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.tasks.AnswerTask
import com.jetbrains.edu.learning.courseFormat.tasks.CodeTask
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.courseFormat.tasks.UnsupportedTask
import com.jetbrains.edu.learning.courseFormat.tasks.choice.ChoiceTask
import com.jetbrains.edu.learning.courseFormat.tasks.data.DataTask
import com.jetbrains.edu.learning.courseFormat.tasks.matching.MatchingTask
import com.jetbrains.edu.learning.courseFormat.tasks.matching.SortingTask
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.stepik.hyperskill.courseFormat.HyperskillCourse
import com.jetbrains.edu.learning.stepik.hyperskill.courseFormat.RemoteEduTask
import com.jetbrains.edu.learning.stepik.hyperskill.settings.HyperskillSettings

class HyperskillRemoteTaskChecker : RemoteTaskChecker {
  override fun canCheck(project: Project, task: Task): Boolean {
    return task.course is HyperskillCourse && HyperskillCheckConnector.isRemotelyChecked(task)
  }

  override fun check(project: Project, task: Task, indicator: ProgressIndicator): CheckResult {
    HyperskillSettings.INSTANCE.account ?: return CheckResult(CheckStatus.Unchecked,
                                                              EduCoreBundle.message("check.login.error", EduNames.JBA))
    return when (task) {
      is AnswerTask -> HyperskillCheckConnector.checkAnswerTask(project, task)
      is ChoiceTask -> HyperskillCheckConnector.checkChoiceTask(project, task)
      is CodeTask -> HyperskillCheckConnector.checkCodeTask(project, task)
      is DataTask -> HyperskillCheckConnector.checkDataTask(project, task, indicator)
      is RemoteEduTask -> HyperskillCheckConnector.checkRemoteEduTask(project, task)
      is SortingTask -> HyperskillCheckConnector.checkSortingBasedTask(project, task)
      is MatchingTask -> HyperskillCheckConnector.checkSortingBasedTask(project, task)
      is UnsupportedTask -> HyperskillCheckConnector.checkUnsupportedTask(task)
      else -> error("Can't check ${task.itemType} on ${EduNames.JBA}")
    }
  }

  override fun retry(task: Task): Result<Boolean, String> {
    HyperskillSettings.INSTANCE.account ?: Err(EduCoreBundle.message("check.login.error", EduNames.JBA))
    return when (task) {
      is ChoiceTask -> HyperskillCheckConnector.retryChoiceTask(task)
      else -> error("Can't retry ${task.itemType} on ${EduNames.JBA}")
    }
  }
}
