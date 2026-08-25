package com.jetbrains.edu.learning.marketplace.submissions

import com.intellij.openapi.project.Project
import com.intellij.platform.ide.progress.withBackgroundProgress
import com.jetbrains.edu.learning.EduUtilsKt.isStudentProject
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.marketplace.MarketplaceSolutionLoader
import com.jetbrains.edu.learning.marketplace.api.MarketplaceLoginListener
import com.jetbrains.edu.learning.marketplace.api.MarketplaceSubmissionsConnector
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.submissions.SubmissionsManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class ProjectMarketplaceLoginListener(private val project: Project, private val scope: CoroutineScope) : MarketplaceLoginListener {
  override fun onLoginSuccess() {
    if (!project.isStudentProject()) return
    val course = project.course ?: return
    SubmissionsManager.getInstance(project).prepareSubmissionsContentWhenLoggedIn {
      scope.launch {
        withBackgroundProgress(project, EduCoreBundle.message("marketplace.uploading.local.submissions.progress.title")) {
          MarketplaceSubmissionsConnector.getInstance().uploadLocalSubmissions(project, course)
        }
        MarketplaceSolutionLoader.getInstance(project).loadSolutionsInBackground()
      }
    }
  }
}