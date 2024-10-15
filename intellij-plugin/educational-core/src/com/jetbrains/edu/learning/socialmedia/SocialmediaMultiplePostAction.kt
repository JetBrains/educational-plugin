package com.jetbrains.edu.learning.socialmedia

import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.checker.CheckListener
import com.jetbrains.edu.learning.courseFormat.CheckResult
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.runInBackground
import com.jetbrains.edu.learning.socialmedia.linkedIn.LinkedInPluginConfigurator
import com.jetbrains.edu.learning.socialmedia.suggestToPostDialog.createSuggestToPostDialogUI
import com.jetbrains.edu.learning.socialmedia.twitter.TwitterPluginConfigurator
import com.jetbrains.edu.learning.statistics.EduCounterUsageCollector


class SocialmediaMultiplePostAction : CheckListener {

  private var statusBeforeCheck: CheckStatus? = null

  private val configurators = listOf(TwitterPluginConfigurator.EP_NAME, LinkedInPluginConfigurator.EP_NAME).flatMap { it.extensionList }

  private fun sendStatistics(course: Course) {
    EduCounterUsageCollector.linkedInDialogShown(course)
    EduCounterUsageCollector.twitterDialogShown(course)
  }

  override fun beforeCheck(project: Project, task: Task) {
    statusBeforeCheck = task.status
  }

  private fun createDialogAndShow(project: Project, configurators: List<SocialmediaPluginConfigurator>, task: Task) {
    val defaultConfigurator = configurators.firstOrNull() ?: return
    // NB! `imageIndex` should be the same for all configurators to post the same images to all social networks
    val (imageIndex, imagePath) = defaultConfigurator.getIndexWithImagePath(task)
    val dialog = createSuggestToPostDialogUI(project, configurators, defaultConfigurator.getMessage(task), imagePath)

    if (dialog.showAndGet()) {
      runInBackground(null, EduCoreBundle.message("linkedin.loading.posting"), true) {
        configurators.forEach {
          it.doPost(task, imageIndex)
        }
      }
    }
  }

  override fun afterCheck(project: Project, task: Task, result: CheckResult) {
    val status = statusBeforeCheck ?: return
    val activeConfigurators = configurators.filter { it.askToPost(project, task, status) }
    if (activeConfigurators.all { !it.settings.askToPost }) return
    if (activeConfigurators.isEmpty()) return

    createDialogAndShow(project, activeConfigurators, task)
    sendStatistics(task.course)
  }
}
