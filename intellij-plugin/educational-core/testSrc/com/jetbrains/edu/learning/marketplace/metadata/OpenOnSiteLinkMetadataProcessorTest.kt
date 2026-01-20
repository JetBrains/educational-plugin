package com.jetbrains.edu.learning.marketplace.metadata

import com.jetbrains.edu.learning.marketplace.settings.OpenOnSiteLinkSettings
import com.jetbrains.edu.learning.newproject.CourseMetadataProcessorTestBase
import org.junit.Test

class OpenOnSiteLinkMetadataProcessorTest : CourseMetadataProcessorTestBase() {
  private val linkParameterName: String = OpenOnSiteLinkMetadataProcessor.OPEN_ON_SITE_URL_PARAMETER_NAME

  private val link: String?
    get() = OpenOnSiteLinkSettings.getInstance(project).link

  @Test
  fun `test link is saved`() {
    // given
    val params = mapOf(linkParameterName to getRandomTrustedUrl())

    // when
    createCourseWithMetadata(params)

    //then
    assertNotNull(link)
    assertEquals(params[linkParameterName], link)
  }

  @Test
  fun `test link with untrusted host is not saved`() {
    // given
    val params = mapOf(linkParameterName to "https://google.com")

    // when
    createCourseWithMetadata(params)

    // then
    assertNull(link)
  }

  @Test
  fun `test link with invalid protocol is not saved`() {
    // given
    val params = mapOf(linkParameterName to "ftp://google.com")

    // when
    createCourseWithMetadata(params)

    // then
    assertNull(link)
  }

  @Test
  fun `test link with absent protocol is not saved`() {
    // given
    val params = mapOf(linkParameterName to "google.com")

    // when
    createCourseWithMetadata(params)

    // then
    assertNull(link)
  }

  @Test
  fun `test missing link parameter`() {
    // given
    val params = emptyMap<String, String>()

    // when
    createCourseWithMetadata(params)

    // then
    assertNull(link)
  }
}