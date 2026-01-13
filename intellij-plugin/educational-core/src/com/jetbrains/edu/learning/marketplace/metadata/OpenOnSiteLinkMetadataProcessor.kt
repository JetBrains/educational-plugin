package com.jetbrains.edu.learning.marketplace.metadata

import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.marketplace.settings.OpenOnSiteLinkSettings
import com.jetbrains.edu.learning.newproject.CourseMetadataProcessor
import com.jetbrains.edu.learning.newproject.CourseProjectState

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
}