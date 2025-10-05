package com.jetbrains.edu.commandLine

import com.intellij.util.application
import com.jetbrains.edu.learning.EduTestCase
import com.jetbrains.edu.learning.Err
import com.jetbrains.edu.learning.Ok
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.marketplace.api.MarketplaceConnector
import com.jetbrains.edu.learning.mockService
import io.mockk.every
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertIs

class MarketplaceCourseSourceTest : EduTestCase() {

  private val testCourse = EduCourse().apply {
    id = COURSE_ID
    name = "Test Course"
    description = "Test course description"
    marketplaceCourseVersion = 1
  }

  @Test
  fun `test load public course from marketplace`() = runTest {
    // given
    val mockConnector = mockService<MarketplaceConnector>(application)
    every { mockConnector.searchCourse(any(), false) } returns testCourse

    // when
    val result = CourseSource.MARKETPLACE.loadCourse("$COURSE_ID")

    // then
    assertIs<Ok<EduCourse>>(result)
    assertEquals(testCourse, result.value)
    verify(exactly = 1) { mockConnector.searchCourse(COURSE_ID, false) }
  }

  @Test
  fun `test load private course from marketplace`() = runTest {
    // given
    val mockConnector = mockService<MarketplaceConnector>(application)
    every { mockConnector.searchCourse(any(), false) } returns null
    every { mockConnector.searchCourse(any(), true) } returns testCourse

    // when
    val result = CourseSource.MARKETPLACE.loadCourse("$COURSE_ID")

    // then
    assertIs<Ok<EduCourse>>(result)
    assertEquals(testCourse, result.value)
    verify(exactly = 1) { mockConnector.searchCourse(COURSE_ID, false) }
    verify(exactly = 1) { mockConnector.searchCourse(COURSE_ID, true) }
  }

  @Test
  fun `test load non-existing marketplace course`() = runTest {
    // given
    val mockConnector = mockService<MarketplaceConnector>(application)
    every { mockConnector.searchCourse(any(), any()) } returns null

    // when
    val result = CourseSource.MARKETPLACE.loadCourse("$COURSE_ID")

    // then
    assertIs<Err<String>>(result)
    assertEquals("Failed to load Marketplace course `$COURSE_ID`", result.error)
    verify(exactly = 2) { mockConnector.searchCourse(COURSE_ID, any()) }
  }

  companion object {
    private const val COURSE_ID = 123
  }
}
