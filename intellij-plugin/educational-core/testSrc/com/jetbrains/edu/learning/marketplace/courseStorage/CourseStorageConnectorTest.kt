package com.jetbrains.edu.learning.marketplace.courseStorage

import com.jetbrains.edu.learning.EduTestCase
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.courseFormat.StudyItem
import com.jetbrains.edu.learning.marketplace.courseConnector
import com.jetbrains.edu.learning.marketplace.courseStorage.api.CourseStorageConnector
import com.jetbrains.edu.learning.marketplace.update.MarketplaceCourseUpdater
import com.jetbrains.edu.learning.statistics.DownloadCourseContext
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.verify
import org.junit.Test
import kotlin.test.assertIs

class CourseStorageConnectorTest : EduTestCase()  {
  private val mockConnector: MockCourseStorageConnector
    get() = CourseStorageConnector.getInstance() as MockCourseStorageConnector

  private fun configureCoursesResponse() {
    mockConnector.withResponseHandler(testRootDisposable) { _, path ->
      when (path) {
        "/api/courses/latest?courseId=205101" -> mockResponse("search_course_response.json")
        else -> null
      }
    }
  }

  @Test
  fun `test search course from link`() {
    configureCoursesResponse()
    val course = mockConnector.getCourseInfoByLink("205101")!!
    doTest(course)
  }

  @Test
  fun `test course with id greater than 200_000 returns CourseStorageConnector`() {
    val course = EduCourse().apply { id = 200_005 }
    val connector = course.courseConnector
    assertIs<CourseStorageConnector>(connector)
  }

  @Test
  fun `test updater uses CourseStorageConnector for getting course from server`() {
    val courseId = 200_005
    val course = course {
      lesson("lesson1") {
        eduTask("task1") {
          taskFile("TaskFile1.kt", "task file 1 text")
          taskFile("TaskFile3.kt", "task file 3 text")
        }
      }
    }.apply {
      id = courseId
      marketplaceCourseVersion = 1
    } as EduCourse

    val remoteCourseInfo = course {}.apply {
      id = courseId
      marketplaceCourseVersion = 2
    } as EduCourse

    val remoteCourseFull = course {
      lesson("lesson1") {
        eduTask("task1") {
          taskFile("TaskFile1.kt", "task file 1 new text")
          taskFile("TaskFile2.kt", "task file 2 new text")
        }
      }
    }.apply {
      id = 200_005
      marketplaceCourseVersion = 2
    } as EduCourse

    mockkObject(mockConnector) {
      every { mockConnector.searchCourse(courseId) } returns remoteCourseInfo
      every { mockConnector.loadCourse(courseId, DownloadCourseContext.UPDATE) } returns remoteCourseFull

      val course = MarketplaceCourseUpdater(project, course, 2).courseFromServer(course)!!

      verify(exactly = 1) { mockConnector.searchCourse(courseId) }
      verify(exactly = 1) { mockConnector.loadCourseStructure(remoteCourseInfo, DownloadCourseContext.UPDATE) }
      verify(exactly = 1) { mockConnector.loadCourse(courseId, DownloadCourseContext.UPDATE) }

      assertEquals(2, course.marketplaceCourseVersion)
      val expectedItems = listOf(
        "TaskFile1.kt" to "task file 1 new text",
        "TaskFile2.kt" to "task file 2 new text",
      )
      val actualItems = course
        .lessons.single()
        .taskList.single()
        .taskFiles.map {
          it.key to it.value.contents.textualRepresentation
        }.sortedBy { it.first }
      assertEquals(expectedItems, actualItems)
    }
  }

  private fun doTest(course: EduCourse) {
    assertEquals(205101, course.id)
    assertEquals("TEST AWS Kotlin Course", course.name)
    assertEquals("Example of the course made within the EduAWS collaboration.", course.description)
    assertEquals(1, course.marketplaceCourseVersion)
    assertEquals(21, course.formatVersion)
    assertEquals(true, course.isMarketplace)
    assertEquals("kotlin", course.languageId)
    assertEquals(null, course.languageVersion)
    assertEquals("en", course.languageCode)
    assertEquals("", course.environment)
    assertEquals(listOf<StudyItem>(), course.items)
  }

  override fun getTestDataPath(): String = super.getTestDataPath() + "/marketplace/courseStorage/"
}