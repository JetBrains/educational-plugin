package com.jetbrains.edu.learning.twitter

import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.checker.CheckListener
import com.jetbrains.edu.learning.courseFormat.CheckResult
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.statistics.EduCounterUsageCollector
import com.jetbrains.edu.learning.twitter.TwitterUtils.createTwitterDialogAndShow

class TwitterAction : CheckListener {
  private var statusBeforeCheck: CheckStatus? = null

  override fun beforeCheck(project: Project, task: Task) {
    statusBeforeCheck = task.status
  }

  override fun afterCheck(project: Project, task: Task, result: CheckResult) {
    val settings = TwitterSettings.getInstance()
    if (!settings.askToTweet()) return

    for (twitterPluginConfigurator in TwitterPluginConfigurator.EP_NAME.extensionList) {
      if (twitterPluginConfigurator.askToTweet(project, task, statusBeforeCheck!!)) {
        createTwitterDialogAndShow(project, twitterPluginConfigurator, task)
        EduCounterUsageCollector.twitterDialogShown(task.course)
      }
    }
  }
}
