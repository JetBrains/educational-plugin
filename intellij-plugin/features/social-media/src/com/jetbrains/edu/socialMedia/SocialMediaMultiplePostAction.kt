package com.jetbrains.edu.socialMedia

import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.checker.CheckListener
import com.jetbrains.edu.learning.courseFormat.CheckResult
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.runInBackground
import com.jetbrains.edu.learning.statistics.EduCounterUsageCollector
import com.jetbrains.edu.socialMedia.linkedIn.LinkedInPluginConfigurator
import com.jetbrains.edu.socialMedia.messages.EduSocialMediaBundle
import com.jetbrains.edu.socialMedia.suggestToPostDialog.createSuggestToPostDialogUI
import com.jetbrains.edu.socialMedia.x.XPluginConfigurator


class SocialMediaMultiplePostAction : CheckListener {

  private var statusBeforeCheck: CheckStatus? = null

  private val configurators = listOf(XPluginConfigurator.EP_NAME, LinkedInPluginConfigurator.EP_NAME).flatMap { it.extensionList }

  private fun sendStatistics(course: Course) {
    EduCounterUsageCollector.linkedInDialogShown(course)
    EduCounterUsageCollector.xDialogShown(course)
  }

  override fun beforeCheck(project: Project, task: Task) {
    statusBeforeCheck = task.status
  }

  private fun createDialogAndShow(project: Project, configurators: List<SocialMediaPluginConfigurator>, task: Task) {
    val defaultConfigurator = configurators.firstOrNull() ?: return
    // NB! `imageIndex` should be the same for all configurators to post the same images to all social networks
    val (imageIndex, imagePath) = defaultConfigurator.getIndexWithImagePath(task)
    val dialog = createSuggestToPostDialogUI(project, configurators, defaultConfigurator.getMessage(task), imagePath)

    if (dialog.showAndGet()) {
      runInBackground(project, EduSocialMediaBundle.message("linkedin.loading.posting"), true) {
        configurators.forEach {
          it.doPost(project, task, imageIndex)
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
