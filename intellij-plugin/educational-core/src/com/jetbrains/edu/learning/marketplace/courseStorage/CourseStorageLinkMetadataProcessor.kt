package com.jetbrains.edu.learning.marketplace.courseStorage

import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.newproject.CourseMetadataProcessor
import com.jetbrains.edu.learning.newproject.CourseProjectState

class CourseStorageLinkMetadataProcessor: CourseMetadataProcessor<EduTrackLink> {
  override fun findApplicableMetadata(rawMetadata: Map<String, String>): EduTrackLink? {
    val link = rawMetadata["link"] ?: return null
    return EduTrackLink(link)
  }

  override fun processMetadata(
    project: Project,
    course: Course,
    metadata: EduTrackLink,
    courseProjectState: CourseProjectState
  ) {
    CourseStorageLinkSettings.getInstance(project).link = metadata.link
  }
}

data class EduTrackLink(val link: String)
