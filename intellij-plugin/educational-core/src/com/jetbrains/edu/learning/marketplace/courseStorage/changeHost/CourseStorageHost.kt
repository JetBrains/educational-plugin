package com.jetbrains.edu.learning.marketplace.courseStorage.changeHost

import com.intellij.openapi.util.NlsContexts.ListItem
import com.jetbrains.edu.learning.actions.changeHost.ServiceHostEnum
import com.jetbrains.edu.learning.actions.changeHost.ServiceHostManager
import com.jetbrains.edu.learning.messages.BUNDLE
import com.jetbrains.edu.learning.messages.EduCoreBundle
import org.jetbrains.annotations.PropertyKey

private const val COURSE_STORAGE_PRODUCTION_URL = "https://edu-aws-tracks.labs.jb.gg"
private const val COURSE_STORAGE_STAGING_URL = "https://edu-aws-tracks-staging.labs.jb.gg"

@Suppress("unused") // All enum values ar used in UI
enum class CourseStorageServiceHost(
  override val url: String,
  @param:PropertyKey(resourceBundle = BUNDLE) private val visibleNameKey: String
) : ServiceHostEnum {
  PRODUCTION(COURSE_STORAGE_PRODUCTION_URL, "change.service.host.production"),
  STAGING(COURSE_STORAGE_STAGING_URL, "change.service.host.staging"),
  OTHER("http://localhost:8080", "change.service.host.other");

  override fun visibleName(): @ListItem String = EduCoreBundle.message(visibleNameKey)

  companion object : ServiceHostManager<CourseStorageServiceHost>("Course Storage", CourseStorageServiceHost::class.java) {
    override val default: CourseStorageServiceHost = PRODUCTION
    override val other: CourseStorageServiceHost = OTHER
  }
}
