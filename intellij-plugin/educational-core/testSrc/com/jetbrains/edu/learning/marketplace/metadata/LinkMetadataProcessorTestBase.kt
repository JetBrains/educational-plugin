package com.jetbrains.edu.learning.marketplace.metadata

import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.newproject.CourseMetadataProcessorTestBase
import org.junit.Test

abstract class LinkMetadataProcessorTestBase : CourseMetadataProcessorTestBase() {
  abstract val linkParameterName: String
  abstract fun getSavedLink(project: Project): String?

  @Test
  fun `test link is saved`() {
    val params = mapOf(linkParameterName to getRandomTrustedUrl())
    createCourseWithMetadata(params)
    assertEquals(params["link"], getSavedLink(project))
  }

  @Test
  fun `test link with untrusted host is not saved`() {
    val params = mapOf(linkParameterName to "https://google.com")
    createCourseWithMetadata(params)
    assertNull(getSavedLink(project))
  }

  @Test
  fun `test link with invalid protocol is not saved`() {
    val params = mapOf(linkParameterName to "ftp://google.com")
    createCourseWithMetadata(params)
    assertNull(getSavedLink(project))
  }

  @Test
  fun `test link with absent protocol is not saved`() {
    val params = mapOf(linkParameterName to "google.com")
    createCourseWithMetadata(params)
    assertNull(getSavedLink(project))
  }

  @Test
  fun `test missing link parameter`() {
    val params = emptyMap<String, String>()
    createCourseWithMetadata(params)
    assertNull(getSavedLink(project))
  }
}