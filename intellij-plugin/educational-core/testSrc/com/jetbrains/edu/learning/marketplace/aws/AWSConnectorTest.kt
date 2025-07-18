package com.jetbrains.edu.learning.marketplace.aws

import com.jetbrains.edu.learning.EduTestCase
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.courseFormat.StudyItem
import com.jetbrains.edu.learning.marketplace.awsTracks.api.AWSConnector
import com.jetbrains.edu.learning.marketplace.getCourseConnector
import org.junit.Test

class AWSConnectorTest : EduTestCase()  {
  private val mockConnector: MockAWSConnector
    get() = AWSConnector.getInstance() as MockAWSConnector

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
  fun `test course with id greater than 200_000 returns AWSConnector`() {
    val course = EduCourse().apply { id = 200_005 }
    val connector = getCourseConnector(course)
    assertInstanceOf(connector, AWSConnector::class.java)
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

  override fun getTestDataPath(): String = super.getTestDataPath() + "/marketplace/aws/"
}