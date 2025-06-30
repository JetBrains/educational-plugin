package com.jetbrains.edu.learning.statistics

import com.jetbrains.edu.learning.newproject.CourseParamsProcessorTestBase
import org.junit.Test

class EntryPointParamProcessorTest : CourseParamsProcessorTestBase() {

  @Test
  fun `entry point metadata`() {
    // given
    val metadata = mapOf("entry_point" to "foo")
    // when
    createCourseWithMetadata(metadata)
    // then
    assertEquals("foo", EntryPointManager.getInstance(project).entryPoint)
  }

  @Test
  fun `too long metadata value`() {
    // given
    val metadata = mapOf("entry_point" to "ABCDEFGHIJKLMNOPQ")
    // when
    createCourseWithMetadata(metadata)
    // then
    assertNull(EntryPointManager.getInstance(project).entryPoint)
  }
}
