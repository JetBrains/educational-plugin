package com.jetbrains.edu.learning.statistics

import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.newproject.CourseMetadataProcessor

private const val MAX_VALUE_LENGTH = 16

class EntryPointMetadataProcessor : CourseMetadataProcessor<String> {
  override fun findApplicableMetadata(rawMetadata: Map<String, String>): String? {
    val entryPointValue = rawMetadata[ENTRY_POINT] ?: return null
    if (entryPointValue.length > MAX_VALUE_LENGTH) {
      thisLogger().warn("entry point value is too long: $entryPointValue. Max supported length is $MAX_VALUE_LENGTH")
      return null
    }
    return entryPointValue
  }

  override fun processMetadata(project: Project, course: Course, metadata: String) {
    EntryPointManager.getInstance(project).entryPoint = metadata
  }

  companion object {
    const val ENTRY_POINT = "entry_point"
  }
}
