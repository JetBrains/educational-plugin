package com.jetbrains.edu.learning.marketplace.courseStorage.changeHost

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.NlsContexts.ListItem
import com.jetbrains.edu.coursecreator.CCUtils.isCourseCreator
import com.jetbrains.edu.learning.EduExperimentalFeatures
import com.jetbrains.edu.learning.actions.changeHost.ServiceHostEnum
import com.jetbrains.edu.learning.actions.changeHost.ServiceHostManager
import com.jetbrains.edu.learning.isFeatureEnabled
import com.jetbrains.edu.learning.messages.BUNDLE
import com.jetbrains.edu.learning.messages.EduCoreBundle
import org.jetbrains.annotations.PropertyKey
import org.jetbrains.annotations.VisibleForTesting

private const val COURSE_STORAGE_PRODUCTION_URL = "https://edu-aws-tracks.labs.jb.gg"
@VisibleForTesting
const val COURSE_STORAGE_PRODUCTION_CC_URL = "https://edu-aws-tracks-internal-production.labs.jb.gg"
private const val COURSE_STORAGE_STAGING_URL = "https://edu-aws-tracks-staging.labs.jb.gg"

/**
 * Represents the service hosts for the Course Storage.
 *
 * Note: Currently, for [PRODUCTION] different URL's must be used depending on the functionality.
 * Student requests go to an open URL, but the CC request to upload the course goes to an internal URL that is only accessible from a VPN.
 *
 * Consider using [CourseStorageServiceHost.getSelectedHost] instead of [ServiceHostManager.selectedHost] for this class.
 *
 * For [STAGING] only one url is used.
 *
 * TODO(merge it with [ServiceHostManager.selectedHost])
 */
@Suppress("unused") // All enum values ar used in UI
enum class CourseStorageServiceHost(
  override val url: String,
  @param:PropertyKey(resourceBundle = BUNDLE) private val visibleNameKey: String
) : ServiceHostEnum {
  PRODUCTION(COURSE_STORAGE_PRODUCTION_URL, "change.service.host.production"),
  STAGING(COURSE_STORAGE_STAGING_URL, "change.service.host.staging"),
  OTHER("http://localhost:8084", "change.service.host.other");

  override fun visibleName(): @ListItem String = EduCoreBundle.message(visibleNameKey)

  companion object : ServiceHostManager<CourseStorageServiceHost>("Course Storage", CourseStorageServiceHost::class.java) {
    override val default: CourseStorageServiceHost = PRODUCTION
    override val other: CourseStorageServiceHost = OTHER

    fun getSelectedHost(project: Project?): SelectedServiceHost<CourseStorageServiceHost> {
      val host = selectedHost
      return if (host.value == PRODUCTION &&
                 project != null && isCourseCreator(project) &&
                 isFeatureEnabled(EduExperimentalFeatures.CC_COURSE_STORAGE)) {
        SelectedServiceHost(PRODUCTION, COURSE_STORAGE_PRODUCTION_CC_URL)
      }
      else host
    }
  }
}
