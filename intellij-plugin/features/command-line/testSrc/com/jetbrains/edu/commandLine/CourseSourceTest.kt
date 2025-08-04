package com.jetbrains.edu.commandLine

import com.intellij.openapi.application.ApplicationManager
import com.intellij.util.application
import com.jetbrains.edu.learning.EduTestCase
import com.jetbrains.edu.learning.Err
import com.jetbrains.edu.learning.Ok
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.marketplace.courseStorage.api.CourseStorageConnector
import com.jetbrains.edu.learning.mockService
import io.mockk.every
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertIs

@OptIn(ExperimentalCoroutinesApi::class)
class CourseSourceTest : EduTestCase() {

  @Test
  fun `test load course from course storage`() = runTest {
    val mockConnector = mockService<CourseStorageConnector>(application)

    val testCourse = EduCourse().apply {
      id = 200_001
      name = "Test Course"
      description = "Test course description"
      marketplaceCourseVersion = 1
      authors = mutableListOf()
    }

    every { mockConnector.searchCourse(any(), any()) } returns testCourse
    val result = CourseSource.COURSE_STORAGE.loadCourse("200001")
    assertIs<Ok<EduCourse>>(result)
    assertEquals(testCourse, result.value)
    verify(exactly = 1) { mockConnector.searchCourse(200_001, any()) }
  }

  @Test
  fun `test load incorrect course from course storage`() = runTest {
    val application = ApplicationManager.getApplication()
    val mockConnector = mockService<CourseStorageConnector>(application)

    every { mockConnector.searchCourse(any(), any()) } returns null
    val result = CourseSource.COURSE_STORAGE.loadCourse("123")
    assertIs<Err<String>>(result)
    assertEquals("Failed to load course from the course storage `123`", result.error)
    verify(exactly = 2) { mockConnector.searchCourse(123, any()) }
  }
}