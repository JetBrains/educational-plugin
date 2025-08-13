package com.jetbrains.edu.learning.marketplace.metadata

import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.marketplace.settings.OpenOnSiteLinkSettings
import com.jetbrains.edu.learning.marketplace.settings.OpenOnSiteLinkSettings.Companion.TRUSTED_OPEN_ON_SITE_HOSTS
import com.jetbrains.edu.learning.newproject.CourseMetadataProcessor
import com.jetbrains.edu.learning.newproject.CourseProjectState
import java.net.URI
import java.net.URISyntaxException

class OpenOnSiteLinkMetadataProcessor : CourseMetadataProcessor<String> {
  override fun findApplicableMetadata(rawMetadata: Map<String, String>): String? {
    val link = rawMetadata["link"] ?: return null
    if (!link.isValidAndAllowedUrl()) {
      return null
    }
    return link
  }

  override fun processMetadata(
    project: Project,
    course: Course,
    metadata: String,
    courseProjectState: CourseProjectState
  ) {
    OpenOnSiteLinkSettings.getInstance(project).link = metadata
  }

  private fun String.isValidAndAllowedUrl(): Boolean = try {
    val uri = URI(this)
    uri.scheme == "https" && uri.host in TRUSTED_OPEN_ON_SITE_HOSTS
  }
  catch (_: URISyntaxException) {
    false
  }
}