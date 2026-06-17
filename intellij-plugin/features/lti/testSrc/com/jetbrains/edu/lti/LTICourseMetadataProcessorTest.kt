package com.jetbrains.edu.lti

import com.jetbrains.edu.lti.LTICourseMetadataProcessor.Companion.LTI_COURSERA_COURSE
import com.jetbrains.edu.lti.LTICourseMetadataProcessor.Companion.LTI_LAUNCH_ID
import com.jetbrains.edu.lti.LTICourseMetadataProcessor.Companion.LTI_LMS_DESCRIPTION
import com.jetbrains.edu.learning.newproject.CourseMetadataProcessorTestBase
import org.junit.Test

class LTICourseMetadataProcessorTest : CourseMetadataProcessorTestBase() {

  @Test
  fun `lti metadata`() {
    // given
    val metadata = mapOf(
      LTI_LAUNCH_ID to "id",
      LTI_LMS_DESCRIPTION to "description"
    )
    // when
    createCourseWithMetadata(metadata)
    // then
    assertEquals(
      LTISettingsDTO("id", "description", LTIOnlineService.STANDALONE, null),
      LTISettingsManager.getInstance(project).settings
    )
  }

  @Test
  fun `missing required parameters`() {
    // given
    val metadata = mapOf(
      LTI_LMS_DESCRIPTION to "description"
    )
    // when
    createCourseWithMetadata(metadata)
    // then
    assertNull(LTISettingsManager.getInstance(project).settings)
  }

  @Test
  fun `coursera_course affects return link`() {
    // given
    val metadata = mapOf(
      LTI_LAUNCH_ID to "id",
      LTI_LMS_DESCRIPTION to "moodle",
      LTI_COURSERA_COURSE to "cats"
    )
    // when
    createCourseWithMetadata(metadata)
    // then
    assertEquals(
      LTISettingsDTO("id", "moodle", LTIOnlineService.STANDALONE, "https://www.coursera.org/learn/cats"),
      LTISettingsManager.getInstance(project).settings
    )
  }
}
