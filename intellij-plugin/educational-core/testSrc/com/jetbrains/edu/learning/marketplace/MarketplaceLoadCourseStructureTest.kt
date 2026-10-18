package com.jetbrains.edu.learning.marketplace

import com.intellij.util.application
import com.jetbrains.edu.learning.EduTestCase
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.marketplace.api.MarketplaceConnector
import com.jetbrains.edu.learning.mockService
import com.jetbrains.edu.learning.statistics.DownloadCourseContext
import io.mockk.every
import org.junit.Test

class MarketplaceLoadCourseStructureTest : EduTestCase() {

  @Test
  fun `test certification and environment settings are taken from archive if not provided by marketplace`() {
    val course = loadCourseStructure(
      courseInfo = createMarketplaceCourse(),
      archiveCourse = createMarketplaceCourse(certification = true, environmentSettings = mapOf(SETTING_KEY to "archive_value"))
    )

    assertEquals(true, course.certification)
    assertEquals(mapOf(SETTING_KEY to "archive_value"), course.environmentSettings)
  }

  // It's more theoretical case that real one, but since it's possible from code POV,
  // this test case covers it
  @Test
  fun `test certification and environment settings provided by marketplace are overridden by archive`() {

    val course = loadCourseStructure(
      courseInfo = createMarketplaceCourse(certification = false, environmentSettings = mapOf(SETTING_KEY to "marketplace_value")),
      archiveCourse = createMarketplaceCourse(certification = true, environmentSettings = mapOf(SETTING_KEY to "archive_value"))
    )

    assertEquals(true, course.certification)
    assertEquals(mapOf(SETTING_KEY to "archive_value"), course.environmentSettings)
  }

  private fun loadCourseStructure(courseInfo: EduCourse, archiveCourse: EduCourse): EduCourse {
    val connector = mockService<MarketplaceConnector>(application)
    every { connector.loadCourse(COURSE_ID, DownloadCourseContext.IDE_UI) } returns archiveCourse
    connector.loadCourseStructure(courseInfo, DownloadCourseContext.IDE_UI)
    return courseInfo
  }

  private fun createMarketplaceCourse(certification: Boolean? = null, environmentSettings: Map<String, String> = emptyMap()): EduCourse {
    return (course {} as EduCourse).apply {
      id = COURSE_ID
      this.certification = certification
      this.environmentSettings = environmentSettings
    }
  }

  companion object {
    private const val COURSE_ID = 1
    private const val SETTING_KEY = "test_setting"
  }
}
