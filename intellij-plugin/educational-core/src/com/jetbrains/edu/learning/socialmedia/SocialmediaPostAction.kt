package com.jetbrains.edu.learning.socialmedia

import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.checker.CheckListener
import com.jetbrains.edu.learning.courseFormat.CheckResult
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.tasks.Task

abstract class SocialmediaPostAction<T : SocialmediaPluginConfigurator> : CheckListener {
  private var statusBeforeCheck: CheckStatus? = null
  abstract val settings: SocialMediaSettings<SocialMediaSettings.SocialMediaSettingsState>
  abstract fun createDialogAndShow(project: Project, configurator: T, task: Task)
  abstract fun sendStatistics(course: Course)
  abstract fun extensionPointName(): ExtensionPointName<T>

  override fun beforeCheck(project: Project, task: Task) {
    statusBeforeCheck = task.status
  }

  override fun afterCheck(project: Project, task: Task, result: CheckResult) {
    if (!settings.askToPost) return
    val status = statusBeforeCheck ?: return

    for (pluginConfigurator in extensionPointName().extensionList) {
      if (pluginConfigurator.askToPost(project, task, status)) {
        createDialogAndShow(project, pluginConfigurator, task)
        sendStatistics(task.course)
      }
    }
  }
}
