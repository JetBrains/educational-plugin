package com.jetbrains.edu.learning.marketplace.courseStorage

import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.newproject.CourseMetadataProcessor
import com.jetbrains.edu.learning.newproject.CourseProjectState
import java.net.URI

class CourseStorageLinkMetadataProcessor : CourseMetadataProcessor<EduTrackLink> {
  override fun findApplicableMetadata(rawMetadata: Map<String, String>): EduTrackLink? {
    val link = rawMetadata["link"] ?: return null
    if (!link.isValidAndAllowedUrl()) {
      return null
    }
    val platformName = rawMetadata["platformName"] ?: return null
    return EduTrackLink(link, platformName)
  }

  override fun processMetadata(
    project: Project,
    course: Course,
    metadata: EduTrackLink,
    courseProjectState: CourseProjectState
  ) {
    val courseStorageLinkSettings = CourseStorageLinkSettings.getInstance(project)
    courseStorageLinkSettings.link = metadata.link
    courseStorageLinkSettings.platformName = metadata.platformName
  }

  private fun String.isValidAndAllowedUrl(): Boolean = try {
    val uri = URI(this)
    uri.scheme == "https" && uri.host in TRUSTED_COURSE_STORAGE_HOSTS
  }
  catch (_: java.net.URISyntaxException) {
    false
  }
}

data class EduTrackLink(val link: String, val platformName: String)
