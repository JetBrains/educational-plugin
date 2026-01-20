package com.jetbrains.edu.learning.marketplace.metadata

import com.jetbrains.edu.learning.marketplace.settings.LicenseLinkSettings
import com.jetbrains.edu.learning.newproject.CourseMetadataProcessorTestBase
import kotlin.test.Test

class LicenseLinkMetadataProcessorTest : CourseMetadataProcessorTestBase() {
  private val linkParameterName: String = LicenseLinkMetadataProcessor.LICENSE_URL_PARAMETER_NAME

  private val link: String?
    get() = LicenseLinkSettings.getInstance(project).link

  @Test
  fun `test license link metadata decodes encoded url and saves it`() {
    // given
    val encodedLink = "https%3A%2F%2Facademy%2Ejetbrains%2Ecom%2Fapi%2Fedu-track%2Faws%2Fplugin%2Flicense%3FtrackRef%3Dabc%26courseRef%3Dedf%26moduleRef%3Dhij"
    val expectedLink = "https://academy.jetbrains.com/api/edu-track/aws/plugin/license?trackRef=abc&courseRef=edf&moduleRef=hij"
    val params = mapOf(linkParameterName to encodedLink)

    // when
    createCourseWithMetadata(params)

    // then
    assertNotNull(link)
    assertEquals(expectedLink, link)
  }

  @Test
  fun `test link with untrusted host is not saved`() {
    // given
    val params = mapOf(linkParameterName to "https%3A%2F%2Fgoogle.com")

    // when
    createCourseWithMetadata(params)

    // then
    assertNull(link)
  }

  @Test
  fun `test link with invalid protocol is not saved`() {
    // given
    val params = mapOf(linkParameterName to "ftp%3A%2F%2Fgoogle.com")

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