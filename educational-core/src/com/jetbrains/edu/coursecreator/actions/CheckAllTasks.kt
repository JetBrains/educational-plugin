package com.jetbrains.edu.coursecreator.actions

import com.google.common.annotations.VisibleForTesting
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.ui.Messages
import com.jetbrains.edu.learning.StudyTaskManager
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.ext.configurator

class CheckAllTasks : AnAction("Check All Tasks") {
  override fun actionPerformed(e: AnActionEvent) {
    val project = e.project ?: return
    val course = StudyTaskManager.getInstance(project).course ?: return

    ProgressManager.getInstance().run(object : Task.Backgroundable(project, "Checking all tasks...", true) {
      override fun run(indicator: ProgressIndicator) {
        var hasFailedTasks = false
        var curTask = 0
        var tasksNum = 0
        course.visitTasks { tasksNum++ }
        course.visitTasks {
          if (indicator.isCanceled) {
            return@visitTasks
          }
          curTask++
          val checker = course.configurator?.taskCheckerProvider?.getTaskChecker(it, project)!!
          indicator.text = "Checking task $curTask/$tasksNum"
          if (checker.check(indicator).status != CheckStatus.Solved) {
            hasFailedTasks = true
          }
          checker.clearState()
        }
        if (indicator.isCanceled) {
          return
        }
        runInEdt {
          Messages.showInfoMessage(if (hasFailedTasks) FAILED_MESSAGE else SUCCESS_MESSAGE, "Check Finished")
        }
      }
    })
  }

  override fun update(e: AnActionEvent) {
    //TODO: implement
  }

  companion object {
    @VisibleForTesting
    const val SUCCESS_MESSAGE = "All tasks are solved correctly"

    @VisibleForTesting
    const val FAILED_MESSAGE = "Failed"
  }
}