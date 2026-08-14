package com.jetbrains.edu.learning.projectView

import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.navigation.StudyItemSelectionService.Companion.STUDY_ITEM_ID
import com.jetbrains.edu.learning.newproject.CourseMetadataProcessor
import com.jetbrains.edu.learning.newproject.CourseMetadataProcessorTestBase
import com.jetbrains.edu.learning.newproject.CourseProjectState
import com.jetbrains.edu.learning.projectView.CourseViewMetadataProcessor.Companion.ENABLE_SUBTREE_VIEW_MODE
import org.junit.Test

/**
 * Checks how the course subtree mode is enabled with the parameters of `openCourse` command.
 *
 * See [CourseViewMetadataProcessor]
 */
class CourseViewMetadataProcessorTest : CourseMetadataProcessorTestBase() {

  @Test
  fun `test task is revealed`() {
    // when
    createCourseWithMetadata(mapOf(ENABLE_SUBTREE_VIEW_MODE to "true", STUDY_ITEM_ID to "121"))

    // then
    val visibleItems = CourseViewVisibleItems.getInstance(project)
    assertTrue(visibleItems.isSubtreeModeEnabled)
    assertEquals(setOf(121), visibleItems.visibleTaskIds)
  }

  @Test
  fun `test all tasks of revealed lesson are visible`() {
    // when
    createCourseWithMetadata(mapOf(ENABLE_SUBTREE_VIEW_MODE to "true", STUDY_ITEM_ID to "11"))

    // then
    val visibleItems = CourseViewVisibleItems.getInstance(project)
    assertTrue(visibleItems.isSubtreeModeEnabled)
    assertEquals(setOf(111, 112), visibleItems.visibleTaskIds)
  }

  @Test
  fun `test all tasks of revealed section are visible`() {
    // when
    createCourseWithMetadata(mapOf(ENABLE_SUBTREE_VIEW_MODE to "true", STUDY_ITEM_ID to "1"))

    // then
    val visibleItems = CourseViewVisibleItems.getInstance(project)
    assertTrue(visibleItems.isSubtreeModeEnabled)
    assertEquals(setOf(111, 112, 121), visibleItems.visibleTaskIds)
  }

  @Test
  fun `test subtree mode is not enabled without study item id`() {
    // when
    createCourseWithMetadata(mapOf(ENABLE_SUBTREE_VIEW_MODE to "true"))

    // then
    val visibleItems = CourseViewVisibleItems.getInstance(project)
    assertFalse(visibleItems.isSubtreeModeEnabled)
    assertEmpty(visibleItems.visibleTaskIds)
  }

  @Test
  fun `test nothing is revealed for unknown study item id`() {
    // when
    createCourseWithMetadata(mapOf(ENABLE_SUBTREE_VIEW_MODE to "true", STUDY_ITEM_ID to "12345"))

    // then
    val visibleItems = CourseViewVisibleItems.getInstance(project)
    assertTrue(visibleItems.isSubtreeModeEnabled)
    assertEmpty(visibleItems.visibleTaskIds)
  }

  @Test
  fun `test subtree mode is not enabled for invalid study item id`() {
    // when
    createCourseWithMetadata(mapOf(ENABLE_SUBTREE_VIEW_MODE to "true", STUDY_ITEM_ID to "not-a-number"))

    // then
    val visibleItems = CourseViewVisibleItems.getInstance(project)
    assertFalse(visibleItems.isSubtreeModeEnabled)
    assertEmpty(visibleItems.visibleTaskIds)
  }

  @Test
  fun `test subtree mode is disabled without parameter`() {
    // when
    createCourseWithMetadata(mapOf(STUDY_ITEM_ID to "121"))

    // then
    val visibleItems = CourseViewVisibleItems.getInstance(project)
    assertFalse(visibleItems.isSubtreeModeEnabled)
    assertEmpty(visibleItems.visibleTaskIds)
  }

  @Test
  fun `test subtree mode is disabled for false parameter value`() {
    // when
    createCourseWithMetadata(mapOf(ENABLE_SUBTREE_VIEW_MODE to "false", STUDY_ITEM_ID to "121"))

    // then
    val visibleItems = CourseViewVisibleItems.getInstance(project)
    assertFalse(visibleItems.isSubtreeModeEnabled)
    assertEmpty(visibleItems.visibleTaskIds)
  }

  @Test
  fun `test subtree mode is disabled for invalid parameter value`() {
    // when
    createCourseWithMetadata(mapOf(ENABLE_SUBTREE_VIEW_MODE to "yes", STUDY_ITEM_ID to "121"))

    // then
    val visibleItems = CourseViewVisibleItems.getInstance(project)
    assertFalse(visibleItems.isSubtreeModeEnabled)
    assertEmpty(visibleItems.visibleTaskIds)
  }

  @Test
  fun `test task is revealed for opened project`() {
    // given
    val course = createCourseWithMetadata(emptyMap())

    // when
    applyProcessors(course, mapOf(ENABLE_SUBTREE_VIEW_MODE to "true", STUDY_ITEM_ID to "121"), CourseProjectState.OPENED_PROJECT)

    // then
    val visibleItems = CourseViewVisibleItems.getInstance(project)
    assertTrue(visibleItems.isSubtreeModeEnabled)
    assertEquals(setOf(121), visibleItems.visibleTaskIds)
  }

  @Test
  fun `test task is revealed for focused open project`() {
    // given
    val course = createCourseWithMetadata(emptyMap())

    // when
    applyProcessors(course, mapOf(ENABLE_SUBTREE_VIEW_MODE to "true", STUDY_ITEM_ID to "121"), CourseProjectState.FOCUSED_OPEN_PROJECT)

    // then
    val visibleItems = CourseViewVisibleItems.getInstance(project)
    assertTrue(visibleItems.isSubtreeModeEnabled)
    assertEquals(setOf(121), visibleItems.visibleTaskIds)
  }

  @Test
  fun `test study item is revealed for project already in subtree mode`() {
    // given
    val course = createCourseWithMetadata(mapOf(ENABLE_SUBTREE_VIEW_MODE to "true", STUDY_ITEM_ID to "121"))

    // when
    applyProcessors(course, mapOf(STUDY_ITEM_ID to "112"), CourseProjectState.FOCUSED_OPEN_PROJECT)

    // then
    // the mode is neither disabled nor reset by a launch without the parameter,
    // and the launched study item is revealed anyway
    val visibleItems = CourseViewVisibleItems.getInstance(project)
    assertTrue(visibleItems.isSubtreeModeEnabled)
    assertEquals(setOf(112, 121), visibleItems.visibleTaskIds)
  }

  /**
   * Emulates applying metadata processors for an already existing course project
   */
  private fun applyProcessors(course: Course, metadata: Map<String, String>, courseProjectState: CourseProjectState) {
    CourseMetadataProcessor.applyProcessors(project, course, metadata, courseProjectState)
  }
}
