package com.jetbrains.edu.learning.marketplace

import com.jetbrains.edu.learning.EduTestCase
import com.jetbrains.edu.learning.marketplace.api.MarketplaceConnector
import com.jetbrains.edu.learning.marketplace.api.MockMarketplaceConnector
import com.jetbrains.edu.learning.marketplace.api.QueryData

class MarketplaceSearchCoursesTest : EduTestCase() {
  override fun setUp() {
    super.setUp()
    configureCoursesResponse()
  }

  private val mockConnector: MockMarketplaceConnector get() = MarketplaceConnector.getInstance() as MockMarketplaceConnector

  private fun configureCoursesResponse() {
    mockConnector.withResponseHandler(testRootDisposable) { request ->
      COURSES_REQUEST_RE.matchEntire(request.path) ?: return@withResponseHandler null
      mockResponse("courses.json")
    }
  }

  fun `test courses loaded`() {
    val courses = MarketplaceConnector.getInstance().searchCourses(QueryData("query"))

    checkNotNull(courses) { "Courses are not loaded" }
    assertEquals(2, courses.size)
  }

  fun `test python en course created`() {
    val courses = MarketplaceConnector.getInstance().searchCourses(QueryData("query"))

    checkNotNull(courses) { "Courses are not loaded" }
    val pythonCourse = courses.first()
    assertEquals("Introduction to Python", pythonCourse.name)
    assertEquals(1, pythonCourse.id)
    assertEquals("Python", pythonCourse.language)
    assertEquals("English", pythonCourse.humanLanguage)
    assertEquals("Introduction course to Python", pythonCourse.description)
    assertEquals(2, pythonCourse.learnersCount)
    assertEquals(5.0, pythonCourse.reviewScore)
    assertEquals(true, pythonCourse.isMarketplace)
  }

  fun `test java ru course created`() {
    val courses = MarketplaceConnector.getInstance().searchCourses(QueryData("query"))

    checkNotNull(courses) { "Courses are not loaded" }
    val javaCourse = courses[1]
    assertEquals("Introduction to Java", javaCourse.name)
    assertEquals(2, javaCourse.id)
    assertEquals("Java", javaCourse.language)
    assertEquals("Russian", javaCourse.humanLanguage)
    assertEquals("Introduction course to Java", javaCourse.description)
    assertEquals(5, javaCourse.learnersCount)
    assertEquals(5.0, javaCourse.reviewScore)
    assertEquals(true, javaCourse.isMarketplace)
  }

  override fun getTestDataPath(): String = super.getTestDataPath() + "/marketplace/searchCourses/"

  companion object {
    private val COURSES_REQUEST_RE = """/api/search/graphql?.*""".toRegex()
  }
}