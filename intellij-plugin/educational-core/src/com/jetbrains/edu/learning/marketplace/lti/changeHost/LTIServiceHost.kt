package com.jetbrains.edu.learning.marketplace.lti.changeHost

import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.util.NlsContexts
import com.jetbrains.edu.learning.actions.changeHost.ServiceHostEnum
import com.jetbrains.edu.learning.actions.changeHost.ServiceHostManager
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.marketplace.MarketplaceSolutionLoader
import com.jetbrains.edu.learning.messages.BUNDLE
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.submissions.SubmissionsManager
import org.jetbrains.annotations.PropertyKey

private const val LTI_PRODUCTION_HOST = "https://lti-tool-production.labs.jb.gg/"
private const val LTI_STAGING_HOST = "https://lti-tool-staging.labs.jb.gg/"

@Suppress("unused") // All enum values ar used in UI
enum class LTIServiceHost(
  override val url: String,
  @param:PropertyKey(resourceBundle = BUNDLE) private val visibleNameKey: String
) : ServiceHostEnum {
  PRODUCTION(LTI_PRODUCTION_HOST, "change.service.host.production"),
  STAGING(LTI_STAGING_HOST, "change.service.host.staging"),
  OTHER("http://localhost:8081", "change.service.host.other");

  override fun visibleName(): @NlsContexts.ListItem String = EduCoreBundle.message(visibleNameKey)

  companion object : ServiceHostManager<LTIServiceHost>("Submissions service", LTIServiceHost::class.java) {

    override val default: LTIServiceHost = PRODUCTION
    override val other: LTIServiceHost = OTHER

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
