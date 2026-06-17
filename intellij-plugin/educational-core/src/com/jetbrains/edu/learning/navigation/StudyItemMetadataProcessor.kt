package com.jetbrains.edu.learning.navigation

import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.newproject.CourseMetadataProcessor
import com.jetbrains.edu.learning.newproject.CourseProjectState

/**
 * Selects the study item (task, lesson or section) specified by the [STUDY_ITEM_ID] metadata parameter on course open.
 */
class StudyItemMetadataProcessor : CourseMetadataProcessor<Int> {
  override fun findApplicableMetadata(rawMetadata: Map<String, String>): Int? =
    rawMetadata[STUDY_ITEM_ID]?.toIntOrNull()

  override fun processMetadata(project: Project, course: Course, metadata: Int, courseProjectState: CourseProjectState) {
    StudyItemSelectionService.getInstance(project).setCurrentStudyItem(metadata)
  }

  companion object {
    const val STUDY_ITEM_ID = "study_item_id"
  }
}