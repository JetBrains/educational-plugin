package com.jetbrains.edu.learning.statistics.metadata

import com.jetbrains.edu.learning.newproject.CourseMetadataProcessorTestBase
import org.junit.Test

class EntryPointMetadataProcessorTest : CourseMetadataProcessorTestBase() {

  @Test
  fun `entry point metadata`() {
    // given
    val metadata = mapOf("entry_point" to "foo")
    // when
    createCourseWithMetadata(metadata)
    // then
    assertEquals(metadata, CourseSubmissionMetadataManager.getInstance(project).metadata)
  }

  @Test
  fun `too long metadata value`() {
    // given
    val metadata = mapOf("entry_point" to "ABCDEFGHIJKLMNOPQ")
    // when
    createCourseWithMetadata(metadata)
    // then
    assertEquals(0, CourseSubmissionMetadataManager.getInstance(project).metadata.size)
  }
}
