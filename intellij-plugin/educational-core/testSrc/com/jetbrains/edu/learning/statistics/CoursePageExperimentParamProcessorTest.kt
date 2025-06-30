package com.jetbrains.edu.learning.statistics

import com.jetbrains.edu.learning.newproject.CourseParamsProcessorTestBase
import org.junit.Test

class CoursePageExperimentParamProcessorTest : CourseParamsProcessorTestBase() {

  @Test
  fun `course page metadata`() {
    // given
    val metadata = mapOf("experiment_id" to "123", "experiment_variant" to "456")
    // when
    createCourseWithMetadata(metadata)
    // then
    assertEquals(CoursePageExperiment("123", "456"), CoursePageExperimentManager.getInstance(project).experiment)
  }

  @Test
  fun `too long metadata value`() {
    // given
    val metadata = mapOf("experiment_id" to "123", "experiment_variant" to "ABCDEFGHIJKLMNOPQ")
    // when
    createCourseWithMetadata(metadata)
    // then
    assertNull(CoursePageExperimentManager.getInstance(project).experiment)
  }

  @Test
  fun `missing required parameter`() {
    // given
    val metadata = mapOf("experiment_id" to "123")
    // when
    createCourseWithMetadata(metadata)
    // then
    assertNull(CoursePageExperimentManager.getInstance(project).experiment)
  }
}
