package com.jetbrains.edu.commandLine

import com.intellij.util.application
import com.jetbrains.edu.learning.EduTestCase
import com.jetbrains.edu.learning.Err
import com.jetbrains.edu.learning.Ok
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.marketplace.courseStorage.api.CourseStorageConnector
import com.jetbrains.edu.learning.mockService
import io.mockk.every
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertIs

class CourseStorageCourseSourceTest : EduTestCase() {

  @Test
  fun `test load course from course storage`() = runTest {
    // given
    val testCourse = EduCourse().apply {
      id = 200_001
      name = "Test Course"
      description = "Test course description"
      marketplaceCourseVersion = 1
      authors = mutableListOf()
    }

    val mockConnector = mockService<CourseStorageConnector>(application)
    every { mockConnector.searchCourse(any(), any()) } returns testCourse

    // when
    val result = CourseSource.COURSE_STORAGE.loadCourse("200001")

    // then
    assertIs<Ok<EduCourse>>(result)
    assertEquals(testCourse, result.value)
    verify(exactly = 1) { mockConnector.searchCourse(200_001, any()) }
  }

  @Test
  fun `test load incorrect course from course storage`() = runTest {
    // given
    val mockConnector = mockService<CourseStorageConnector>(application)
    every { mockConnector.searchCourse(any(), any()) } returns null

    // when
    val result = CourseSource.COURSE_STORAGE.loadCourse("123")

    // then
    assertIs<Err<String>>(result)
    assertEquals("Failed to load course from the course storage `123`", result.error)
    verify(exactly = 2) { mockConnector.searchCourse(123, any()) }
  }
}
