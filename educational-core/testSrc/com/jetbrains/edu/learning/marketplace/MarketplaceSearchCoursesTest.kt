package com.jetbrains.edu.learning.marketplace

import com.jetbrains.edu.learning.EduTestCase
import com.jetbrains.edu.learning.UserInfo
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.marketplace.api.MarketplaceConnector
import com.jetbrains.edu.learning.marketplace.api.MockMarketplaceConnector
import okhttp3.mockwebserver.RecordedRequest

class MarketplaceSearchCoursesTest : EduTestCase() {

  private val mockConnector: MockMarketplaceConnector get() = MarketplaceConnector.getInstance() as MockMarketplaceConnector

  private fun configureCoursesResponse() {
    mockConnector.withResponseHandler(testRootDisposable) { request ->
      COURSES_REQUEST_RE.matchEntire(request.path) ?: return@withResponseHandler null
      mockResponse("courses.json")
    }
  }

  fun `test courses loaded`() {
    configureCoursesResponse()
    doTestCoursesLoaded()
  }

  fun `test python en course created`() {
    configureCoursesResponse()
    val courses = doTestCoursesLoaded()

    val pythonCourse = courses.first()
    assertEquals("Introduction to Python", pythonCourse.name)
    assertEquals(1, pythonCourse.marketplaceId)
    assertEquals("Python", pythonCourse.language)
    assertEquals("English", pythonCourse.humanLanguage)
    assertEquals("Introduction course to Python", pythonCourse.description)
    assertEquals(2, pythonCourse.learnersCount)
    assertEquals(5.0, pythonCourse.reviewScore)
    assertTrue(pythonCourse.isMarketplace)
    assertEquals("JetBrains s.r.o.", pythonCourse.organization)
    checkAuthorFullNames(listOf("JetBrains s.r.o."), pythonCourse.authorFullNames)
    checkAuthors(listOf("FirstName LastName"), pythonCourse.authors)
  }

  fun `test java ru course created`() {
    configureCoursesResponse()
    val courses = doTestCoursesLoaded()

    val javaCourse = courses[1]
    assertEquals("Introduction to Java", javaCourse.name)
    assertEquals(2, javaCourse.marketplaceId)
    assertEquals("JAVA", javaCourse.language)
    assertEquals("Russian", javaCourse.humanLanguage)
    assertEquals("Introduction course to Java", javaCourse.description)
    assertEquals(5, javaCourse.learnersCount)
    assertEquals(5.0, javaCourse.reviewScore)
    assertTrue(javaCourse.isMarketplace)
    assertNull(javaCourse.organization)
    val expectedAuthors = listOf("user1 LastName1", "user2 LastName2")
    checkAuthorFullNames(expectedAuthors, javaCourse.authorFullNames)
    checkAuthors(expectedAuthors, javaCourse.authors)
  }

  fun `test all courses loaded`() {
    mockConnector.withResponseHandler(testRootDisposable) { request ->
      COURSES_REQUEST_RE.matchEntire(request.path) ?: return@withResponseHandler null
      when (request.getOffset()) {
        0 -> mockResponse("courses_10.json")
        else -> mockResponse("courses.json")
      }
    }

    doTestCoursesLoaded(12)
  }

  fun `test course found by id`() {
    mockConnector.withResponseHandler(testRootDisposable) { request ->
      COURSES_REQUEST_RE.matchEntire(request.path) ?: return@withResponseHandler null
      mockResponse("course_by_id.json")
    }
    val courseId = 1
    val course = MarketplaceConnector.getInstance().searchCourse(courseId)
    checkNotNull(course)
    assertEquals(courseId, course.marketplaceId)
    assertEquals("Introduction to Python", course.name)
    assertEquals("Python", course.language)
    assertEquals("English", course.humanLanguage)
    assertEquals("Introduction course to Python", course.description)
    assertEquals(2, course.learnersCount)
    assertEquals(5.0, course.reviewScore)
    assertTrue(course.isMarketplace)
    assertNull(course.organization)
    checkAuthorFullNames(listOf("FirstName LastName"), course.authorFullNames)
    checkAuthors(listOf("FirstName LastName"), course.authors)
  }

  private fun checkAuthorFullNames(expected: List<String>, actual: MutableList<String>) {
    assertEquals(expected.size, actual.size)
    for (n in expected.indices) {
      assertEquals(expected[n], actual[n])
    }
  }

  private fun checkAuthors(expected: List<String>, actual: MutableList<UserInfo>) {
    assertEquals(expected.size, actual.size)
    for (n in expected.indices) {
      assertEquals(expected[n], actual[n].getFullName())
    }
  }

  private fun RecordedRequest.getOffset(): Int {
    return body.readUtf8().substringAfter("offset: ").substringBefore("\\n").toInt()
  }

  private fun doTestCoursesLoaded(coursesNumber: Int = 2): List<EduCourse> {
    val courses = MarketplaceConnector.getInstance().searchCourses()
    assertEquals(coursesNumber, courses.size)
    return courses
  }

  override fun getTestDataPath(): String = super.getTestDataPath() + "/marketplace/searchCourses/"

  companion object {
    private val COURSES_REQUEST_RE = """/api/search/graphql?.*""".toRegex()
  }
}