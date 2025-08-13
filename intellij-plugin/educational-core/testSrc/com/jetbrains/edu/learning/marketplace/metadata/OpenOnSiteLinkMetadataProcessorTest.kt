package com.jetbrains.edu.learning.marketplace.metadata

import com.jetbrains.edu.learning.marketplace.settings.OpenOnSiteLinkSettings
import com.jetbrains.edu.learning.marketplace.settings.OpenOnSiteLinkSettings.Companion.TRUSTED_OPEN_ON_SITE_HOSTS
import com.jetbrains.edu.learning.newproject.CourseMetadataProcessorTestBase
import org.junit.Test

class OpenOnSiteLinkMetadataProcessorTest : CourseMetadataProcessorTestBase() {

  @Test
  fun `test link is saved`() {
    val params = mapOf("link" to "https://${TRUSTED_OPEN_ON_SITE_HOSTS.keys.shuffled().first()}")
    createCourseWithMetadata(params)
    assertEquals(params["link"], OpenOnSiteLinkSettings.getInstance(project).link)
  }

  @Test
  fun `test link with untrusted host is not saved`() {
    val params = mapOf("link" to "https://google.com")
    createCourseWithMetadata(params)
    assertNull(OpenOnSiteLinkSettings.getInstance(project).link)
  }

  @Test
  fun `test link with invalid protocol is not saved`() {
    val params = mapOf("link" to "ftp://google.com")
    createCourseWithMetadata(params)
    assertNull(OpenOnSiteLinkSettings.getInstance(project).link)
  }

  @Test
  fun `test link with absent protocol is not saved`() {
    val params = mapOf("link" to "google.com")
    createCourseWithMetadata(params)
    assertNull(OpenOnSiteLinkSettings.getInstance(project).link)
  }

  @Test
  fun `test missing link parameter`() {
    val params = emptyMap<String, String>()
    createCourseWithMetadata(params)
    assertNull(OpenOnSiteLinkSettings.getInstance(project).link)
  }
}