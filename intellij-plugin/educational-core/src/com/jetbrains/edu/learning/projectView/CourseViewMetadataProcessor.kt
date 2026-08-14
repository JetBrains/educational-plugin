package com.jetbrains.edu.learning.projectView

import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.StudyItem
import com.jetbrains.edu.learning.courseFormat.ext.visitItems
import com.jetbrains.edu.learning.navigation.StudyItemSelectionService.Companion.STUDY_ITEM_ID
import com.jetbrains.edu.learning.newproject.CourseMetadataProcessor
import com.jetbrains.edu.learning.newproject.CourseProjectState

/**
 * Enables the course subtree mode for the course project if the [ENABLE_SUBTREE_VIEW_MODE] parameter is `true`,
 * and makes the study item passed with [STUDY_ITEM_ID] visible in Course View together with all its child items.
 *
 * A study item to reveal is required: without a parsable [STUDY_ITEM_ID] nothing happens at all,
 * since enabling the mode with no item to show would leave the learner with an empty Course View.
 *
 * The parameter can only enable the mode, it never disables it:
 * without the parameter (or with any value except `true`), the mode stays off for a new course project,
 * and an already existing project keeps the mode it was opened with before.
 * The launched study item is revealed in any case since [CourseViewVisibleItems] itself
 * takes into account whether the mode is enabled or not.
 *
 * See [CourseViewVisibleItems]
 */
class CourseViewMetadataProcessor : CourseMetadataProcessor<CourseViewMetadataProcessor.CourseViewParams> {

  override fun findApplicableMetadata(rawMetadata: Map<String, String>): CourseViewParams? {
    val enableSubtreeMode = rawMetadata[ENABLE_SUBTREE_VIEW_MODE]?.toBooleanStrictOrNull() ?: false
    val visibleStudyItemId = rawMetadata[STUDY_ITEM_ID]?.toIntOrNull() ?: return null
    return CourseViewParams(enableSubtreeMode, visibleStudyItemId)
  }

  override fun processMetadata(
    project: Project,
    course: Course,
    metadata: CourseViewParams,
    courseProjectState: CourseProjectState
  ) {
    val visibleItems = CourseViewVisibleItems.getInstance(project)
    if (metadata.enableSubtreeMode) {
      visibleItems.isSubtreeModeEnabled = true
    }

    val itemToReveal = course.findStudyItem(metadata.visibleStudyItemId) ?: return
    visibleItems.markStudyItemAsVisible(itemToReveal)
  }

  private fun Course.findStudyItem(id: Int): StudyItem? {
    // Items of courses without ids (for example, local ones) have zero id, so such an id can't identify a particular item
    if (id == 0) return null

    var result: StudyItem? = null
    visitItems { item ->
      if (result == null && item.id == id) {
        result = item
      }
    }
    return result
  }

  companion object {
    const val ENABLE_SUBTREE_VIEW_MODE = "enable_subtree_view_mode"
  }

  data class CourseViewParams(val enableSubtreeMode: Boolean, val visibleStudyItemId: Int)
}
