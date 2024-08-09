package com.jetbrains.edu.learning.marketplace.changeHost

import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.DumbAwareAction
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.marketplace.MarketplaceSolutionLoader
import com.jetbrains.edu.learning.marketplace.SUBMISSIONS_SERVICE_HOST_PROPERTY
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.services.dialog.showDialogAndGetHost
import com.jetbrains.edu.learning.submissions.SubmissionsManager
import org.jetbrains.annotations.NonNls

class SubmissionsServiceChangeHost : DumbAwareAction(EduCoreBundle.message("submissions.service.change.host")) {

  override fun actionPerformed(e: AnActionEvent) {
    val project = e.project ?: return
    val course = project.course ?: return

    val ltiLaunchId = course.ltiLaunchId
    val selectedUrl = SubmissionsServiceChangeHostDialog(ltiLaunchId).showDialogAndGetHost()
    if (selectedUrl == null) {
      LOG.warn("Selected Submissions service URL item is null")
      return
    }

    val existingValue = PropertiesComponent.getInstance().getValue(SUBMISSIONS_SERVICE_HOST_PROPERTY, SubmissionsServiceHost.PRODUCTION.url)
    if (selectedUrl == existingValue) return

    PropertiesComponent.getInstance().setValue(SUBMISSIONS_SERVICE_HOST_PROPERTY, selectedUrl, existingValue)
    LOG.info("Submissions service URL was changed to $selectedUrl")

    val submissionsManager = SubmissionsManager.getInstance(project)
    if (!course.isMarketplace || !submissionsManager.submissionsSupported()) return

    SubmissionsManager.getInstance(project).deleteCourseSubmissionsLocally()
    submissionsManager.prepareSubmissionsContentWhenLoggedIn {
      MarketplaceSolutionLoader.getInstance(project).loadSolutionsInBackground()
    }
  }

  companion object {
    private val LOG: Logger = thisLogger()

    @NonNls
    const val ACTION_ID = "Educational.Student.SubmissionsServiceChangeHost"
  }
}