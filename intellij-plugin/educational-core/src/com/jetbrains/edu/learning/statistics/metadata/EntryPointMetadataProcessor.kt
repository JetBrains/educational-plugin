package com.jetbrains.edu.learning.statistics.metadata

import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.newproject.CourseMetadataProcessor
import com.jetbrains.edu.learning.newproject.CourseProjectState
import com.jetbrains.edu.learning.statistics.metadata.CourseSubmissionMetadataManager.Companion.ENTRY_POINT
import com.jetbrains.edu.learning.statistics.metadata.CourseSubmissionMetadataManager.Companion.MAX_VALUE_LENGTH

class EntryPointMetadataProcessor : CourseMetadataProcessor<String> {
  override fun findApplicableMetadata(rawMetadata: Map<String, String>): String? {
    val entryPointValue = rawMetadata[ENTRY_POINT] ?: return null
    if (entryPointValue.length > MAX_VALUE_LENGTH) {
      thisLogger().warn("entry point value is too long: $entryPointValue. Max supported length is $MAX_VALUE_LENGTH")
      return null
    }
    return entryPointValue
  }

  override fun processMetadata(project: Project, course: Course, metadata: String, courseProjectState: CourseProjectState) {
    CourseSubmissionMetadataManager.getInstance(project).addMetadata(ENTRY_POINT to metadata)
  }
}
