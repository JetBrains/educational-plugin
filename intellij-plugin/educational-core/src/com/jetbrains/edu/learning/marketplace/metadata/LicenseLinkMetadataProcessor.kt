package com.jetbrains.edu.learning.marketplace.metadata

import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.marketplace.settings.LicenseLinkSettings
import com.jetbrains.edu.learning.newproject.CourseMetadataProcessor
import com.jetbrains.edu.learning.newproject.CourseProjectState
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

class LicenseLinkMetadataProcessor : CourseMetadataProcessor<String> {
  override fun findApplicableMetadata(rawMetadata: Map<String, String>): String? {
    val link = rawMetadata[LICENSE_URL_PARAMETER_NAME] ?: return null
    val decodedLink = URLDecoder.decode(link, StandardCharsets.UTF_8)
    if (!decodedLink.isValidAndAllowedUrl()) {
      return null
    }
    return decodedLink
  }

  override fun processMetadata(
    project: Project,
    course: Course,
    metadata: String,
    courseProjectState: CourseProjectState
  ) {
    LicenseLinkSettings.getInstance(project).link = metadata
  }

  companion object {
    const val LICENSE_URL_PARAMETER_NAME = "license_url"
  }
}