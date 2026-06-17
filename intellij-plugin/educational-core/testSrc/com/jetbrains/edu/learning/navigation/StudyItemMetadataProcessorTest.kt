package com.jetbrains.edu.learning.navigation

import com.jetbrains.edu.learning.navigation.StudyItemMetadataProcessor.Companion.STUDY_ITEM_ID
import com.jetbrains.edu.learning.newproject.CourseMetadataProcessorTestBase
import org.junit.Test

class StudyItemMetadataProcessorTest : CourseMetadataProcessorTestBase() {

  @Test
  fun `study item id is selected`() {
    // given
    val metadata = mapOf(STUDY_ITEM_ID to "12345")
    // when
    createCourseWithMetadata(metadata)
    // then
    assertEquals(12345, StudyItemSelectionService.getInstance(project).lastStudyItemId())
  }

  @Test
  fun `no study item id`() {
    // given
    val metadata = emptyMap<String, String>()
    // when
    createCourseWithMetadata(metadata)
    // then
    assertNull(StudyItemSelectionService.getInstance(project).lastStudyItemId())
  }

  @Test
  fun `invalid study item id`() {
    // given
    val metadata = mapOf(STUDY_ITEM_ID to "not-a-number")
    // when
    createCourseWithMetadata(metadata)
    // then
    assertNull(StudyItemSelectionService.getInstance(project).lastStudyItemId())
  }
}
