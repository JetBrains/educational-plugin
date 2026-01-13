package com.jetbrains.edu.learning.marketplace.metadata

import com.jetbrains.edu.learning.newproject.CourseMetadataProcessorTestBase
import org.junit.Test

abstract class LinkMetadataProcessorTestBase : CourseMetadataProcessorTestBase() {
  protected abstract val linkParameterName: String
  protected abstract val link: String?

  @Test
  fun `test link is saved`() {
    val params = mapOf(linkParameterName to getRandomTrustedUrl())
    createCourseWithMetadata(params)
    assertNotNull(link)
    assertEquals(params[linkParameterName], link)
  }

  @Test
  fun `test link with untrusted host is not saved`() {
    val params = mapOf(linkParameterName to "https://google.com")
    createCourseWithMetadata(params)
    assertNull(link)
  }

  @Test
  fun `test link with invalid protocol is not saved`() {
    val params = mapOf(linkParameterName to "ftp://google.com")
    createCourseWithMetadata(params)
    assertNull(link)
  }

  @Test
  fun `test link with absent protocol is not saved`() {
    val params = mapOf(linkParameterName to "google.com")
    createCourseWithMetadata(params)
    assertNull(link)
  }

  @Test
  fun `test missing link parameter`() {
    val params = emptyMap<String, String>()
    createCourseWithMetadata(params)
    assertNull(link)
  }
}