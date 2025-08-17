package com.jetbrains.edu.learning.marketplace.changeHost

import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.util.NlsContexts
import com.jetbrains.edu.learning.actions.changeHost.ServiceHostEnum
import com.jetbrains.edu.learning.actions.changeHost.ServiceHostManager
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.marketplace.MarketplaceSolutionLoader
import com.jetbrains.edu.learning.marketplace.SUBMISSIONS_SERVICE_PRODUCTION_URL
import com.jetbrains.edu.learning.marketplace.SUBMISSIONS_SERVICE_STAGING_URL
import com.jetbrains.edu.learning.messages.BUNDLE
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.submissions.SubmissionsManager
import org.jetbrains.annotations.PropertyKey

@Suppress("unused") // All enum values ar used in UI
enum class SubmissionsServiceHost(
  override val url: String,
  @param:PropertyKey(resourceBundle = BUNDLE) private val visibleNameKey: String
) : ServiceHostEnum {
  PRODUCTION(SUBMISSIONS_SERVICE_PRODUCTION_URL, "change.service.host.production"),
  STAGING(SUBMISSIONS_SERVICE_STAGING_URL, "change.service.host.staging"),
  OTHER("http://localhost:8080", "change.service.host.other");

  override fun visibleName(): @NlsContexts.ListItem String = EduCoreBundle.message(visibleNameKey)

  companion object : ServiceHostManager<SubmissionsServiceHost>("Submissions service", SubmissionsServiceHost::class.java) {

    override val default: SubmissionsServiceHost = PRODUCTION
    override val other: SubmissionsServiceHost = OTHER

    override fun onHostChanged() {
      for (project in ProjectManager.getInstance().openProjects) {
        val course = project.course ?: continue

        val submissionsManager = SubmissionsManager.getInstance(project)
        if (!course.isMarketplace || !submissionsManager.submissionsSupported()) return

        SubmissionsManager.getInstance(project).deleteCourseSubmissionsLocally()
        submissionsManager.prepareSubmissionsContentWhenLoggedIn {
          MarketplaceSolutionLoader.getInstance(project).loadSolutionsInBackground()
        }
      }
    }
  }
}
