package com.jetbrains.edu.socialMedia

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.EduTestAware
import com.jetbrains.edu.learning.checker.CheckListener
import com.jetbrains.edu.learning.courseFormat.CheckResult
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.ext.isPreview
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.runInBackground
import com.jetbrains.edu.learning.statistics.EduCounterUsageCollector
import com.jetbrains.edu.socialMedia.linkedIn.LinkedInPluginConfigurator
import com.jetbrains.edu.socialMedia.messages.EduSocialMediaBundle
import com.jetbrains.edu.socialMedia.suggestToPostDialog.createSuggestToPostDialogUI
import com.jetbrains.edu.socialMedia.x.XPluginConfigurator
import org.jetbrains.annotations.TestOnly
import java.util.concurrent.ConcurrentHashMap


class SocialMediaMultiplePostAction : CheckListener {

  private fun sendStatistics(course: Course) {
    EduCounterUsageCollector.linkedInDialogShown(course)
    EduCounterUsageCollector.xDialogShown(course)
  }

  override fun beforeCheck(project: Project, task: Task) {
    PreviousTaskStatusService.getInstance(project).saveCurrentStatus(task)
  }

  private fun createDialogAndShow(project: Project, configurators: List<SocialMediaPluginConfigurator>, task: Task) {
    val defaultConfigurator = configurators.firstOrNull() ?: return
    // NB! `imageIndex` should be the same for all configurators to post the same images to all social networks
    val (imageIndex, imagePath) = defaultConfigurator.getIndexWithImagePath(task)
    val dialog = createSuggestToPostDialogUI(project, configurators, defaultConfigurator.getMessage(task), imagePath)

    if (dialog.showAndGet()) {
      runInBackground(project, EduSocialMediaBundle.message("social.media.posting.progress.title"), true) {
        configurators.forEach {
          it.doPost(project, task, imageIndex)
        }
      }
    }
  }

  override fun afterCheck(project: Project, task: Task, result: CheckResult) {
    val course = task.course
    // It doesn't make sense to suggest posting to social media in educator mode or for preview course
    if (!course.isStudy || course.isPreview) return
    if (result.status != CheckStatus.Solved) return

    val courseId = course.id
    if (!SocialMediaPostManager.needToAskedToPost(courseId)) return

    val previousStatus = PreviousTaskStatusService.getInstance(project).getPreviousStatus(task) ?: return
    val activeConfigurators = listOf(XPluginConfigurator.EP_NAME, LinkedInPluginConfigurator.EP_NAME)
      .flatMap { it.extensionList }
      .filter { it.askToPost(project, task, previousStatus) }
    if (activeConfigurators.all { !it.settings.askToPost }) return
    if (activeConfigurators.isEmpty()) return

    createDialogAndShow(project, activeConfigurators, task)

    SocialMediaPostManager.setAskedToPost(courseId)
    sendStatistics(course)
  }
}

// TODO: reevaluate this solution. It seems we can do it better
@Service(Service.Level.PROJECT)
private class PreviousTaskStatusService() : EduTestAware {

  private val previousStatuses = ConcurrentHashMap<Int, CheckStatus>()

  fun saveCurrentStatus(task: Task) {
    previousStatuses[task.id] = task.status
  }

  fun getPreviousStatus(task: Task): CheckStatus? = previousStatuses[task.id]

  @TestOnly
  override fun cleanUpState() {
    previousStatuses.clear()
  }

  companion object {
    fun getInstance(project: Project): PreviousTaskStatusService = project.service()
  }
}
